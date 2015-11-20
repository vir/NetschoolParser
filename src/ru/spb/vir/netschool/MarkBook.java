package ru.spb.vir.netschool;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class MarkBook {
    private MarkBook() {
        tasks = new LinkedList<Task>();
    }

    public static class Task {
        static final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy, EE");
        public String date;
        public String subject;
        public String type;
        public String content;
        public int mark;
        public boolean outdated;
        public Task(String date, String subject, String type, String content, String mark) {
            this.date = date;
            this.subject = subject;
            this.type = type;
            this.content = content;
            this.mark = (mark.equals("-") || mark.isEmpty()) ? 0 : Integer.parseInt(mark);
        }
        public Date getDate() throws ParseException {
            return dateFormat.parse(date);
        }
    }
    public static MarkBook parse(Document html) throws InvalidHtmlException {
        MarkBook r = new MarkBook();

        // get class
        Elements els = html.select("form[name=Assignments] nobr");
        if(els.size() != 1)
            throw new InvalidHtmlException("no class found");
        r.pupCls = els.first().text();

        // get tasks
        r.parseAppendTasks(html, true);
        return r;
    }
    public void parseAppendTasks(Document html, boolean needOutdated) throws InvalidHtmlException {
        Elements els = html.select("h3");
        if(els.size() == 1 && els.first().text().equalsIgnoreCase("Нет заданий на этой неделе"))
            return;
        els = html.select("table[cellpadding=3]");
        if(els.size() != 1)
            throw new InvalidHtmlException("no main table in markbook");
        els = els.first().select("tr");
        Task t;
        String lastdate = "";
        for(Element tr: els) {
            Elements row = tr.children();
            if(row.first().text().equalsIgnoreCase("Срок сдачи"))
                continue;
            if(row.size() == 5) {
                t = new Task(row.get(0).text(), row.get(1).text(), row.get(2).text(), row.get(3).text(), row.get(4).text());
                lastdate = row.get(0).text();
            } else
                t = new Task(lastdate, row.get(0).text(), row.get(1).text(), row.get(2).text(), row.get(3).text());
            String color = tr.attr("BGCOLOR");
            t.outdated = ! color.equalsIgnoreCase("#FFFFFF");
            if(!(t.outdated && needOutdated))
                tasks.add(t);
        }
    }
    public void setName(String name) {
        this.pupName = name;
    }
    public MarkBook filterMarksOnly() {
        MarkBook n = new MarkBook();
        n.pupName = this.pupName;
        n.pupCls = this.pupCls;
        for(Task t: tasks) {
            if(t.mark == 0)
                continue;
            n.tasks.add(t);
        }
        return n;
    }

    public void dump(PrintWriter out) throws ParseException {
        out.printf("%s, %s\n", pupName, pupCls);
        for(Task t: tasks) {
            out.printf("%12s %40s %1s %120s %d\n", t.date, t.subject, t.type, t.content, t.mark);
        }
    }
    private LinkedList<Task> tasks;
    private String pupName, pupCls;
}
