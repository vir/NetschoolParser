package ru.spb.vir.http;

import java.io.*;
import java.net.Socket;

public class UserAgent {
    public interface Logger {
        void logRequest(String host, int port, Request req);
        void logResponse(String host, int port, Response rsp);
        void logMsg(String msg);
    }

    public class HttpBadRequestException extends Exception {
        public HttpBadRequestException(String message) {
            super(message);
        }
    }

    public UserAgent(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.keepalive = true;
        this.chunked = false;
        this.version = "1.1";
        this.requestCont = 0;
    }
    public String getHostname() {
        return hostname;
    }
    public int getPort() {
        return port;
    }
    public String getHttpVersion() {
        return version;
    }
    public Logger getLogger() { return logger; }
    public void setLogger(Logger logger) { this.logger = logger; }

    public Request createRequest(String method, String uri)
    {
        Request req = new Request(method, uri, getHttpVersion());
        String hostname = getHostname();
        if(getPort() != 80)
            hostname += ":" + getPort();
        req.addHeader("Host", hostname);
        return req;
    }

    public Response send(Request req) throws IOException
    {
        setConnectionHeaders(req);
        if(socket == null)
            connect();
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        Response rsp = new Response();
        try {
            if(logger != null)
                logger.logRequest(hostname, port, req);
            req.write(out);
            out.flush();

            rsp.readHead(in);
            parseConnectionHeaders(rsp);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            if(chunked)
                readChunkedBody(os, in);
            else
                readBody(os, in, rsp.getHeader("Content-Length"));
            rsp.setBody(os.toByteArray());
            if(logger != null)
                logger.logResponse(hostname, port, rsp);
        }
        catch (EOFException e) {
            if(keepalive && requestCont > 0) { // reconnect and retry
                if(logger != null)
                    logger.logMsg("Connection lost, trying to reconnect");
                connect();
                return send(req);
            }
            else
                throw e;
        }
        if(keepalive)
            ++requestCont;
        else
            disconnect();
        return rsp;
    }

    private void parseConnectionHeaders(Response rsp) throws IOException {
        version = rsp.getVersion();
        String connection = rsp.getHeader("Connection");
        keepalive = version.compareTo("1.0") > 0 ? true : false;
        if(connection == null)
            return;
        String[] connTokens = connection.split(", *");
        for(String tok: connTokens) {
            if(tok.equalsIgnoreCase("keep-alive"))
                keepalive = true;
            else if(tok.equalsIgnoreCase("close"))
                keepalive = false;
            else
                throw new IOException("Unsupported connection token " + tok);
        }

        String transferEncoding = rsp.getHeader("Transfer-Encoding");
        chunked = false;
        if(transferEncoding != null) {
            for(String tok: transferEncoding.split(", *")) {
                if(tok.equalsIgnoreCase("chunked"))
                    chunked = true;
                else
                    throw new IOException("Unsupported Transfer-Encoding token " + tok);
            }
        }
    }

    protected void setConnectionHeaders(HttpMessage m) {
        m.setHeader("Connection", keepalive ? "keep-alive" : "close");
        if(m.getBodyLength() != 0)
            m.setHeader("Content-Length", String.valueOf(m.getBodyLength()));
    }
    private void connect() throws IOException {
        if(socket != null)
            socket.close();
        socket = new Socket(hostname, port);
        requestCont = 0;
    }
    private void disconnect() throws IOException {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
        socket = null;
    }
    private void readBody(OutputStream os, InputStream ins, String contlen) throws IOException {
        final int bufsize = 8192;
        int toRead = -1;
        if(contlen != null)
            toRead = Integer.parseInt(contlen);
        else if(keepalive)
            throw new IOException("Unsupportrd keepalive connection without Content-Length header");
        int offs = 0;
        byte buffer[] = new byte[bufsize];
        for(;;) {
            int rd = ins.read(buffer, 0, toRead < bufsize && toRead > 0 ? toRead : bufsize);
            if(0 >= rd)
                break;
            os.write(buffer, 0, rd);
            offs += rd;
            if(toRead >= 0)
                toRead -= rd;
            if(toRead == 0)
                break;
        }
    }

    private void readChunkedBody(OutputStream os, InputStream ins) throws IOException {
        for(;;) {
            String slen = HttpMessage.readLine(ins);
            if(slen.length() == 0)
                slen = HttpMessage.readLine(ins);
            int len = Integer.parseInt(slen, 16);
            if(len == 0)
                break;
            byte buffer[] = new byte[len];
            int rd = ins.read(buffer, 0, len);
            if(rd != len)
                throw new IOException("Truncated message body");
            os.write(buffer, 0, rd);
        }
    }

    private String hostname;
    private int port;
    private Socket socket;
    private boolean keepalive, chunked;
    private String version;
    private int requestCont;
    private Logger logger;
}
