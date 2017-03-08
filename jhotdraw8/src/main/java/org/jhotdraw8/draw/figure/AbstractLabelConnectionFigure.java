/* @(#)AbstractLabelConnectionFigure.java
 * Copyright (c) 2017 by the authors and contributors of JHotDraw.
 * You may only use this file in compliance with the accompanying license terms.
 */
package org.jhotdraw8.draw.figure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.scene.transform.Transform;
import org.jhotdraw8.draw.connector.Connector;
import org.jhotdraw8.draw.handle.BoundsInLocalOutlineHandle;
import org.jhotdraw8.draw.handle.Handle;
import org.jhotdraw8.draw.handle.HandleType;
import org.jhotdraw8.draw.handle.LineConnectorHandle;
import org.jhotdraw8.draw.handle.MoveHandle;
import org.jhotdraw8.draw.key.DirtyBits;
import org.jhotdraw8.draw.key.DirtyMask;
import org.jhotdraw8.draw.key.DoubleStyleableFigureKey;
import org.jhotdraw8.draw.key.EnumStyleableFigureKey;
import org.jhotdraw8.draw.key.Point2DStyleableMapAccessor;
import org.jhotdraw8.draw.key.SimpleFigureKey;
import org.jhotdraw8.draw.locator.RelativeLocator;
import org.jhotdraw8.geom.Geom;
import org.jhotdraw8.geom.Transforms;

/**
 * AbstractLabelConnectionFigure.
 *
 * @author Werner Randelshofer
 * @version $$Id$$
 */
