/* @(#)CssRectangle2DConverterTest.java
 * Copyright (c) 2016 by the authors and contributors of JHotDraw.
 * You may only use this file in compliance with the accompanying license terms.
 */

package org.jhotdraw8.css.text;

import org.jhotdraw8.css.text.CssDimensionRectangle2D;
import org.jhotdraw8.css.text.CssDimensionRectangle2DConverter;
import org.jhotdraw8.css.text.CssRectangle2DConverter;
import org.jhotdraw8.io.IdFactory;
import org.jhotdraw8.io.SimpleIdFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * CssRectangle2DConverterTest.
 *
 * @author Werner Randelshofer
 * @version $Id$
 */
public class CssDimensionRectangle2DConverterTest {

    public CssDimensionRectangle2DConverterTest() {
    }




    /**
     * Test of fromString method, of class CssDoubleConverter.
     */
    public static void testFromString(CssDimensionRectangle2D expected, String string) throws Exception {
        System.out.println("fromString " + string);
        CharBuffer buf = CharBuffer.wrap(string);
        IdFactory idFactory = new SimpleIdFactory();
        CssDimensionRectangle2DConverter instance = new CssDimensionRectangle2DConverter(false);
        CssDimensionRectangle2D actual = instance.fromString(buf, idFactory);
        System.out.println("  expected: " + expected);
        System.out.println("    actual: " + actual);

        String actualString = instance.toString(expected);
        System.out.println("  expectedString: " + string);
        System.out.println("    actualString: " + actualString);
        assertEquals(expected,actual);
        assertEquals(string,actualString);
    }
    @TestFactory
    public List<DynamicTest> testFromStringFactory() {
        return Arrays.asList(
                dynamicTest("1",()->  testFromString(new CssDimensionRectangle2D(11,22,33,44), "11 22 33 44")),
                dynamicTest("2",()->  testFromString(new CssDimensionRectangle2D(new CssDimension(0,"cm"),
                        new CssDimension(0,"cm"),new CssDimension(21,"cm"),new CssDimension(29.7,"cm")), "0cm 0cm 21cm 29.7cm"))
        );
    }
}