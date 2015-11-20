package ru.spb.vir.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Response extends HttpMessage {
    public Response() {
        status = 0;
    }
    public void readHead(InputStream ins) throws IOException
    {
        String firstLine = readLine(ins);
        String[] tokens = firstLine.split(" +", 3);
        version = tokens[0];
        if(version.startsWith("HTTP/"))
            version = version.substring(5);
        else
            throw new IOException("Bad status line <<" + firstLine + ">>");
        status = Integer.parseInt(tokens[1]);
        msg = tokens[2];
        readHeaders(ins);
    }
    public void write(OutputStream wr) throws IOException {
        writeFirstLine(wr);
        writeHeaders(wr);
        wr.write(crlf);
        if(null != getRawBody())
            wr.write(getRawBody());
    }
    private void writeFirstLine(OutputStream wr) throws IOException {
        String line = String.format("HTTP/%s %d %s", getHttpVersion(), status, msg);
        wr.write(line.getBytes());
        wr.write(crlf);
    }
    public int getStatus() {
        return status;
    }
    public String getMsg() { return msg; }
    public String getVersion() { return version; }
    private int status;
    private String msg;
    private String version;

}
