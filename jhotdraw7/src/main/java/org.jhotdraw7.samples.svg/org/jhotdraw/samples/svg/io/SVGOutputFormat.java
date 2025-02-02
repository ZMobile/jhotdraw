/* @(#)SVGOutputFormat.java
 * Copyright © The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw.samples.svg.io;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.XMLElement;
import net.n3.nanoxml.XMLWriter;
import org.jhotdraw.draw.AttributeKey;
import org.jhotdraw.draw.BezierFigure;
import org.jhotdraw.draw.Drawing;
import org.jhotdraw.draw.Figure;
import org.jhotdraw.draw.io.OutputFormat;
import org.jhotdraw.geom.BezierPath;
import org.jhotdraw.gui.datatransfer.InputStreamTransferable;
import org.jhotdraw.gui.filechooser.ExtensionFileFilter;
import org.jhotdraw.io.Base64;
import org.jhotdraw.samples.svg.Gradient;
import org.jhotdraw.samples.svg.LinearGradient;
import org.jhotdraw.samples.svg.RadialGradient;
import org.jhotdraw.samples.svg.SVGAttributeKeys;
import org.jhotdraw.samples.svg.figures.SVGEllipseFigure;
import org.jhotdraw.samples.svg.figures.SVGGroupFigure;
import org.jhotdraw.samples.svg.figures.SVGImageFigure;
import org.jhotdraw.samples.svg.figures.SVGPathFigure;
import org.jhotdraw.samples.svg.figures.SVGRectFigure;
import org.jhotdraw.samples.svg.figures.SVGTextAreaFigure;
import org.jhotdraw.samples.svg.figures.SVGTextFigure;

import org.jhotdraw.annotation.Nullable;

import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static org.jhotdraw.samples.svg.SVGAttributeKeys.FILL_COLOR;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.FILL_GRADIENT;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.FILL_OPACITY;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.FONT_BOLD;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.FONT_FACE;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.FONT_ITALIC;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.FONT_SIZE;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.FONT_UNDERLINE;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.LINK;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.LINK_TARGET;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.OPACITY;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_CAP;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_COLOR;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_DASHES;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_DASH_PHASE;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_GRADIENT;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_JOIN;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_MITER_LIMIT;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_OPACITY;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.STROKE_WIDTH;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.TRANSFORM;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.VIEWPORT_FILL;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.VIEWPORT_FILL_OPACITY;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.VIEWPORT_HEIGHT;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.VIEWPORT_WIDTH;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.WINDING_RULE;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.WindingRule;
import static org.jhotdraw.samples.svg.SVGConstants.SVG_MIMETYPE;
import static org.jhotdraw.samples.svg.SVGConstants.SVG_NAMESPACE;

/**
 * An output format for storing drawings as
 * Scalable Vector Graphics SVG Tiny 1.2.
 *
 * @author Werner Randelshofer
 * @version $Id$
 */
public class SVGOutputFormat implements OutputFormat {
    /**
     * This is a counter used to create the next unique identification.
     */
    private int nextId;
    /**
     * In this hash map we store all elements to which we have assigned
     * an id.
     */
    private HashMap<IXMLElement, String> identifiedElements;
    /**
     * This element holds all definitions of the SVG file.
     */
    private IXMLElement defs;
    /**
     * Holds the document that is currently being written.
     */
    private IXMLElement document;
    /**
     * Maps gradients to ID's. We use this, so that we need to store
     * the same gradient only once.
     */
    private HashMap<Gradient, String> gradientToIDMap;
    /**
     * Set this to true for pretty printing.
     */
    private boolean isPrettyPrint;
    private static final HashMap<Integer, String> strokeLinejoinMap;

    static {
        strokeLinejoinMap = new HashMap<Integer, String>();
        strokeLinejoinMap.put(BasicStroke.JOIN_MITER, "miter");
        strokeLinejoinMap.put(BasicStroke.JOIN_ROUND, "round");
        strokeLinejoinMap.put(BasicStroke.JOIN_BEVEL, "bevel");
    }

    private static final HashMap<Integer, String> strokeLinecapMap;

    static {
        strokeLinecapMap = new HashMap<Integer, String>();
        strokeLinecapMap.put(BasicStroke.CAP_BUTT, "butt");
        strokeLinecapMap.put(BasicStroke.CAP_ROUND, "round");
        strokeLinecapMap.put(BasicStroke.CAP_SQUARE, "square");
    }

    /**
     * Set this variable to true if values should be written with float precision
     * instead with double precision.
     * Float precision is less accurate then double precision, but it uses
     * less storage space.
     */
    private static final boolean isFloatPrecision = true;

    /**
     * Creates a new instance.
     */
    public SVGOutputFormat() {
    }

    public javax.swing.filechooser.FileFilter getFileFilter() {
        return new ExtensionFileFilter("Scalable Vector Graphics (SVG)", "svg");
    }

    public JComponent getOutputFormatAccessory() {
        return null;
    }

    public void setPrettyPrint(boolean newValue) {
        isPrettyPrint = newValue;
    }

    public boolean isPrettyPrint() {
        return isPrettyPrint;
    }

    protected void writeElement(IXMLElement parent, Figure f) throws IOException {
        // Write link attribute as encosing "a" element
        if (f.get(LINK) != null && f.get(LINK).trim().length() > 0) {
            IXMLElement aElement = parent.createElement("a");
            aElement.setAttribute("xlink:href", f.get(LINK));
            if (f.get(LINK_TARGET) != null && f.get(LINK).trim().length() > 0) {
                aElement.setAttribute("target", f.get(LINK_TARGET));
            }
            parent.addChild(aElement);
            parent = aElement;
        }

        // Write the actual element
        if (f instanceof SVGEllipseFigure) {
            SVGEllipseFigure ellipse = (SVGEllipseFigure) f;
            if (ellipse.getWidth() == ellipse.getHeight()) {
                writeCircleElement(parent, ellipse);
            } else {
                writeEllipseElement(parent, ellipse);
            }
        } else if (f instanceof SVGGroupFigure) {
            writeGElement(parent, (SVGGroupFigure) f);
        } else if (f instanceof SVGImageFigure) {
            writeImageElement(parent, (SVGImageFigure) f);
        } else if (f instanceof SVGPathFigure) {
            SVGPathFigure path = (SVGPathFigure) f;
            if (path.getChildCount() == 1) {
                BezierFigure bezier = (BezierFigure) path.getChild(0);
                boolean isLinear = true;
                for (int i = 0, n = bezier.getNodeCount(); i < n; i++) {
                    if (bezier.getNode(i).getMask() != 0) {
                        isLinear = false;
                        break;
                    }
                }
                if (isLinear) {
                    if (bezier.isClosed()) {
                        writePolygonElement(parent, path);
                    } else {
                        if (bezier.getNodeCount() == 2) {
                            writeLineElement(parent, path);
                        } else {
                            writePolylineElement(parent, path);
                        }
                    }
                } else {
                    writePathElement(parent, path);
                }
            } else {
                writePathElement(parent, path);
            }
        } else if (f instanceof SVGRectFigure) {
            writeRectElement(parent, (SVGRectFigure) f);
        } else if (f instanceof SVGTextFigure) {
            writeTextElement(parent, (SVGTextFigure) f);
        } else if (f instanceof SVGTextAreaFigure) {
            writeTextAreaElement(parent, (SVGTextAreaFigure) f);
        } else {
            System.out.println("Unable to write: " + f);
        }
    }

