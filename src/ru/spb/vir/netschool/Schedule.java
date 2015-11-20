package ru.spb.vir.netschool;

import org.jsoup.nodes.Document;

public class Schedule {
    public static Schedule parse(Document html) {
        Schedule r = new Schedule();
        return r;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    private String name;
}
