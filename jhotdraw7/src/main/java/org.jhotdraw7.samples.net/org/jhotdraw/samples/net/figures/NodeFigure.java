/* @(#)NodeFigure.java
 * Copyright © The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw.samples.net.figures;

import org.jhotdraw.draw.AttributeKey;
import org.jhotdraw.draw.ConnectionFigure;
import org.jhotdraw.draw.LineConnectionFigure;
import org.jhotdraw.draw.RectangleFigure;
import org.jhotdraw.draw.TextFigure;
import org.jhotdraw.draw.connector.Connector;
import org.jhotdraw.draw.connector.LocatorConnector;
import org.jhotdraw.draw.handle.BoundsOutlineHandle;
import org.jhotdraw.draw.handle.ConnectorHandle;
import org.jhotdraw.draw.handle.Handle;
import org.jhotdraw.draw.handle.MoveHandle;
import org.jhotdraw.draw.locator.RelativeLocator;
import org.jhotdraw.geom.Geom;
import org.jhotdraw.geom.Insets2D;
import org.jhotdraw.samples.net.NetLabels;
import org.jhotdraw.util.ResourceBundleUtil;
import org.jhotdraw.xml.DOMInput;
import org.jhotdraw.xml.DOMOutput;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import static org.jhotdraw.draw.AttributeKeys.DECORATOR_INSETS;

/**
 * NodeFigure.
 *
 * @author Werner Randelshofer
 * @version $Id$
 */
public class NodeFigure extends TextFigure {
    private static final long serialVersionUID = 1L;

    private LinkedList<Connector> connectors;
    private static LocatorConnector north;
    private static LocatorConnector south;
    private static LocatorConnector east;
    private static LocatorConnector west;

    /** Creates a new instance. */
    public NodeFigure() {
        RectangleFigure rf = new RectangleFigure();
        setDecorator(rf);
        createConnectors();
        set(DECORATOR_INSETS, new Insets2D.Double(6, 10, 6, 10));
        ResourceBundleUtil labels = NetLabels.getLabels();
        setText(labels.getString("nodeDefaultName"));
        setAttributeEnabled(DECORATOR_INSETS, false);
    }

    private void createConnectors() {
        connectors = new LinkedList<Connector>();
        connectors.add(new LocatorConnector(this, RelativeLocator.north()));
        connectors.add(new LocatorConnector(this, RelativeLocator.east()));
        connectors.add(new LocatorConnector(this, RelativeLocator.west()));
        connectors.add(new LocatorConnector(this, RelativeLocator.south()));
    }

    @Override
    public Collection<Connector> getConnectors(ConnectionFigure prototype) {
        return Collections.unmodifiableList(connectors);
    }

    @Override
    public Collection<Handle> createHandles(int detailLevel) {
        java.util.List<Handle> handles = new LinkedList<Handle>();
        switch (detailLevel) {
            case -1:
                handles.add(new BoundsOutlineHandle(getDecorator(), false, true));
                break;
            case 0:
                handles.add(new MoveHandle(this, RelativeLocator.northWest()));
                handles.add(new MoveHandle(this, RelativeLocator.northEast()));
                handles.add(new MoveHandle(this, RelativeLocator.southWest()));
                handles.add(new MoveHandle(this, RelativeLocator.southEast()));
                for (Connector c : connectors) {
                    handles.add(new ConnectorHandle(c, new LineConnectionFigure()));
                }
                break;
        }
        return handles;
    }

    @Override
    public Rectangle2D.Double getFigureDrawingArea() {
        Rectangle2D.Double b = super.getFigureDrawingArea();
        // Grow for connectors
        Geom.grow(b, 10d, 10d);
        return b;
    }

    @Override
    public Connector findConnector(Point2D.Double p, ConnectionFigure figure) {
        // return closest connector
        double min = Double.MAX_VALUE;
        Connector closest = null;
        for (Connector c : connectors) {
            Point2D.Double p2 = Geom.center(c.getBounds());
            double d = Geom.length2(p.x, p.y, p2.x, p2.y);
            if (d < min) {
                min = d;
                closest = c;
            }
        }
        return closest;
    }

    @Override
    public Connector findCompatibleConnector(Connector c, boolean isStart) {
        if (c instanceof LocatorConnector) {
            LocatorConnector lc = (LocatorConnector) c;
            for (Connector cc : connectors) {
                LocatorConnector lcc = (LocatorConnector) cc;
                if (lcc.getLocator().equals(lc.getLocator())) {
                    return lcc;
                }
            }
        }
        return connectors.getFirst();
    }

    @Override
    public NodeFigure clone() {
        NodeFigure that = (NodeFigure) super.clone();
        that.createConnectors();
        return that;
    }

    @Override
    public int getLayer() {
        return -1; // stay below ConnectionFigures
    }

    @Override
    protected void writeDecorator(DOMOutput out) throws IOException {
        // do nothing
    }

    @Override
    protected void readDecorator(DOMInput in) throws IOException {
        // do nothing
    }

    @Override
    public <T> void set(AttributeKey<T> key, T newValue) {
        super.set(key, newValue);
        if (getDecorator() != null) {
            getDecorator().set(key, newValue);
        }
    }
}

