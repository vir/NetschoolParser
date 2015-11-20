package ru.spb.vir.http;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpMessage {
    public class Header {
        public Header(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public String name, value;
    }

    public final byte[] crlf = { '\r', '\n' };

    public HttpMessage() {
        this.headers = new LinkedList<Header>();
        this.httpVersion = "1.0";
    }

    protected static String readLine(InputStream ins) throws IOException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        int b;
        boolean ok = false;
        while((b = ins.read()) >= 0) {
            if (b == 13)
                continue;
            if (b == 10) {
                ok = true;
                break;
            }
            tmp.write(b);
        }
        if(! ok) {
            if(tmp.size() == 0)
                throw new EOFException();
            else
                throw new IOException("No end-of-line");
        }
        return new String(tmp.toByteArray());
    }

    public String getHttpVersion() { return httpVersion; }
    public void setHttpVersion(String httpVersion) { this.httpVersion = httpVersion; }

    // ========= headers =========
    public void addHeader(String name, String value)
    {
        headers.add(new Header(name, value));
    }
    public boolean deleteHeader(String name) {
        boolean r = false;
        for (Iterator<Header> iter = headers.listIterator(); iter.hasNext(); ) {
            Header h = iter.next();
            if (h.name.equalsIgnoreCase(name)) {
                iter.remove();
                r = true;
            }
        }
        return r;
    }
    public void setHeader(String name, String value) {
        deleteHeader(name);
        addHeader(name, value);
    }
    public boolean hasHeader(String name) {
        for(Header h: headers) {
            if(h.name.equalsIgnoreCase(name))
                return true;
        }
        return false;
    }
    public String getHeader(String name)
    {
        for(Header h: headers) {
            if(h.name.equalsIgnoreCase(name))
                return h.value;
        }
        return null;
    }
    public final LinkedList<Header> getHeaders() {
        return headers;
    }
    protected void readHeaders(InputStream ins) throws IOException {
        headers.clear();
        String s;
        while((s = readLine(ins)) != null) {
            if(s.length() == 0)
                break;
            String[] tokens = s.split(":[ \t]*", 2);
            headers.add(new Header(tokens[0], tokens[1]));
        }
    }
    protected void writeHeaders(OutputStream wr) throws IOException {
        for(Header h: headers) {
            wr.write((String.format("%s: %s", h.name, h.value)).getBytes());
            wr.write(crlf);
        }
    }

    // ========= body =========
    public void setBody(String body)
    {
        setBody(body.getBytes(Charset.forName("UTF-8")));
    }
    public void setBody(byte[] body)
    {
        this.body = body;
    }
    public byte[] getRawBody() {
        return body;
    }
    public String getDecodedBody() throws UnsupportedEncodingException {
        String encoding = "UTF-8";
        return new String(body, encoding);
    }
    public int getBodyLength() {
        return body == null ? 0 : body.length;
    }
    public String guessBodyCharset() {
        final String defaultBodyCharset = "ISO-8859-1";
        String ct = getHeader("Content-Type");
        if(ct == null)
            return  defaultBodyCharset;
        Pattern p = Pattern.compile(";\\s*charset=\\s*([a-zA-Z0-9-]+)", Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(ct);
        if (m.find())
            return m.group(1);
        else
            return defaultBodyCharset;
    }

    // ========= data =========
    private String httpVersion;
    private LinkedList<Header> headers;
    private byte[] body;
}
