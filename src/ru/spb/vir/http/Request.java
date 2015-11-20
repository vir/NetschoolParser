package ru.spb.vir.http;

import java.io.IOException;
import java.io.OutputStream;

public class Request extends HttpMessage {
    public Request(String method, String uri, String httpVersion) {
        this.method = method;
        this.uri = uri;
        setHttpVersion(httpVersion);
    }
    public void write(OutputStream wr) throws IOException {
        writeFirstLine(wr);
        writeHeaders(wr);
        wr.write(crlf);
        if(null != getRawBody())
            wr.write(getRawBody());
    }
    private void writeFirstLine(OutputStream wr) throws IOException {
        String line = method + " " + uri + " HTTP/" + getHttpVersion();
        wr.write(line.getBytes());
        wr.write(crlf);
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getMethod() {
        return method;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public String getUri() {
        return uri;
    }
    private String method, uri;
}
