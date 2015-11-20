package ru.spb.vir.netschool;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.spb.vir.browser.Browser;
import ru.spb.vir.browser.Form;
import ru.spb.vir.http.Request;
import ru.spb.vir.http.Response;
import ru.spb.vir.http.UserAgent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InvalidHtmlException extends Exception {
    public InvalidHtmlException(String message) {
        super(message);
    }
}

class Mail {
    public static Mail Parse(Document html) {
        return new Mail();
    }
}

class Announcements {
    public static Announcements parse(Document html) {
        return new Announcements();
    }
}

class Utils {
    public static String md5hex(String input) throws UnsupportedEncodingException {
        byte[] s = input.getBytes("UTF-8");
        String md5 = null;
        if(null == input) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s, 0, s.length);
            md5 = new BigInteger(1, digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5;
    }
    static class PupilSwitcher extends Form {
        private PupilSwitcher() {
        }
        public static PupilSwitcher Parse(Document doc, String formName) throws InvalidHtmlException {
            Elements els;
            els = doc.select("form[name=" + formName + "]");
            if(els.size() != 1)
                throw new InvalidHtmlException("no pupil switch form");
            Element form = els.first();

            PupilSwitcher r = new PupilSwitcher();
            r.parseInPlace(form);

            els = form.select("select[name=SID]");
            if(els.size() != 1)
                throw new InvalidHtmlException("No pupil switch");
            r.pupils = Form.Select.parse(els.first());
            r.name = r.pupils.curLabel();

            els = form.select("select[name=DATE]");
            if(els.size() != 1)
                throw new InvalidHtmlException("No date switch");
            r.weeks = Form.Select.parse(els.first());
            return r;
        }
        public boolean nextPupil() {
            if(! pupils.selectNext())
                return false;
            set("SID", pupils.curVal());
            setAction("/asp/Curriculum/Assignments.asp");
            return true;
        }
        public boolean nextWeek() {
            if(! weeks.selectNext())
                return false;
            set("DATE", weeks.curVal());
            setAction("/asp/Curriculum/Assignments.asp");
            return true;
        }
        public String getName() { return name; }
        public String getWeek() { return weeks.curVal(); }
        public boolean setWeek(String w) {
            if (weeks.selectVal(w)) {
                set("DATE", weeks.curVal());
                return true;
            }
            return false;
        }
        private Form.Select pupils, weeks;
        private String name;
    }
}

public class NetSchoolParser {
    private class HttpLogget implements UserAgent.Logger {
        public HttpLogget(Logger logger) {
            this.logger = logger;
        }
        @Override
        public void logRequest(String host, int port, Request req) {
            try {
                logger.writeln(String.format("===== Sending HTTP request to %s:%d =====<<<", host, port));
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                req.write(os);
                int len = os.size();
                logger.writeln(os.toString(req.guessBodyCharset()));
                logger.writeln(String.format(">>>===== %d bytes =====", len));
            } catch (IOException e) {
                logger.writeln(String.format("Got exception while dumping HTTP request: %s", e.toString()));
            }
        }
        @Override
        public void logResponse(String host, int port, Response rsp) {
            try {
                logger.writeln(String.format("===== Got HTTP response from %s:%d =====<<<", host, port));
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                rsp.write(os);
                int len = os.size();
                logger.writeln(os.toString(rsp.guessBodyCharset()));
                logger.writeln(String.format(">>>===== %d bytes =====", len));
            } catch (IOException e) {
                logger.writeln(String.format("Got exception while dumping HTTP response: %s", e.toString()));
            }
        }
        @Override
        public void logMsg(String msg) {
            logger.writeln("HTTP: " + msg);
        }
        Logger logger;
    }

    public class NetSchoolError extends RuntimeException {
        NetSchoolError(String msg) {
            super(msg);
        }
    }

    public NetSchoolParser(String host, int port) throws IOException {
        log = new Logger(new InitParams());
        br = new Browser(host, port);
        br.setLogger(new HttpLogget(log));
    }

    public void login(String un, String pw) throws IOException, UserAgent.HttpBadRequestException, InvalidHtmlException {
        log.stage("login");
        html = br.simpleGet("/?AL=Y");
        log.dumpFile(html, "form");

        String salt = null;
        {
            Pattern p = Pattern.compile("var salt = '(.*?)';", Pattern.DOTALL);
            Matcher m = p.matcher(html);
            if(m.find()) {
                salt = m.group(1);
                log.writeln("Found salt: " + salt);
            }
        }

        doc = Jsoup.parse(html);
        Form f = Form.parse(doc.select("form[name=MainForm]").first());
        f.set("UN", un);
        f.set("PW", pw);

        // DIC 2 SID 1 PID -1 CN 1 SFT 2 SCID 1
        f.set("DIC", "2");
        f.set("SID", "1");
        f.set("PID", "-1");
        f.set("CN", "1");
        f.set("SFT", "2");
        f.set("SCID", "1");

        // var salt = '13317768397';
        // pw2.val(hexMD5_(salt + hexMD5_(pw.val())));
        // pw.val(pw2.val().substr(0, pw.val().length));
        String pw2 = Utils.md5hex(salt + Utils.md5hex(pw));
        f.set("PW2", pw2);
        f.set("PW", pw2.substring(0, pw.length()));
        log.writeln("Form: " + f.encode());

        f.setAction("/asp/postlogin.asp");
        submitFrom(f, "postresult");

        // ----------------------------

        Elements rf = doc.select("form[name=RF]");
        if (rf != null) {
            f = Form.parse(rf.first());
            submitFrom(f, "postredir");
        }

        // ----------------------------

        if(pageTitle.equals("Предупреждение о безопасности")) {
            // 	var form = document.forms[0];  DoSubmit(form,'');
            f = selectForm("Proceed");
            submitFrom(f, "postredir2");
        }
        if(pageTitle.startsWith("Установить контрольный вопрос и секретный ответ пользователя")) {
            // goBack(document.SaveRecoveryPasswordInfo, '/asp/Announce/ViewAnnouncements.asp');
            f = selectForm("SaveRecoveryPasswordInfo");
            f.setAction("/asp/Announce/ViewAnnouncements.asp");
            submitFrom(f, "postredir3");
        }
    }
        // Почта => JavaScript:ShowMail()
        // Объявления => JavaScript:SetSelectedMenu('12','/asp/Reports/Reports.asp')
        /* function SetSelectedMenu(miID, url) {
            var form = document.forms['MenuForm'];
            if (!form)
                return;
            checkForChanges().then(function () {
                form.elements['MenuItem'].value = miID;
                form.elements['TabItem'].value = 0;
                form.action = url;
                isHaveToLogout = false;
                form.submit();
            });
        }
        */