    protected void writeCircleElement(IXMLElement parent, SVGEllipseFigure f) throws IOException {
        parent.addChild(
                createCircle(
                        document,
                        f.getX() + f.getWidth() / 2d,
                        f.getY() + f.getHeight() / 2d,
                        f.getWidth() / 2d,
                        f.getAttributes()));
    }

    protected IXMLElement createCircle(IXMLElement doc,
                                       double cx, double cy, double r,
                                       Map<AttributeKey<?>, Object> attributes) throws IOException {
        IXMLElement elem = doc.createElement("circle");
        writeAttribute(elem, "cx", cx, 0d);
        writeAttribute(elem, "cy", cy, 0d);
        writeAttribute(elem, "r", r, 0d);
        writeShapeAttributes(elem, attributes);
        writeOpacityAttribute(elem, attributes);
        writeTransformAttribute(elem, attributes);
        return elem;
    }

    protected IXMLElement createG(IXMLElement doc,
                                  Map<AttributeKey<?>, Object> attributes) throws IOException {
        IXMLElement elem = doc.createElement("g");
        writeOpacityAttribute(elem, attributes);
        return elem;
    }

    protected IXMLElement createLinearGradient(IXMLElement doc,
                                               double x1, double y1, double x2, double y2,
                                               double[] stopOffsets, Color[] stopColors, double[] stopOpacities,
                                               boolean isRelativeToFigureBounds,
                                               AffineTransform transform) throws IOException {
        IXMLElement elem = doc.createElement("linearGradient");

        writeAttribute(elem, "x1", toNumber(x1), "0");
        writeAttribute(elem, "y1", toNumber(y1), "0");
        writeAttribute(elem, "x2", toNumber(x2), "1");
        writeAttribute(elem, "y2", toNumber(y2), "0");
        writeAttribute(elem, "gradientUnits",
                (isRelativeToFigureBounds) ? "objectBoundingBox" : "userSpaceOnUse",
                "objectBoundingBox");
        writeAttribute(elem, "gradientTransform", toTransform(transform), "none");

        for (int i = 0; i < stopOffsets.length; i++) {
            IXMLElement stop = new XMLElement("stop");
            writeAttribute(stop, "offset", toNumber(stopOffsets[i]), null);
            writeAttribute(stop, "stop-color", toColor(stopColors[i]), null);
            writeAttribute(stop, "stop-opacity", toNumber(stopOpacities[i]), "1");
            elem.addChild(stop);
        }

        return elem;
    }

    protected IXMLElement createRadialGradient(IXMLElement doc,
                                               double cx, double cy, double fx, double fy, double r,
                                               double[] stopOffsets, Color[] stopColors, double[] stopOpacities,
                                               boolean isRelativeToFigureBounds,
                                               AffineTransform transform) throws IOException {
        IXMLElement elem = doc.createElement("radialGradient");

        writeAttribute(elem, "cx", toNumber(cx), "0.5");
        writeAttribute(elem, "cy", toNumber(cy), "0.5");
        writeAttribute(elem, "fx", toNumber(fx), toNumber(cx));
        writeAttribute(elem, "fy", toNumber(fy), toNumber(cy));
        writeAttribute(elem, "r", toNumber(r), "0.5");
        writeAttribute(elem, "gradientUnits",
                (isRelativeToFigureBounds) ? "objectBoundingBox" : "userSpaceOnUse",
                "objectBoundingBox");
        writeAttribute(elem, "gradientTransform", toTransform(transform), "none");

        for (int i = 0; i < stopOffsets.length; i++) {
            IXMLElement stop = new XMLElement("stop");
            writeAttribute(stop, "offset", toNumber(stopOffsets[i]), null);
            writeAttribute(stop, "stop-color", toColor(stopColors[i]), null);
            writeAttribute(stop, "stop-opacity", toNumber(stopOpacities[i]), "1");
            elem.addChild(stop);
        }

        return elem;
    }

    protected void writeEllipseElement(IXMLElement parent, SVGEllipseFigure f) throws IOException {
        parent.addChild(createEllipse(
                document,
                f.getX() + f.getWidth() / 2d,
                f.getY() + f.getHeight() / 2d,
                f.getWidth() / 2d,
                f.getHeight() / 2d,
                f.getAttributes()));
    }

    protected IXMLElement createEllipse(IXMLElement doc,
                                        double cx, double cy, double rx, double ry,
                                        Map<AttributeKey<?>, Object> attributes) throws IOException {
        IXMLElement elem = doc.createElement("ellipse");
        writeAttribute(elem, "cx", cx, 0d);
        writeAttribute(elem, "cy", cy, 0d);
        writeAttribute(elem, "rx", rx, 0d);
        writeAttribute(elem, "ry", ry, 0d);
        writeShapeAttributes(elem, attributes);
        writeOpacityAttribute(elem, attributes);
        writeTransformAttribute(elem, attributes);
        return elem;
    }

    protected void writeGElement(IXMLElement parent, SVGGroupFigure f) throws IOException {
        IXMLElement elem = createG(document, f.getAttributes());
        for (Figure child : f.getChildren()) {
            writeElement(elem, child);
        }
        parent.addChild(elem);
    }

    protected void writeImageElement(IXMLElement parent, SVGImageFigure f) throws IOException {
        parent.addChild(
                createImage(document,
                        f.getX(),
                        f.getY(),
                        f.getWidth(),
                        f.getHeight(),
                        f.getImageData(),
                        f.getAttributes()));
    }

    protected IXMLElement createImage(IXMLElement doc,
                                      double x, double y, double w, double h,
                                      byte[] imageData,
                                      Map<AttributeKey<?>, Object> attributes) throws IOException {
        IXMLElement elem = doc.createElement("image");
        writeAttribute(elem, "x", x, 0d);
        writeAttribute(elem, "y", y, 0d);
        writeAttribute(elem, "width", w, 0d);
        writeAttribute(elem, "height", h, 0d);
        writeAttribute(elem, "xlink:href", "data:image;base64," + Base64.encodeBytes(imageData), "");
        writeOpacityAttribute(elem, attributes);
        writeTransformAttribute(elem, attributes);
        return elem;
    }

    protected void writePathElement(IXMLElement parent, SVGPathFigure f) throws IOException {
        BezierPath[] beziers = new BezierPath[f.getChildCount()];
        for (int i = 0; i < beziers.length; i++) {
            beziers[i] = ((BezierFigure) f.getChild(i)).getBezierPath();
        }
        parent.addChild(createPath(
                document,
                beziers,
                f.getAttributes()));
    }

    protected IXMLElement createPath(IXMLElement doc,
                                     BezierPath[] beziers,
                                     Map<AttributeKey<?>, Object> attributes) throws IOException {
        IXMLElement elem = doc.createElement("path");
        writeShapeAttributes(elem, attributes);
        writeOpacityAttribute(elem, attributes);
        writeTransformAttribute(elem, attributes);
        writeAttribute(elem, "d", toPath(beziers), null);
        return elem;
    }

