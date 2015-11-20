package ru.spb.vir.netschool;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {
    public Logger(InitParams initParams) throws IOException {
        this.dumpDir = initParams.getDumpDir();
        this.dumpBase = "xxx";
        this.dumpCount = 0;
        if(initParams.getLogName() != null) {
            File fn = new File(initParams.getDumpDir(), initParams.getLogName());
            logFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fn), encoding), true);
        }
    }
    public void stage(String s) {
        this.dumpBase = s;
        this.dumpCount = 0;
        writeln(String.format("=== %s ===", s));
    }
    public void dumpFile(String data, String suffix) throws IOException {
        String fn = String.format("%s\\NSP_%s_%d_%s.html", dumpDir, dumpBase, ++dumpCount, suffix);
        FileOutputStream fos = new FileOutputStream(fn);
        writeln("Dumping something to " + fn);
        fos.write(data.getBytes(encoding));
        fos.close();
    }
    public void writeln(String s) {
        System.out.println(s);
        if(logFile != null) {
            String ts = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss:").format(Calendar.getInstance().getTime());
            logFile.printf("%s %s\n", ts, s);
        }
    }
    public static final String encoding = "UTF-8";
    private String dumpDir;
    private String dumpBase;
    private int dumpCount;
    private PrintWriter logFile;
}
