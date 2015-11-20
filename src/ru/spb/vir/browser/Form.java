package ru.spb.vir.browser;

import javafx.util.Pair;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class Form {
    public static class Select extends Vector<Pair<String, String>> {
        private Select() {
            curSel = -1;
        }
        public static Select parse(Element el) {
            Select s = new Select();
            s.name = el.attr("name");
            for(Element opt: el.select("option")) {
                if(opt.hasAttr("selected"))
                    s.curSel = s.size();
                String val = opt.attr("value");
                s.add(new Pair<String, String>(val, opt.text()));
            }
            return s;
        }
        public String curVal() {
            return curSel < 0 ? null : elementAt(curSel).getKey();
        }
        public String curLabel() {
            return curSel < 0 ? null : elementAt(curSel).getValue();
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public boolean isSelected() {
            return curSel >= 0;
        }
        public boolean selectNext() {
            if(curSel >= size() - 1)
                return false;
            ++curSel;
            return true;
        }
        public boolean selectVal(String val) {
            for(int i = 0; i < size(); ++i) {
                if(elementAt(i).getKey().equals(val)) {
                    curSel = i;
                    return true;
                }
            }
            return false;
        }
        public boolean selectPrev() {
            if(curSel <= 0)
                return false;
            --curSel;
            return true;
        }
        private String name;
        private int curSel;
    }

    private static final String charset = "UTF-8";

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public class Input {
        public Input(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public String name;
        public String value;
    }

    public Form() {
        this.inputs = new LinkedList<Input>();
    }
    protected void parseInPlace(Element formelem) {
        setMethod(formelem.attr("method"));
        setAction(formelem.attr("action"));
        Elements inputs = formelem.select("input");
        for(Element inp: inputs) {
            String name = inp.attr("name");
            String val = inp.attr("value");
            add(name, val);
        }
        inputs = formelem.select("select");
        for(Element inp: inputs) {
            Select s = Select.parse(inp);
            if(s.isSelected())
                add(s.name, s.curVal());
        }

    }
    public static Form parse(Element formelem) {
        Form f = new Form();
        f.parseInPlace(formelem);
        return f;
    }

    public int size() { return inputs.size(); }
    public void add(String name, String value) {
        inputs.add(new Input(name, value));
    }
    public void set(String name, String value) {
        for(Input x: inputs) {
            if(x.name.equalsIgnoreCase(name)) {
                x.value = value;
                return;
            }
        }
        inputs.add(new Input(name, value));
    }
    public String encode() throws IOException {
        String r = "";
        for(Input x: inputs) {
            if(r.length() != 0)
                r += "&";
            r += urlEscape(x.name);
            r += "=";
            if(x.value != null)
                r += urlEscape(x.value);
        }
        return r;
    }
    public static boolean isAllowedUriChar(byte c) {
        return (
                (c >= 'A' && c <= 'Z') ||
                        (c >= 'a' && c <= 'z') ||
                        (c >= '0' && c <= '9') ||
                        c == '-' || c == '_' ||
                        c == '.' || c == '~'
        );
    }
    public static boolean contains(byte[] arr, byte b) {
        for(byte x: arr) {
            if(x == b)
                return true;
        }
        return false;
    }
    public static String urlEscape(String s, byte[] moreAllowedChars, boolean plusForSpace) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for(byte c: s.getBytes(charset)) {
            if(isAllowedUriChar(c) || (moreAllowedChars != null && contains(moreAllowedChars, c)))
                os.write(c);
            else if(c == ' ' && plusForSpace)
                os.write('+');
            else
                os.write(String.format("%%%02X", ((int)c)&0xFF).getBytes());
        }
        return os.toString();
    }
    public static String urlEscape(String s) throws IOException {
        return urlEscape(s, null, true);
    }
    private List<Input> inputs;
    private String method, action;
}