    protected void writePolygonElement(IXMLElement parent, SVGPathFigure f) throws IOException {
        LinkedList<Point2D.Double> points = new LinkedList<Point2D.Double>();
        for (int i = 0, n = f.getChildCount(); i < n; i++) {
            BezierPath bezier = ((BezierFigure) f.getChild(i)).getBezierPath();
            for (BezierPath.Node node : bezier) {
                points.add(new Point2D.Double(node.x[0], node.y[0]));
            }
        }

        parent.addChild(createPolygon(
                document,
                points.toArray(new Point2D.Double[points.size()]),
                f.getAttributes()));
    }

    protected IXMLElement createPolygon(IXMLElement doc,
                                        Point2D.Double[] points,
                                        Map<AttributeKey<?>, Object> attributes)
            throws IOException {
        IXMLElement elem = doc.createElement("polygon");
        writeAttribute(elem, "points", toPoints(points), null);
        writeShapeAttributes(elem, attributes);
        writeOpacityAttribute(elem, attributes);
        writeTransformAttribute(elem, attributes);
        return elem;
    }

    protected void writePolylineElement(IXMLElement parent, SVGPathFigure f) throws IOException {
        LinkedList<Point2D.Double> points = new LinkedList<Point2D.Double>();
        for (int i = 0, n = f.getChildCount(); i < n; i++) {
            BezierPath bezier = ((BezierFigure) f.getChild(i)).getBezierPath();
            for (BezierPath.Node node : bezier) {
                points.add(new Point2D.Double(node.x[0], node.y[0]));
            }
        }

        parent.addChild(createPolyline(
                document,
                points.toArray(new Point2D.Double[points.size()]),
                f.getAttributes()));
    }

    protected IXMLElement createPolyline(IXMLElement doc,
                                         Point2D.Double[] points,
                                         Map<AttributeKey<?>, Object> attributes) throws IOException {

        IXMLElement elem = doc.createElement("polyline");
        writeAttribute(elem, "points", toPoints(points), null);
        writeShapeAttributes(elem, attributes);
        writeOpacityAttribute(elem, attributes);
        writeTransformAttribute(elem, attributes);
        return elem;
    }

    protected void writeLineElement(IXMLElement parent, SVGPathFigure f)
            throws IOException {
        BezierFigure bezier = (BezierFigure) f.getChild(0);
        parent.addChild(createLine(
                document,
                bezier.getNode(0).x[0],
                bezier.getNode(0).y[0],
                bezier.getNode(1).x[0],
                bezier.getNode(1).y[0],
                f.getAttributes()));
    }

    protected IXMLElement createLine(IXMLElement doc,
                                     double x1, double y1, double x2, double y2,
                                     Map<AttributeKey<?>, Object> attributes) throws IOException {
        IXMLElement elem = doc.createElement("line");
        writeAttribute(elem, "x1", x1, 0d);
        writeAttribute(elem, "y1", y1, 0d);
        writeAttribute(elem, "x2", x2, 0d);
        writeAttribute(elem, "y2", y2, 0d);
        writeShapeAttributes(elem, attributes);
        writeOpacityAttribute(elem, attributes);
        writeTransformAttribute(elem, attributes);
        return elem;
    }

    protected void writeRectElement(IXMLElement parent, SVGRectFigure f) throws IOException {
        parent.addChild(
                createRect(
                        document,
                        f.getX(),
                        f.getY(),
                        f.getWidth(),
                        f.getHeight(),
                        f.getArcWidth(),
                        f.getArcHeight(),
                        f.getAttributes()));
    }

    protected IXMLElement createRect(IXMLElement doc,
                                     double x, double y, double width, double height,
                                     double rx, double ry,
                                     Map<AttributeKey<?>, Object> attributes)
            throws IOException {
        IXMLElement elem = doc.createElement("rect");
        writeAttribute(elem, "x", x, 0d);
        writeAttribute(elem, "y", y, 0d);
        writeAttribute(elem, "width", width, 0d);
        writeAttribute(elem, "height", height, 0d);
        writeAttribute(elem, "rx", rx, 0d);
        writeAttribute(elem, "ry", ry, 0d);
        writeShapeAttributes(elem, attributes);
        writeOpacityAttribute(elem, attributes);
        writeTransformAttribute(elem, attributes);
        return elem;
    }

    protected void writeTextElement(IXMLElement parent, SVGTextFigure f) throws IOException {
        DefaultStyledDocument styledDoc = new DefaultStyledDocument();
        try {
            styledDoc.insertString(0, f.getText(), null);
        } catch (BadLocationException e) {
            InternalError error = new InternalError(e.getMessage());
            error.initCause(e);
            throw error;
        }
        parent.addChild(
                createText(
                        document,
                        f.getCoordinates(),
                        f.getRotates(),
                        styledDoc,
                        f.getAttributes()));
    }

    protected IXMLElement createText(IXMLElement doc,
                                     Point2D.Double[] coordinates, double[] rotate,
                                     StyledDocument text,
                                     Map<AttributeKey<?>, Object> attributes) throws IOException {
        IXMLElement elem = doc.createElement("text");
        StringBuilder bufX = new StringBuilder();
        StringBuilder bufY = new StringBuilder();
        for (int i = 0; i < coordinates.length; i++) {
            if (i != 0) {
                bufX.append(',');
                bufY.append(',');
            }
            bufX.append(toNumber(coordinates[i].getX()));
            bufY.append(toNumber(coordinates[i].getY()));
        }
        StringBuilder bufR = new StringBuilder();
        if (rotate != null) {
            for (int i = 0; i < rotate.length; i++) {
                if (i != 0) {
                    bufR.append(',');
                }
                bufR.append(toNumber(rotate[i]));
            }
        }
        writeAttribute(elem, "x", bufX.toString(), "0");
        writeAttribute(elem, "y", bufY.toString(), "0");
        writeAttribute(elem, "rotate", bufR.toString(), "");
        String str;
        try {
            str = text.getText(0, text.getLength());
        } catch (BadLocationException e) {
            InternalError error = new InternalError(e.getMessage());
            error.initCause(e);
            throw error;
        }

        elem.setContent(str);

        writeShapeAttributes(elem, attributes);
        writeOpacityAttribute(elem, attributes);
        writeTransformAttribute(elem, attributes);
        writeFontAttributes(elem, attributes);
        return elem;
    }

    protected void writeTextAreaElement(IXMLElement parent, SVGTextAreaFigure f)
            throws IOException {
        DefaultStyledDocument styledDoc = new DefaultStyledDocument();
        try {
            styledDoc.insertString(0, f.getText(), null);
        } catch (BadLocationException e) {
            InternalError error = new InternalError(e.getMessage());
            error.initCause(e);
            throw error;
        }

        Rectangle2D.Double bounds = f.getBounds();

        parent.addChild(
                createTextArea(
                        document,
                        bounds.x, bounds.y, bounds.width, bounds.height,
                        styledDoc,
                        f.getAttributes()));
    }