    public Mail getMail() {
        log.stage("mail");
        /*
        function getVer() { var d; d = new Date(); return d.getTime(); }
        function ShowMail() {
            if ( wndMail && !wndMail.closed ) { wndMail.forceClosing = true; wndMail.close(); }
            wndMail = window.open( '/asp/Messages/MailBox.asp?VER=' + getVer() + '&AT=93635806328758056000199', '_mail', 'status=yes,toolbar=no,menubar=no,location=no,scrollbars=yes,resizable=yes,directories=no,width=750,height=560' );
            center(wndMail, 750,560);
        }
        */
        return Mail.Parse(doc);
    }

    public LinkedList<MarkBook> getMarkBooks() throws IOException, UserAgent.HttpBadRequestException, InvalidHtmlException {
        // Дневник => JavaScript:SetSelectedMenu('14','/asp/Curriculum/Assignments.asp')
        log.stage("markbook");
        jsSetSelectedMenu(14, "/asp/Curriculum/Assignments.asp");

        int reqLimit = 10; // just in case

        LinkedList<MarkBook> r = new LinkedList<MarkBook>();
        String startWeek = null;
        for(;;) {
            Utils.PupilSwitcher ps = Utils.PupilSwitcher.Parse(doc, "Assignments");
            if(startWeek == null)
                startWeek = ps.getWeek();
            MarkBook mb = MarkBook.parse(doc);
            mb.setName(ps.getName());

            // fetch next week
            if(ps.nextWeek()) {
                submitFrom(ps, "nextweek");
                mb.parseAppendTasks(doc, false);
                ps.setWeek(startWeek);
            }

            // remember schedule
            r.add(mb);
            if(! ps.nextPupil())
                break;
            if(0 == --reqLimit)
                break;

            submitFrom(ps, "nextpupil");
        }
        return r;
    }

    public LinkedList<Schedule> getSchedules() throws IOException, UserAgent.HttpBadRequestException, InvalidHtmlException {
        // Расписание => JavaScript:SetSelectedMenu('10','/asp/SetupSchool/Calendar/YearView.asp')
        log.stage("schedule");
        jsSetSelectedMenu(10, "/asp/SetupSchool/Calendar/YearView.asp");

        // TAB: День => SetSelectedTab(20,'/asp/Calendar/DayViewS.asp');
        jsSetSelectedTab(20, "/asp/Calendar/DayViewS.asp");

        int reqLimit = 10; // just in case
        LinkedList<Schedule> r = new LinkedList<Schedule>();
        for(;;) {
            Utils.PupilSwitcher ps = Utils.PupilSwitcher.Parse(doc, "View");
            Schedule scd = Schedule.parse(doc);
            scd.setName(ps.getName());
            r.add(scd);
            if(! ps.nextPupil())
                break;
            if(0 == --reqLimit)
                break;

            submitFrom(ps, "nextpupil");
        }
        return r;
    }

    private Form selectForm(String name) throws InvalidHtmlException {
        Elements els = doc.select("form[name=" + name + "]");
        if(els == null || els.size() != 1)
            throw new InvalidHtmlException("Form " + name + " not found");
        return Form.parse(els.first());
    }

    private void jsSetSelectedMenu(int id, String url) throws IOException, UserAgent.HttpBadRequestException, InvalidHtmlException {
        Form f = selectForm("MenuForm");
        f.setAction(url);
        f.set("MenuItem", Integer.toString(id));
        f.set("TabItem", "0");
        submitFrom(f, "menu" + Integer.toString(id));
    }
    private void jsSetSelectedTab(int id, String url) throws InvalidHtmlException, IOException, UserAgent.HttpBadRequestException {
        Form f = selectForm("MenuForm");
        f.setAction(url);
        f.set("TabItem", Integer.toString(id));
        submitFrom(f, "tab" + Integer.toString(id));
    }

    private void submitFrom(Form ps, String dumpName) throws IOException, UserAgent.HttpBadRequestException {
        html = br.submit(ps);
        log.dumpFile(html, dumpName);
        doc = Jsoup.parse(html);
        pageTitle = doc.select("div.PageTitle").text();
        log.writeln("Page title: " + pageTitle);

        if(pageTitle.equalsIgnoreCase("Ошибка")) {
            Pattern p = Pattern.compile("var text = \"(.*?)\";", Pattern.DOTALL);
            Matcher m = p.matcher(html);
            if(m.find()) {
                String err = m.group(1);
                throw new NetSchoolError(err);
            }
        }

    }

    private Browser br;
    private Logger log;
    private String html;
    private Document doc;
    private String pageTitle;
}
