package ru.spb.vir.browser;

import org.junit.Assert;
import org.junit.Test;

public class FormTest {

    @Test
    public void testAdd() throws Exception {
        Form f = new Form();
        Assert.assertEquals(0, f.size());
        f.add("a", "b");
        f.add("c", "d");
        Assert.assertEquals(2, f.size());
    }

    @Test
    public void testSet() throws Exception {
        Form f = new Form();
        Assert.assertEquals(0, f.size());
        f.add("a", "b");
        f.set("c", "d");
        f.set("c", "f");
        Assert.assertEquals(2, f.size());
        Assert.assertEquals("a=b&c=f", f.encode());
    }

    @Test
    public void testEncode() throws Exception {
        Form f = new Form();
        f.add("a", "b");
        f.add("c", "d");
        f.add("e", "f");
        Assert.assertEquals("a=b&c=d&e=f", f.encode());
    }

    @Test
    public void testIsAllowedUriChar() throws Exception {
        Assert.assertTrue(Form.isAllowedUriChar((byte)'x'));
        Assert.assertTrue(Form.isAllowedUriChar((byte)'8'));
        Assert.assertFalse(Form.isAllowedUriChar((byte) '%'));
        Assert.assertFalse(Form.isAllowedUriChar((byte) '='));
    }

    @Test
    public void testContains() throws Exception {
        byte[] arr = { 'a', 'b', 'c' };
        Assert.assertTrue(Form.contains(arr, (byte) 'a'));
        Assert.assertFalse(Form.contains(arr, (byte) 'x'));
    }

    @Test
    public void testUrlEscape() throws Exception {
        Assert.assertEquals("me%26you%3Dyou%26me", Form.urlEscape("me&you=you&me"));
        Assert.assertEquals("%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82", Form.urlEscape("привет"));
        Assert.assertEquals("one+two", Form.urlEscape("one two"));
        Assert.assertEquals("one=%20two", Form.urlEscape("one= two", new byte[] {(byte)'='}, false));
    }
}