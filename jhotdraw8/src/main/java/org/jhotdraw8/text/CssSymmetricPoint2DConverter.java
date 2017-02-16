/* @(#)CssPoint2DConverter.java
 * Copyright (c) 2015 by the authors and contributors of JHotDraw.
 * You may only use this file in compliance with the accompanying license terms.
 */
package org.jhotdraw8.text;

import java.io.IOException;
import java.nio.CharBuffer;
import java.text.ParseException;
import javafx.geometry.Point2D;
import org.jhotdraw8.io.IdFactory;

/**
 * Converts a {@code javafx.geometry.Point2D} into a {@code String} and vice
 * versa. If the X and the Y-value are identical, then only one value is output.
 *
 * @author Werner Randelshofer
 * @version $Id$
 */
public class CssSymmetricPoint2DConverter implements Converter<Point2D> {

    private final PatternConverter formatter = new PatternConverter("{0,list,{1,number}|[ ]+}", new CssConverterFactory());

    @Override
    public void toString(Appendable out, IdFactory idFactory, Point2D value) throws IOException {
        double x = value.getX();
        double y = value.getY();
        if (x == y) {
            formatter.toStr(out, idFactory, 1, value.getX());
        } else {
            formatter.toStr(out, idFactory, 2, value.getX(), value.getY());
        }
    }

    @Override
    public Point2D fromString(CharBuffer buf, IdFactory idFactory) throws ParseException, IOException {
        Object[] v = formatter.fromString(buf);
        int count = (Integer) v[0];
        switch (count) {
            case 1:
                return new Point2D(((Number) v[1]).doubleValue(), ((Number) v[1]).doubleValue());
            case 2:
                return new Point2D(((Number) v[1]).doubleValue(), ((Number) v[2]).doubleValue());
            default:
                throw new ParseException("one or two numbers expected, found " + count + " numbers", 0);
        }
    }

    @Override
    public Point2D getDefaultValue() {
        return new Point2D(0, 0);
    }
}