public abstract class AbstractLabelConnectionFigure extends AbstractLabelFigure
        implements ConnectingFigure {

    /**
     * The horizontal position of the text. Default value: {@code baseline}
     */
    public static EnumStyleableFigureKey<HPos> TEXT_HPOS = new EnumStyleableFigureKey<>("textHPos", HPos.class, DirtyMask.of(DirtyBits.NODE, DirtyBits.LAYOUT), false,HPos.LEFT);

    /**
     * The label target.
     */
    public static SimpleFigureKey<Figure> LABEL_TARGET = new SimpleFigureKey<>("labelTarget", Figure.class, DirtyMask.of(DirtyBits.STATE, DirtyBits.LAYOUT_SUBJECT, DirtyBits.LAYOUT, DirtyBits.LAYOUT_OBSERVERS, DirtyBits.TRANSFORM), null);
    /**
     * The connector.
     */
    public static SimpleFigureKey<Connector> LABEL_CONNECTOR = new SimpleFigureKey<>("labelConnector", Connector.class, DirtyMask.of(DirtyBits.STATE, DirtyBits.LAYOUT_SUBJECT, DirtyBits.LAYOUT, DirtyBits.LAYOUT_OBSERVERS, DirtyBits.TRANSFORM), null);
    public final static DoubleStyleableFigureKey LABELED_LOCATION_X = new DoubleStyleableFigureKey("labeledLocationX", DirtyMask.of(DirtyBits.NODE, DirtyBits.LAYOUT, DirtyBits.LAYOUT_OBSERVERS), 0.0);
    public final static DoubleStyleableFigureKey LABELED_LOCATION_Y = new DoubleStyleableFigureKey("labeledLocationY", DirtyMask.of(DirtyBits.NODE, DirtyBits.LAYOUT, DirtyBits.LAYOUT_OBSERVERS), 0.0);
    public final static Point2DStyleableMapAccessor LABELED_LOCATION = new Point2DStyleableMapAccessor("labeledLocation", LABELED_LOCATION_X, LABELED_LOCATION_Y);

    /**
     * The perpendicular offset of the label.
     */
    public final static DoubleStyleableFigureKey LABEL_OFFSET = new DoubleStyleableFigureKey("labelOffset", DirtyMask.of(DirtyBits.NODE, DirtyBits.LAYOUT, DirtyBits.LAYOUT_OBSERVERS), 13.0);
    /**
     * Holds a strong reference to the property.
     */
    private Property<Figure> labelTargetProperty;
    /**
     * Holds a strong reference to the property.
     */
    private Property<Connector> labelConnectorProperty;
    private final ReadOnlyBooleanWrapper connected = new ReadOnlyBooleanWrapper();

    public AbstractLabelConnectionFigure() {
        // We must update the start and end point when ever one of
        // the connection targets changes
        ChangeListener<Figure> clTarget = (observable, oldValue, newValue) -> {
            if (oldValue != null && get(LABEL_TARGET) != oldValue) {
                oldValue.getLayoutObservers().remove(AbstractLabelConnectionFigure.this);
            }
            if (newValue != null) {
                newValue.getLayoutObservers().add(AbstractLabelConnectionFigure.this);
            }
            updateConnectedProperty();
        };
        ChangeListener<Connector> clConnector = (observable, oldValue, newValue) -> {
            updateConnectedProperty();
        };
        labelTargetProperty = LABEL_TARGET.propertyAt(getProperties());
        labelTargetProperty.addListener(clTarget);
        labelConnectorProperty = LABEL_CONNECTOR.propertyAt(getProperties());
        labelConnectorProperty.addListener(clConnector);

        connected.addListener((o, oldv, newv) -> {
            if (newv) {
                connectNotify();
            } else {
                disconnectNotify();
            }
        });
    }

    /**
     * This method is called, when connectedProperty becomes true. This
     * implementation is empty.
     */
    protected void connectNotify() {
    }

    /**
     * This method is called, when connectedProperty becomes false. This
     * implementation is empty.
     */
    protected void disconnectNotify() {
    }

    private void updateConnectedProperty() {
        connected.set(get(LABEL_CONNECTOR) != null
                && get(LABEL_TARGET) != null);
    }

    /**
     * This property is true when the figure is connected.
     *
     * @return the connected property
     */
    public ReadOnlyBooleanProperty connectedProperty() {
        return connected.getReadOnlyProperty();
    }

    @Override
    public void createHandles(HandleType handleType, List<Handle> list) {
        if (handleType == HandleType.MOVE) {
            list.add(new BoundsInLocalOutlineHandle(this, Handle.STYLECLASS_HANDLE_MOVE_OUTLINE));
            if (get(LABEL_CONNECTOR) == null) {
                list.add(new MoveHandle(this, RelativeLocator.northEast()));
                list.add(new MoveHandle(this, RelativeLocator.northWest()));
                list.add(new MoveHandle(this, RelativeLocator.southEast()));
                list.add(new MoveHandle(this, RelativeLocator.southWest()));
            }
        } else if (handleType == HandleType.RESIZE) {
            list.add(new BoundsInLocalOutlineHandle(this, Handle.STYLECLASS_HANDLE_MOVE_OUTLINE));
            list.add(new LineConnectorHandle(this, LABELED_LOCATION, LABEL_CONNECTOR, LABEL_TARGET));
        } else if (handleType == HandleType.POINT) {
            list.add(new BoundsInLocalOutlineHandle(this, Handle.STYLECLASS_HANDLE_MOVE_OUTLINE));
            list.add(new LineConnectorHandle(this, Handle.STYLECLASS_HANDLE_POINT, Handle.STYLECLASS_HANDLE_POINT_CONNECTED, LABELED_LOCATION, LABEL_CONNECTOR, LABEL_TARGET));
        } else {
            super.createHandles(handleType, list);
        }
    }

    /**
     * Returns all figures which are connected by this figure - they provide to
     * the layout of this figure.
     *
     * @return a list of connected figures
     */
    @Override
    public Set<Figure> getLayoutSubjects() {
        HashSet<Figure> ctf = new HashSet<>();
        if (get(LABEL_TARGET) != null) {
            ctf.add(get(LABEL_TARGET));
        }
        return ctf;
    }

    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public boolean isGroupReshapeableWith(Set<Figure> others) {
        for (Figure f : getLayoutSubjects()) {
            if (others.contains(f)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isLayoutable() {
        return true;
    }

    @Override
    public void layout() {
        Point2D labeledLoc = get(LABELED_LOCATION);
        Connector labelConnector = get(LABEL_CONNECTOR);
        Figure labelTarget = get(LABEL_TARGET);
        Point2D perp;
        if (labelConnector != null && labelTarget != null) {
            labeledLoc = labelConnector.getPositionInWorld(this, labelTarget);
            perp = Geom.perp(Transforms.deltaTransform(getWorldToLocal(), labelConnector.getTangentInWorld(this, labelTarget)));
        } else {
            perp = new Point2D(0, -1);
        }

        set(LABELED_LOCATION, labeledLoc);
        Bounds b = getLayoutBounds();
        double tx = 0;
        switch (getStyled(TEXT_HPOS)) {
            case CENTER:
                tx = b.getWidth() * -0.5;
                break;
            case LEFT:
                break;
            case RIGHT:
                tx = -b.getWidth();
                break;
        }
        Point2D origin = labeledLoc.add(perp.multiply(getStyled(LABEL_OFFSET))).add(tx,0);
        set(ORIGIN, origin);
    }

    @Override
    public void removeAllLayoutSubjects() {
        set(LABEL_TARGET, null);
    }

    @Override
    public void removeLayoutSubject(Figure subject) {

        if (subject == get(LABEL_TARGET)) {
            set(LABEL_TARGET, null);
        }

    }

    @Override
    public void reshapeInLocal(Transform transform) {
        if (get(LABEL_TARGET) == null) {
            set(ORIGIN, transform.transform(get(ORIGIN)));
            set(LABELED_LOCATION, get(ORIGIN));
        }
    }

    @Override
    public void reshapeInLocal(double x, double y, double width, double height) {
        if (get(LABEL_TARGET) == null) {
            set(ORIGIN, new Point2D(x, y));
            set(LABELED_LOCATION, get(ORIGIN));
        }
    }

    public void setLabelConnection(Figure target, Connector connector) {
        set(LABEL_CONNECTOR, connector);
        set(LABEL_TARGET, target);
    }
}