    protected IXMLElement createTextArea(IXMLElement doc,
                                         double x, double y, double w, double h,
                                         StyledDocument text,
                                         Map<AttributeKey<?>, Object> attributes)
            throws IOException {
        if (true) {
            return createTextAreaWithSvgTextAreaElement(doc, x, y, w, h, text, attributes);
        } else {
            return createTextAreaWithSvgTextElement(doc, x, y, w, h, text, attributes);
        }
    }

    protected IXMLElement createTextAreaWithSvgTextAreaElement(IXMLElement doc,
                                                               double x, double y, double w, double h,
                                                               StyledDocument text,
                                                               Map<AttributeKey<?>, Object> attributes)
            throws IOException {
        IXMLElement elem = doc.createElement("textArea");
        writeAttribute(elem, "x", toNumber(x), "0");
        writeAttribute(elem, "y", toNumber(y), "0");
        writeAttribute(elem, "width", toNumber(w), "0");
        writeAttribute(elem, "height", toNumber(h), "0");

        String str;
        try {
            str = text.getText(0, text.getLength());
        } catch (BadLocationException e) {
            InternalError error = new InternalError(e.getMessage());
            error.initCause(e);
            throw error;
        }
        String[] lines = str.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i != 0) {
                elem.addChild(doc.createElement("tbreak"));
            }
            IXMLElement contentElement = doc.createElement(null);
            contentElement.setContent(lines[i]);
            elem.addChild(contentElement);
        }

        writeShapeAttributes(elem, attributes);
        writeTransformAttribute(elem, attributes);
        writeOpacityAttribute(elem, attributes);
        writeFontAttributes(elem, attributes);
        return elem;
    }

    protected IXMLElement createTextAreaWithSvgTextElement(IXMLElement doc,
                                                           double x, double y, double w, double h,
                                                           StyledDocument text,
                                                           Map<AttributeKey<?>, Object> a)
            throws IOException {
        IXMLElement elem = doc.createElement("text");
        writeAttribute(elem, "x", toNumber(x), "0");
        writeAttribute(elem, "y", toNumber(y), "0");
        String str;
        try {
            str = text.getText(0, text.getLength());
        } catch (BadLocationException e) {
            InternalError error = new InternalError(e.getMessage());
            error.initCause(e);
            throw error;
        }

        drawText(doc, elem, str, x, y, w, h, FONT_FACE.get(a),
                8,
                FONT_UNDERLINE.get(a), false, SVGAttributeKeys.TextAlign.START, 1.0);
        writeShapeAttributes(elem, a);
        writeTransformAttribute(elem, a);
        writeOpacityAttribute(elem, a);
        writeFontAttributes(elem, a);
        return elem;

    }

    private void drawText(IXMLElement doc, IXMLElement parent, String str, double x, double y, double w, double h,
                          Font tfont, int tabSize, boolean isUnderlined,
                          boolean isStrikethrough,
                          SVGAttributeKeys.TextAlign textAlignment, double lineSpacing) {
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        java.awt.Font font = new java.awt.Font(tfont.getName(), java.awt.Font.PLAIN, (int) tfont.getSize()).deriveFont((float) tfont.getSize());
        float leftMargin = (float) x;
        float rightMargin = (float) Math.max(leftMargin + 1, x + w + 1);
        float verticalPos = (float) y;
        float maxVerticalPos = (float) (y + h);
        if (leftMargin < rightMargin) {
            //float tabWidth = (float) (getTabSize() * g.getFontMetrics(font).charWidth('m'));
            float tabWidth = (float) (tabSize * font.getStringBounds("m", frc).getWidth());
            float[] tabStops = new float[(int) (w / tabWidth)];
            for (int i = 0; i < tabStops.length; i++) {
                tabStops[i] = (float) (x + (int) (tabWidth * (i + 1)));
            }

            if (str != null) {
                String[] paragraphs = str.split("\n");

                for (int i = 0; i < paragraphs.length; i++) {
                    if (paragraphs[i].length() == 0) {
                        paragraphs[i] = " ";
                    }
                    AttributedString as = new AttributedString(paragraphs[i]);
                    as.addAttribute(TextAttribute.FONT, font);
                    if (isUnderlined) {
                        as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
                    }
                    if (isStrikethrough) {
                        as.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
                    }
                    int tabCount = paragraphs[i].split("\t").length - 1;
                    Rectangle2D.Double paragraphBounds = drawParagraph(doc, parent, frc,
                            paragraphs[i], as.getIterator(), verticalPos, maxVerticalPos, leftMargin, rightMargin, tabStops, tabCount,
                            textAlignment,
                            lineSpacing);
                    verticalPos = (float) (paragraphBounds.y + paragraphBounds.height + lineSpacing);
                    if (verticalPos > maxVerticalPos) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Draws or measures a paragraph of text at the specified y location and
     * the bounds of the paragraph.
     *
     * @param styledText     the text of the paragraph.
     * @param verticalPos    the top bound of the paragraph
     * @param maxVerticalPos the bottom bound of the paragraph
     * @param leftMargin     the left bound of the paragraph
     * @param rightMargin    the right bound of the paragraph
     * @param tabStops       an array with tab stops
     * @param tabCount       the number of entries in tabStops which contain actual
     *                       values
     * @return Returns the actual bounds of the paragraph.
     */
    private Rectangle2D.Double drawParagraph(IXMLElement doc, IXMLElement parent,
                                             FontRenderContext frc, String
                                                     paragraph, AttributedCharacterIterator styledText,
                                             float verticalPos, float maxVerticalPos, float leftMargin,
                                             float rightMargin, float[] tabStops, int tabCount,
                                             SVGAttributeKeys.TextAlign textAlignment, double lineSpacing) {
        // This method is based on the code sample given
        // in the class comment of java.awt.font.LineBreakMeasurer,

        // assume styledText is an AttributedCharacterIterator, and the number
        // of tabs in styledText is tabCount

        Rectangle2D.Double paragraphBounds = new Rectangle2D.Double(leftMargin, verticalPos, 0, 0);

        int[] tabLocations = new int[tabCount + 1];

        int i = 0;
        for (char c = styledText.first(); c != AttributedCharacterIterator.DONE; c = styledText.next()) {
            if (c == '\t') {
                tabLocations[i++] = styledText.getIndex();
            }
        }
        tabLocations[tabCount] = styledText.getEndIndex() - 1;

        // Now tabLocations has an entry for every tab's offset in
        // the text.  For convenience, the last entry is tabLocations
        // is the offset of the last character in the text.

        LineBreakMeasurer measurer = new LineBreakMeasurer(styledText, frc);
        int currentTab = 0;

        int textIndex = 0;
        while (measurer.getPosition() < styledText.getEndIndex() && verticalPos <= maxVerticalPos) {

            // Lay out and draw each line.  All segments on a line
            // must be computed before any drawing can occur, since
            // we must know the largest ascent on the line.
            // TextLayouts are computed and stored in a List;
            // their horizontal positions are stored in a parallel
            // List.

            // lineContainsText is true after first segment is drawn
            boolean lineContainsText = false;
            boolean lineComplete = false;
            float maxAscent = 0, maxDescent = 0;
            float horizontalPos = leftMargin;
            LinkedList<TextLayout> layouts = new LinkedList<>();
            LinkedList<Float> penPositions = new LinkedList<>();

            int first = layouts.size();

            while (!lineComplete && verticalPos <= maxVerticalPos) {
                float wrappingWidth = rightMargin - horizontalPos;
                TextLayout layout = null;
                layout = measurer.nextLayout(wrappingWidth,
                        tabLocations[currentTab] + 1,
                        lineContainsText);

                // layout can be null if lineContainsText is true
                if (layout != null) {
                    layouts.add(layout);
                    penPositions.add(horizontalPos);
                    horizontalPos += layout.getAdvance();
                    maxAscent = Math.max(maxAscent, layout.getAscent());
                    maxDescent = Math.max(maxDescent,
                            layout.getDescent() + layout.getLeading());
                } else {
                    lineComplete = true;
                }

                lineContainsText = true;

                if (measurer.getPosition() == tabLocations[currentTab] + 1) {
                    currentTab++;
                }

                if (measurer.getPosition() == styledText.getEndIndex()) {
                    lineComplete = true;
                } else if (tabStops.length == 0 || horizontalPos >= tabStops[tabStops.length - 1]) {
                    lineComplete = true;
                }
                if (!lineComplete) {
                    // move to next tab stop
                    int j = 0;
                    while (horizontalPos >= tabStops[j]) {
                        j++;
                    }
                    horizontalPos = tabStops[j];
                }
            }
            // If there is only one layout element on the line, and we are
            // drawing, then honor alignment
            if (first == layouts.size() - 1) {
                switch (textAlignment) {
                case END:
                    penPositions.set(first, rightMargin - layouts.get(first).getVisibleAdvance() - 1);
                    break;
                case CENTER:
                    penPositions.set(first, (rightMargin - 1 - leftMargin - layouts.get(first).getVisibleAdvance()) / 2 + leftMargin);
                    break;
                case START:
                default:
                    break;
                }
            }

            verticalPos += maxAscent;
            Iterator<Float> positionEnum = penPositions.iterator();

            // now iterate through layouts and draw them
            styledText.first();
            for (TextLayout nextLayout : layouts) {
                float nextPosition = positionEnum.next();

                IXMLElement tspan = doc.createElement("tspan");
                int characterCount = nextLayout.getCharacterCount();
                tspan.setContent((paragraph.substring(textIndex, textIndex + characterCount)));
                tspan.setAttribute("x", toNumber(nextPosition));
                tspan.setAttribute("y", toNumber(verticalPos));
                parent.addChild(tspan);

                Rectangle2D layoutBounds = nextLayout.getBounds();
                paragraphBounds.add(new Rectangle2D.Double(layoutBounds.getX() + nextPosition,
                        layoutBounds.getY() + verticalPos,
                        layoutBounds.getWidth(),
                        layoutBounds.getHeight()));

                textIndex += characterCount;
            }

            verticalPos += maxDescent + lineSpacing;
        }

        return paragraphBounds;
    }

    // ------------
    // Attributes
    // ------------
    /* Writes shape attributes.
     */
    protected void writeShapeAttributes(IXMLElement elem, Map<AttributeKey<?>, Object> m)
            throws IOException {
        Color color;
        String value;
        int intValue;

        //'color'
        // Value:  	<color> | inherit
        // Initial:  	 depends on user agent
        // Applies to:  	None. Indirectly affects other properties via currentColor
        // Inherited:  	 yes
        // Percentages:  	 N/A
        // Media:  	 visual
        // Animatable:  	 yes
        // Computed value:  	 Specified <color> value, except inherit
        //
        // Nothing to do: Attribute 'color' is not needed.

        //'color-rendering'
        // Value:  	 auto | optimizeSpeed | optimizeQuality | inherit
        // Initial:  	 auto
        // Applies to:  	 container elements , graphics elements and 'animateColor'
        // Inherited:  	 yes
        // Percentages:  	 N/A
        // Media:  	 visual
        // Animatable:  	 yes
        // Computed value:  	 Specified value, except inherit
        //
        // Nothing to do: Attribute 'color-rendering' is not needed.

        // 'fill'
        // Value:  	<paint> | inherit (See Specifying paint)
        // Initial:  	 black
        // Applies to:  	 shapes and text content elements
        // Inherited:  	 yes
        // Percentages:  	 N/A
        // Media:  	 visual
        // Animatable:  	 yes
        // Computed value:  	 "none", system paint, specified <color> value or absolute IRI
        Gradient gradient = FILL_GRADIENT.get(m);
        if (gradient != null) {
            String id;
            if (gradientToIDMap.containsKey(gradient)) {
                id = gradientToIDMap.get(gradient);
            } else {
                IXMLElement gradientElem;
                if (gradient instanceof LinearGradient) {
                    LinearGradient lg = (LinearGradient) gradient;
                    gradientElem = createLinearGradient(document,
                            lg.getX1(), lg.getY1(),
                            lg.getX2(), lg.getY2(),
                            lg.getStopOffsets(),
                            lg.getStopColors(),
                            lg.getStopOpacities(),
                            lg.isRelativeToFigureBounds(),
                            lg.getTransform());
                } else /*if (gradient instanceof RadialGradient)*/ {
                    RadialGradient rg = (RadialGradient) gradient;
                    gradientElem = createRadialGradient(document,
                            rg.getCX(), rg.getCY(),
                            rg.getFX(), rg.getFY(),
                            rg.getR(),
                            rg.getStopOffsets(),
                            rg.getStopColors(),
                            rg.getStopOpacities(),
                            rg.isRelativeToFigureBounds(),
                            rg.getTransform());
                }
                id = getId(gradientElem);
                gradientElem.setAttribute("id", "xml", id);
                defs.addChild(gradientElem);
                gradientToIDMap.put(gradient, id);
            }
            writeAttribute(elem, "fill", "url(#" + id + ")", "#000");
        } else {
            writeAttribute(elem, "fill", toColor(FILL_COLOR.get(m)), "#000");
        }


        //'fill-opacity'
        //Value:  	 <opacity-value> | inherit
        //Initial:  	 1
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "fill-opacity", FILL_OPACITY.get(m), 1d);

        // 'fill-rule'
        // Value:	 nonzero | evenodd | inherit
        // Initial: 	 nonzero
        // Applies to:  	 shapes and text content elements
        // Inherited:  	 yes
        // Percentages:  	 N/A
        // Media:  	 visual
        // Animatable:  	 yes
        // Computed value:  	 Specified value, except inherit
        if (WINDING_RULE.get(m) != WindingRule.NON_ZERO) {
            writeAttribute(elem, "fill-rule", "evenodd", "nonzero");
        }

        //'stroke'
        //Value:  	<paint> | inherit (See Specifying paint)
        //Initial:  	 none
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 "none", system paint, specified <color> value
        // or absolute IRI
        gradient = STROKE_GRADIENT.get(m);
        if (gradient != null) {
            String id;
            if (gradientToIDMap.containsKey(gradient)) {
                id = gradientToIDMap.get(gradient);
            } else {
                IXMLElement gradientElem;
                if (gradient instanceof LinearGradient) {
                    LinearGradient lg = (LinearGradient) gradient;
                    gradientElem = createLinearGradient(document,
                            lg.getX1(), lg.getY1(),
                            lg.getX2(), lg.getY2(),
                            lg.getStopOffsets(),
                            lg.getStopColors(),
                            lg.getStopOpacities(),
                            lg.isRelativeToFigureBounds(),
                            lg.getTransform());
                } else /*if (gradient instanceof RadialGradient)*/ {
                    RadialGradient rg = (RadialGradient) gradient;
                    gradientElem = createRadialGradient(document,
                            rg.getCX(), rg.getCY(),
                            rg.getFX(), rg.getFY(),
                            rg.getR(),
                            rg.getStopOffsets(),
                            rg.getStopColors(),
                            rg.getStopOpacities(),
                            rg.isRelativeToFigureBounds(),
                            rg.getTransform());
                }
                id = getId(gradientElem);
                gradientElem.setAttribute("id", "xml", id);
                defs.addChild(gradientElem);
                gradientToIDMap.put(gradient, id);
            }
            writeAttribute(elem, "stroke", "url(#" + id + ")", "none");
        } else {
            writeAttribute(elem, "stroke", toColor(STROKE_COLOR.get(m)), "none");
        }

        //'stroke-dasharray'
        //Value:  	 none | <dasharray> | inherit
        //Initial:  	 none
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes (non-additive)
        //Computed value:  	 Specified value, except inherit
        double[] dashes = STROKE_DASHES.get(m);
        if (dashes != null) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < dashes.length; i++) {
                if (i != 0) {
                    buf.append(',');
                }
                buf.append(toNumber(dashes[i]));
            }
            writeAttribute(elem, "stroke-dasharray", buf.toString(), null);
        }

        //'stroke-dashoffset'
        //Value:  	<length> | inherit
        //Initial:  	 0
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-dashoffset", STROKE_DASH_PHASE.get(m), 0d);

        //'stroke-linecap'
        //Value:  	 butt | round | square | inherit
        //Initial:  	 butt
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-linecap", strokeLinecapMap.get(STROKE_CAP.get(m)), "butt");

        //'stroke-linejoin'
        //Value:  	 miter | round | bevel | inherit
        //Initial:  	 miter
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-linejoin", strokeLinejoinMap.get(STROKE_JOIN.get(m)), "miter");

        //'stroke-miterlimit'
        //Value:  	 <miterlimit> | inherit
        //Initial:  	 4
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-miterlimit", STROKE_MITER_LIMIT.get(m), 4d);

        //'stroke-opacity'
        //Value:  	 <opacity-value> | inherit
        //Initial:  	 1
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-opacity", STROKE_OPACITY.get(m), 1d);

        //'stroke-width'
        //Value:  	<length> | inherit
        //Initial:  	 1
        //Applies to:  	 shapes and text content elements
        //Inherited:  	 yes
        //Percentages:  	 N/A
        //Media:  	 visual
        //Animatable:  	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "stroke-width", STROKE_WIDTH.get(m), 1d);
    }
    /* Writes the opacity attribute.
     */

    protected void writeOpacityAttribute(IXMLElement elem, Map<AttributeKey<?>, Object> m)
            throws IOException {
        //'opacity'
        //Value:  	<opacity-value> | inherit
        //Initial:  	1
        //Applies to:  	 'image' element
        //Inherited:  	no
        //Percentages:  	N/A
        //Media:  	visual
        //Animatable:  	yes
        //Computed value:  	 Specified value, except inherit
        //<opacity-value>
        //The uniform opacity setting must be applied across an entire object.
        //Any values outside the range 0.0 (fully transparent) to 1.0
        //(fully opaque) shall be clamped to this range.
        //(See Clamping values which are restricted to a particular range.)
        writeAttribute(elem, "opacity", OPACITY.get(m), 1d);
    }
    /* Writes the transform attribute as specified in
     * http://www.w3.org/TR/SVGMobile12/coords.html#TransformAttribute
     *
     */

    protected void writeTransformAttribute(IXMLElement elem, Map<AttributeKey<?>, Object> a)
            throws IOException {
        AffineTransform t = TRANSFORM.get(a);
        if (t != null) {
            writeAttribute(elem, "transform", toTransform(t), "none");
        }
    }
    /* Writes font attributes as listed in
     * http://www.w3.org/TR/SVGMobile12/feature.html#Font
     */

    private void writeFontAttributes(IXMLElement elem, Map<AttributeKey<?>, Object> a)
            throws IOException {
        String value;
        double doubleValue;

        // 'font-family'
        // Value:  	[[ <family-name> |
        // <generic-family> ],]* [<family-name> |
        // <generic-family>] | inherit
        // Initial:  	depends on user agent
        // Applies to:  	text content elements
        // Inherited:  	yes
        // Percentages:  	N/A
        // Media:  	visual
        // Animatable:  	yes
        // Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "font-family", FONT_FACE.get(a).getFontName(), "Dialog");

        // 'font-getChildCount'
        // Value:  	<absolute-getChildCount> | <relative-getChildCount> |
        // <length> | inherit
        // Initial:  	medium
        // Applies to:  	text content elements
        // Inherited:  	yes, the computed value is inherited
        // Percentages:  	N/A
        // Media:  	visual
        // Animatable:  	yes
        // Computed value:  	 Absolute length
        writeAttribute(elem, "font-size", FONT_SIZE.get(a), 0d);

        // 'font-style'
        // Value:  	normal | italic | oblique | inherit
        // Initial:  	normal
        // Applies to:  	text content elements
        // Inherited:  	yes
        // Percentages:  	N/A
        // Media:  	visual
        // Animatable:  	yes
        // Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "font-style", (FONT_ITALIC.get(a)) ? "italic" : "normal", "normal");


        //'font-variant'
        //Value:  	normal | small-caps | inherit
        //Initial:  	normal
        //Applies to:  	text content elements
        //Inherited:  	yes
        //Percentages:  	N/A
        //Media:  	visual
        //Animatable:  	no
        //Computed value:  	 Specified value, except inherit
        // XXX - Implement me
        writeAttribute(elem, "font-variant", "normal", "normal");

        // 'font-weight'
        // Value:  	normal | bold | bolder | lighter | 100 | 200 | 300
        // | 400 | 500 | 600 | 700 | 800 | 900 | inherit
        // Initial:  	normal
        // Applies to:  	text content elements
        // Inherited:  	yes
        // Percentages:  	N/A
        // Media:  	visual
        // Animatable:  	yes
        // Computed value:  	 one of the legal numeric values, non-numeric
        // values shall be converted to numeric values according to the rules
        // defined below.
        writeAttribute(elem, "font-weight", (FONT_BOLD.get(a)) ? "bold" : "normal", "normal");

        // Note: text-decoration is an SVG 1.1 feature
        //'text-decoration'
        //Value:  	none | [ underline || overline || line-through || blink ] | inherit
        //Initial:  	none
        //Applies to:  	text content elements
        //Inherited:  	no (see prose)
        //Percentages:  	N/A
        //Media:  	visual
        //Animatable:  	yes
        writeAttribute(elem, "text-decoration", (FONT_UNDERLINE.get(a)) ? "underline" : "none", "none");
    }
    /* Writes viewport attributes.
     */

    private void writeViewportAttributes(IXMLElement elem, Map<AttributeKey<?>, Object> a)
            throws IOException {
        Object value;
        Double doubleValue;

        if (VIEWPORT_WIDTH.get(a) != null && VIEWPORT_HEIGHT.get(a) != null) {
            // width of the viewport
            writeAttribute(elem, "width", toNumber(VIEWPORT_WIDTH.get(a)), null);
            // height of the viewport
            writeAttribute(elem, "height", toNumber(VIEWPORT_HEIGHT.get(a)), null);
        }
        //'viewport-fill'
        //Value:	 "none" | <color> | inherit
        //Initial:	 none
        //Applies to:	viewport-creating elements
        //Inherited:	 no
        //Percentages:	 N/A
        //Media:	 visual
        //Animatable:	 yes
        //Computed value:  	 "none" or specified <color> value, except inherit
        writeAttribute(elem, "viewport-fill", toColor(VIEWPORT_FILL.get(a)), "none");

        //'viewport-fill-opacity'
        //Value:	<opacity-value> | inherit
        //Initial:	 1.0
        //Applies to:	viewport-creating elements
        //Inherited:	 no
        //Percentages:	 N/A
        //Media:	 visual
        //Animatable:	 yes
        //Computed value:  	 Specified value, except inherit
        writeAttribute(elem, "viewport-fill-opacity", VIEWPORT_FILL_OPACITY.get(a), 1.0);

    }

    protected void writeAttribute(IXMLElement elem, String name, String value, @Nullable String defaultValue) {
        writeAttribute(elem, name, SVG_NAMESPACE, value, defaultValue);
    }

    protected void writeAttribute(IXMLElement elem, String name, String namespace, String value, @Nullable String defaultValue) {
        if (!value.equals(defaultValue)) {
            elem.setAttribute(name, value);
        }
    }

    protected void writeAttribute(IXMLElement elem, String name, double value, double defaultValue) {
        writeAttribute(elem, name, SVG_NAMESPACE, value, defaultValue);
    }

    protected void writeAttribute(IXMLElement elem, String name, String namespace, double value, double defaultValue) {
        if (value != defaultValue) {
            elem.setAttribute(name, toNumber(value));
        }
    }

    /**
     * Returns a value as a SVG Path attribute.
     * as specified in http://www.w3.org/TR/SVGMobile12/paths.html#PathDataBNF
     */
    public static String toPath(BezierPath[] paths) {
        StringBuilder buf = new StringBuilder();

        for (int j = 0; j < paths.length; j++) {
            BezierPath path = paths[j];

            if (path.size() == 0) {
                // nothing to do
            } else if (path.size() == 1) {
                BezierPath.Node current = path.get(0);
                buf.append("M ");
                buf.append(toNumber(current.x[0]));
                buf.append(' ');
                buf.append(toNumber(current.y[0]));
                //buf.append(" L ");
                buf.append(toNumber(current.x[0]));
                buf.append(' ');
                buf.append(toNumber(current.y[0] + 1));
            } else {
                BezierPath.Node previous;
                BezierPath.Node current;

                previous = current = path.get(0);
                buf.append("M ");
                buf.append(toNumber(current.x[0]));
                buf.append(' ');
                buf.append(toNumber(current.y[0]));
                char nextCommand = 'L';
                for (int i = 1, n = path.size(); i < n; i++) {
                    previous = current;
                    current = path.get(i);

                    if ((previous.mask & BezierPath.C2_MASK) == 0) {
                        if ((current.mask & BezierPath.C1_MASK) == 0) {
                            if (nextCommand != 'L') {
                                buf.append(" L ");
                                nextCommand = 'L';
                            } else {
                                buf.append(' ');
                            }
                            buf.append(toNumber(current.x[0]));
                            buf.append(' ');
                            buf.append(toNumber(current.y[0]));
                        } else {
                            if (nextCommand != 'Q') {
                                buf.append(" Q ");
                                nextCommand = 'Q';
                            } else {
                                buf.append(' ');
                            }
                            buf.append(toNumber(current.x[1]));
                            buf.append(' ');
                            buf.append(toNumber(current.y[1]));
                            buf.append(' ');
                            buf.append(toNumber(current.x[0]));
                            buf.append(' ');
                            buf.append(toNumber(current.y[0]));
                        }
                    } else {
                        if ((current.mask & BezierPath.C1_MASK) == 0) {
                            if (nextCommand != 'Q') {
                                buf.append(" Q ");
                                nextCommand = 'Q';
                            } else {
                                buf.append(' ');
                            }
                            buf.append(toNumber(previous.x[2]));
                            buf.append(' ');
                            buf.append(toNumber(previous.y[2]));
                            buf.append(' ');
                            buf.append(toNumber(current.x[0]));
                            buf.append(' ');
                            buf.append(toNumber(current.y[0]));
                        } else {
                            if (nextCommand != 'C') {
                                buf.append(" C ");
                                nextCommand = 'C';
                            } else {
                                buf.append(' ');
                            }
                            buf.append(toNumber(previous.x[2]));
                            buf.append(' ');
                            buf.append(toNumber(previous.y[2]));
                            buf.append(' ');
                            buf.append(toNumber(current.x[1]));
                            buf.append(' ');
                            buf.append(toNumber(current.y[1]));
                            buf.append(' ');
                            buf.append(toNumber(current.x[0]));
                            buf.append(' ');
                            buf.append(toNumber(current.y[0]));
                        }
                    }
                }
                if (path.isClosed()) {
                    if (path.size() > 1) {
                        previous = path.get(path.size() - 1);
                        current = path.get(0);

                        if ((previous.mask & BezierPath.C2_MASK) == 0) {
                            if ((current.mask & BezierPath.C1_MASK) == 0) {
                                if (nextCommand != 'L') {
                                    buf.append(" L ");
                                    nextCommand = 'L';
                                } else {
                                    buf.append(' ');
                                }
                                buf.append(toNumber(current.x[0]));
                                buf.append(' ');
                                buf.append(toNumber(current.y[0]));
                            } else {
                                if (nextCommand != 'Q') {
                                    buf.append(" Q ");
                                    nextCommand = 'Q';
                                } else {
                                    buf.append(' ');
                                }
                                buf.append(toNumber(current.x[1]));
                                buf.append(' ');
                                buf.append(toNumber(current.y[1]));
                                buf.append(' ');
                                buf.append(toNumber(current.x[0]));
                                buf.append(' ');
                                buf.append(toNumber(current.y[0]));
                            }
                        } else {
                            if ((current.mask & BezierPath.C1_MASK) == 0) {
                                if (nextCommand != 'Q') {
                                    buf.append(" Q ");
                                    nextCommand = 'Q';
                                } else {
                                    buf.append(' ');
                                }
                                buf.append(toNumber(previous.x[2]));
                                buf.append(' ');
                                buf.append(toNumber(previous.y[2]));
                                buf.append(' ');
                                buf.append(toNumber(current.x[0]));
                                buf.append(' ');
                                buf.append(toNumber(current.y[0]));
                            } else {
                                if (nextCommand != 'C') {
                                    buf.append(" C ");
                                    nextCommand = 'C';
                                } else {
                                    buf.append(' ');
                                }
                                buf.append(toNumber(previous.x[2]));
                                buf.append(' ');
                                buf.append(toNumber(previous.y[2]));
                                buf.append(' ');
                                buf.append(toNumber(current.x[1]));
                                buf.append(' ');
                                buf.append(toNumber(current.y[1]));
                                buf.append(' ');
                                buf.append(toNumber(current.x[0]));
                                buf.append(' ');
                                buf.append(toNumber(current.y[0]));
                            }
                        }
                    }
                    buf.append(" Z");
                    nextCommand = '\0';
                }
            }
        }
        return buf.toString();
    }

    /**
     * Returns a double array as a number attribute value.
     */
    public static String toNumber(double number) {
        String str = (isFloatPrecision) ? Float.toString((float) number) : Double.toString(number);
        if (str.endsWith(".0")) {
            str = str.substring(0, str.length() - 2);
        }
        return str;
    }

    /**
     * Returns a Point2D.Double array as a Points attribute value.
     * as specified in http://www.w3.org/TR/SVGMobile12/shapes.html#PointsBNF
     */
    public static String toPoints(Point2D.Double[] points) throws IOException {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < points.length; i++) {
            if (i != 0) {
                buf.append(", ");
            }
            buf.append(toNumber(points[i].x));
            buf.append(',');
            buf.append(toNumber(points[i].y));
        }
        return buf.toString();
    }
    /* Converts an AffineTransform into an SVG transform attribute value as specified in
     * http://www.w3.org/TR/SVGMobile12/coords.html#TransformAttribute
     */

    public static String toTransform(AffineTransform t) throws IOException {
        StringBuilder buf = new StringBuilder();
        switch (t.getType()) {
        case AffineTransform.TYPE_IDENTITY:
            buf.append("none");
            break;
        case AffineTransform.TYPE_TRANSLATION:
            // translate(<tx> [<ty>]), specifies a translation by tx and ty.
            // If <ty> is not provided, it is assumed to be zero.
            buf.append("translate(");
            buf.append(toNumber(t.getTranslateX()));
            if (t.getTranslateY() != 0d) {
                buf.append(' ');
                buf.append(toNumber(t.getTranslateY()));
            }
            buf.append(')');
            break;
            /*
            case AffineTransform.TYPE_GENERAL_ROTATION :
            case AffineTransform.TYPE_QUADRANT_ROTATION :
            case AffineTransform.TYPE_MASK_ROTATION :
            // rotate(<rotate-angle> [<cx> <cy>]), specifies a rotation by
            // <rotate-angle> degrees about a given point.
            // If optional parameters <cx> and <cy> are not supplied, the
            // rotate is about the origin of the current user coordinate
            // system. The operation corresponds to the matrix
            // [cos(a) sin(a) -sin(a) cos(a) 0 0].
            // If optional parameters <cx> and <cy> are supplied, the rotate
            // is about the point (<cx>, <cy>). The operation represents the
            // equivalent of the following specification:
            // translate(<cx>, <cy>) rotate(<rotate-angle>)
            // translate(-<cx>, -<cy>).
            buf.append("rotate(");
            buf.append(toNumber(t.getScaleX()));
            buf.append(')');
            break;*/
        case AffineTransform.TYPE_UNIFORM_SCALE:
            // scale(<sx> [<sy>]), specifies a scale operation by sx
            // and sy. If <sy> is not provided, it is assumed to be equal
            // to <sx>.
            buf.append("scale(");
            buf.append(toNumber(t.getScaleX()));
            buf.append(')');
            break;
        case AffineTransform.TYPE_GENERAL_SCALE:
        case AffineTransform.TYPE_MASK_SCALE:
            // scale(<sx> [<sy>]), specifies a scale operation by sx
            // and sy. If <sy> is not provided, it is assumed to be equal
            // to <sx>.
            buf.append("scale(");
            buf.append(toNumber(t.getScaleX()));
            buf.append(' ');
            buf.append(toNumber(t.getScaleY()));
            buf.append(')');
            break;
        default:
            // matrix(<a> <b> <c> <d> <e> <f>), specifies a transformation
            // in the form of a transformation matrix of six values.
            // matrix(a,b,c,d,e,f) is equivalent to applying the
            // transformation matrix [a b c d e f].
            buf.append("matrix(");
            double[] matrix = new double[6];
            t.getMatrix(matrix);
            for (int i = 0; i < matrix.length; i++) {
                if (i != 0) {
                    buf.append(' ');
                }
                buf.append(toNumber(matrix[i]));
            }
            buf.append(')');
            break;
        }

        return buf.toString();
    }

    public static String toColor(Color color) {
        if (color == null) {
            return "none";
        }


        String value;
        value = "000000" + Integer.toHexString(color.getRGB());
        value = "#" + value.substring(value.length() - 6);
        if (value.charAt(1) == value.charAt(2)
                && value.charAt(3) == value.charAt(4)
                && value.charAt(5) == value.charAt(6)) {
            value = "#" + value.charAt(1) + value.charAt(3) + value.charAt(5);
        }
        return value;
    }

    @Override
    public String getFileExtension() {
        return "svg";
    }

    @Override
    public void write(URI uri, Drawing drawing) throws IOException {
        write(new File(uri), drawing);
    }

    public void write(File file, Drawing drawing) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(file));
        try {
            write(out, drawing);
        } finally {
            out.close();
        }
    }

    @Override
    public void write(OutputStream out, Drawing drawing) throws IOException {
        write(out, drawing, drawing.getChildren());
    }

    /**
     * All other write methods delegate their work to here.
     */
    public void write(OutputStream out, Drawing drawing, java.util.List<Figure> figures) throws IOException {
        document = new XMLElement("svg", SVG_NAMESPACE);
        document.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
        document.setAttribute("version", "1.2");
        document.setAttribute("baseProfile", "tiny");
        writeViewportAttributes(document, drawing.getAttributes());

        initStorageContext(document);

        defs = new XMLElement("defs");
        document.addChild(defs);

        for (Figure f : figures) {
            writeElement(document, f);
        }

        // Write XML prolog
        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(out, "UTF-8"));
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        // Write XML content
        XMLWriter xmlWriter = new XMLWriter(writer);
        xmlWriter.setIndentation("  ");
        xmlWriter.write(document, isPrettyPrint);

        // Flush writer
        writer.flush();
        document.dispose();
    }

    private void initStorageContext(IXMLElement root) {
        identifiedElements = new HashMap<IXMLElement, String>();
        gradientToIDMap = new HashMap<Gradient, String>();
    }

    /**
     * Gets a unique ID for the specified element.
     */
    public String getId(IXMLElement element) {
        if (identifiedElements.containsKey(element)) {
            return identifiedElements.get(element);
        } else {
            String id = Integer.toString(nextId++, Character.MAX_RADIX);
            identifiedElements.put(element, id);
            return id;
        }
    }

    @Override
    public Transferable createTransferable(Drawing drawing, java.util.List<Figure> figures, double scaleFactor) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        write(buf, drawing, figures);
        return new InputStreamTransferable(new DataFlavor(SVG_MIMETYPE, "Image SVG"), buf.toByteArray());
    }
}
