package ru.spb.vir.browser;

import ru.spb.vir.http.HttpMessage;
import ru.spb.vir.http.Request;
import ru.spb.vir.http.Response;
import ru.spb.vir.http.UserAgent;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Browser extends UserAgent {
    final boolean methodGetAfter302 = true;
    public String simpleGet(String uri) throws IOException, HttpBadRequestException {
        Request req = createRequest("GET", uri);
        addRequestHeaders(req);
        Response rsp = send(req);
        if(rsp.getStatus() >= 400)
            throw new HttpBadRequestException("bad response");
        storeCookies(rsp);
        return rsp.getDecodedBody();
    }
    public String simplePost(String uri, Form f) throws IOException, HttpBadRequestException {
        Response rsp;
        Request req = createRequest("POST", uri);
        req.setBody(f.encode());
        req.setHeader("Content-Type", "application/x-www-form-urlencoded");
        addRequestHeaders(req);
        for(;;) {
            rsp = send(req);
            if(rsp.getStatus() >= 400 || rsp.getStatus() < 200)
                throw new HttpBadRequestException("bad response");
            storeCookies(rsp);
            if(rsp.getStatus() < 300)
                return rsp.getDecodedBody();
            req.setUri(rsp.getHeader("Location")); // redirect
            if(rsp.getStatus() == 303
                    || (methodGetAfter302 && (rsp.getStatus() == 301 || rsp.getStatus() == 302)))
                req.setMethod("GET");
            System.err.println("Redirected to " + uri);
        }
    }
    public String submit(Form f) throws IOException, HttpBadRequestException {
        if(0 == f.getMethod().compareToIgnoreCase("GET")) {
            // XXX GET unimplemented XXX
        } else if(0 == f.getMethod().compareToIgnoreCase("POST")) {
            return simplePost(f.getAction(), f);
        } else
            throw new IOException("Bad Form object");
        return "XXX";
    }
    private void addRequestHeaders(HttpMessage m) {
        m.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0");
        m.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        m.addHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3");
        m.addHeader("Accept-Encoding", "gzip, deflate");
        addCookies(m);
    }
    private void addCookies(HttpMessage m) {
        String c = "";
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if(c.length() != 0)
                c += "; ";
            c += entry.getKey() + "=" + entry.getValue();
        }
        m.addHeader("Cookie", c);
    }
    private void storeCookies(HttpMessage rsp) {
        LinkedList<HttpMessage.Header> hdrs = rsp.getHeaders();
        Pattern p = Pattern.compile("^(.*?)=(.*?)(;.*)?$", Pattern.DOTALL);
        for(HttpMessage.Header h: hdrs) {
            if(0 != h.name.compareToIgnoreCase("Set-Cookie"))
                continue;
            Matcher m = p.matcher(h.value);
            if(m.find())
                cookies.put(m.group(1), m.group(2));
        }
    }

    public Browser(String hostname, int port) {
        super(hostname, port);
        cookies = new HashMap<String, String>();
    }
    private Map<String, String> cookies;
}
