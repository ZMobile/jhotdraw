/* @(#)TransformHandleKit.java
 * Copyright (c) 2015 by the authors and contributors of JHotDraw.
 * You may only use this file in compliance with the accompanying license terms.
 */
package org.jhotdraw.draw.handle;

import java.util.Collection;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Transform;
import org.jhotdraw.draw.DrawingView;
import org.jhotdraw.draw.figure.Figure;
import static org.jhotdraw.draw.figure.TransformableFigure.ROTATE;
import static org.jhotdraw.draw.figure.TransformableFigure.ROTATION_AXIS;
import org.jhotdraw.draw.locator.Locator;
import org.jhotdraw.draw.locator.RelativeLocator;
import org.jhotdraw.draw.model.DrawingModel;
import static java.lang.Math.*;
import javafx.geometry.BoundingBox;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.jhotdraw.draw.figure.TransformableFigure;
import org.jhotdraw.geom.Geom;

/**
 * /**
 * A set of utility methods to create handles which transform a Figure by using
 * its {@code transform} method, if the Figure is transformable.
 *
 * @author Werner Randelshofer
 */
public class TransformHandleKit {

    /**
     * Prevent instance creation.
     */
    private TransformHandleKit() {

    }

    /**
     * Creates handles for each corner of a figure and adds them to the provided
     * collection.
     *
     * @param f the figure which will own the handles
     * @param handles the list to which the handles should be added
     */
    static public void addCornerTransformHandles(Figure f, Collection<Handle> handles) {
        handles.add(southEast(f));
        handles.add(southWest(f));
        handles.add(northEast(f));
        handles.add(northWest(f));
    }

    /**
     * Fills the given collection with handles at each the north, south, east,
     * and west of the figure.
     *
     * @param f the figure which will own the handles
     * @param handles the list to which the handles should be added
     */
    static public void addEdgeTransformHandles(Figure f, Collection<Handle> handles) {
        handles.add(south(f));
        handles.add(north(f));
        handles.add(east(f));
        handles.add(west(f));
    }

    /**
     * Fills the given collection with handles at each the north, south, east,
     * and west of the figure.
     *
     * @param f the figure which will own the handles
     * @param handles the list to which the handles should be added
     */
    static public void addResizeHandles(Figure f, Collection<Handle> handles) {
        addCornerTransformHandles(f, handles);
        addEdgeTransformHandles(f, handles);
    }

    /**
     * Creates a handle for the specified figure.
     *
     * @param owner the figure which will own the handle
     * @return the handle
     */
    static public Handle south(Figure owner) {
        return new SouthHandle(owner);
    }

    /**
     * Creates a handle for the specified figure.
     *
     * @param owner the figure which will own the handle
     * @return the handle
     */
    static public Handle southEast(Figure owner) {
        return new SouthEastHandle(owner);
    }

    /**
     * Creates a handle for the specified figure.
     *
     * @param owner the figure which will own the handle
     * @return the handle
     */
    static public Handle southWest(Figure owner) {
        return new SouthWestHandle(owner);
    }

    /**
     * Creates a handle for the specified figure.
     *
     * @param owner the figure which will own the handle
     * @return the handle
     */
    static public Handle north(Figure owner) {
        return new NorthHandle(owner);
    }

    /**
     * Creates a handle for the specified figure.
     *
     * @param owner the figure which will own the handle
     * @return the handle
     */
    static public Handle northEast(Figure owner) {
        return new NorthEastHandle(owner);
    }

    /**
     * Creates a handle for the specified figure.
     *
     * @param owner the figure which will own the handle
     * @return the handle
     */
    static public Handle northWest(Figure owner) {
        return new NorthWestHandle(owner);
    }

    /**
     * Creates a handle for the specified figure.
     *
     * @param owner the figure which will own the handle
     * @return the handle
     */
    static public Handle east(Figure owner) {
        return new EastHandle(owner);
    }

    /**
     * Creates a handle for the specified figure.
     *
     * @param owner the figure which will own the handle
     * @return the handle
     */
    static public Handle west(Figure owner) {
        return new WestHandle(owner);
    }

    private abstract static class AbstractResizeHandle extends LocatorHandle {

        private Point2D pickLocation;
        private Point2D oldPoint;
        protected final Region node;
        private final String styleclass;
        private Bounds startBounds;
        private static final Rectangle REGION_SHAPE = new Rectangle(5, 5);
        private static final Background REGION_BACKGROUND = new Background(new BackgroundFill(Color.WHITE, null, null));
        private static final Border REGION_BORDER = new Border(new BorderStroke(Color.PINK, BorderStrokeStyle.SOLID, null, null));
        /**
         * The height divided by the width.
         */
        protected double preferredAspectRatio;

        public AbstractResizeHandle(Figure owner, Locator locator) {
            this(owner, STYLECLASS_HANDLE_SCALE_TRANSLATE, locator);
        }

        public AbstractResizeHandle(Figure owner, String styleclass, Locator locator) {
            super(owner, locator);
            this.styleclass = styleclass;
            node = new Region();
            node.setShape(REGION_SHAPE);
            node.setManaged(false);
            node.setScaleShape(false);
            node.setCenterShape(true);
            node.resize(11, 11);
            node.getStyleClass().clear();
            node.getStyleClass().add(styleclass);
            node.setBorder(REGION_BORDER);
            node.setBackground(REGION_BACKGROUND);
        }

        @Override
        public Region getNode() {
            return node;
        }

        @Override
        public void updateNode(DrawingView view) {
            Figure f = getOwner();
            Transform t = view.getWorldToView().createConcatenation(f.getLocalToWorld());
            Bounds b = f.getBoundsInLocal();
            Point2D p = getLocation();
            pickLocation = p = t.transform(p);
            node.relocate(p.getX() - 5, p.getY() - 5);
            // rotates the node:
            // f.applyTransformableFigureProperties(node);
            node.setRotate(f.getStyled(ROTATE));
            node.setRotationAxis(f.getStyled(ROTATION_AXIS));
        }

        @Override
        public void onMousePressed(MouseEvent event, DrawingView view) {
            oldPoint = view.getConstrainer().constrainPoint(getOwner(), view.viewToWorld(new Point2D(event.getX(), event.getY())));
            startBounds = getOwner().getBoundsInLocal();
            preferredAspectRatio = getOwner().getPreferredAspectRatio();
        }

        @Override
        public void onMouseDragged(MouseEvent event, DrawingView view) {
            Point2D newPoint = view.viewToWorld(new Point2D(event.getX(), event.getY()));

            if (!event.isAltDown() && !event.isControlDown()) {
                // alt or control turns the constrainer off
                newPoint = view.getConstrainer().constrainPoint(getOwner(), newPoint);
            }
            if (event.isMetaDown()) {
                // meta snaps the location of the handle to the grid
                Point2D loc = getLocation();
                oldPoint = getOwner().localToWorld(loc);
            }
            // shift keeps the aspect ratio
            boolean keepAspect = event.isShiftDown();

            resize(getOwner().worldToLocal(newPoint), getOwner(), startBounds, view.getModel(), keepAspect);
        }

        @Override
        public void onMouseReleased(MouseEvent event, DrawingView dv) {
            // FIXME fire undoable edit
        }

        @Override
        public boolean isSelectable() {
            return true;
        }

        @Override
        public Point2D getLocationInView() {
            return pickLocation;
        }

        /**
         * Resizes the figure.
         *
         * @param newPoint new point in local coordinates
         * @param owner the figure
         * @param bounds the bounds of the figure on mouse pressed
         * @param model the drawing model
         * @param keepAspect whether the aspect should be preserved. The bounds
         * of the figure on mouse pressed can be used as a reference.
         */
        protected abstract void resize(Point2D newPoint, Figure owner, Bounds bounds, DrawingModel model, boolean keepAspect);

        /**
         *
         * @param model
         * @param owner
         * @param x new x in local coordinates
         * @param y new y in local coordinates
         * @param width new width in local coordinates
         * @param height new height in local coordinates
         */
        protected void transform(DrawingModel model, Figure owner, double x, double y, double width, double height) {
            if (width == 0 || height == 0) {
                return;
            }

            Bounds oldBounds = owner.getBoundsInLocal();
            Point2D oldCenter = Geom.center(oldBounds);

            double sx = width / oldBounds.getWidth();
            double sy = height / oldBounds.getHeight();

            if (Double.isNaN(sx) || Double.isNaN(sy)) {
                return;
            }
            // sx and sy scale around x and y
            //   translate2D(pivotX, pivotY);
            // scale2D(sx, sy);
            //   translate2D(-pivotX, -pivotY);

            // this translation is appended to the scale
            double tx = x - oldBounds.getMinX();
            double ty = y - oldBounds.getMinY();

            double oldTx = owner.get(TransformableFigure.TRANSLATE_X);
            double oldTy = owner.get(TransformableFigure.TRANSLATE_Y);
            double oldSx = owner.get(TransformableFigure.SCALE_X);
            double oldSy = owner.get(TransformableFigure.SCALE_Y);

            double newTx = oldTx + tx - x + sx * x;
            double newTy = oldTy + ty - y + sy * y;
            double newSx = oldSx * sx;
            double newSy = oldSy * sy;

            //model.set(owner, TransformableFigure.TRANSLATE_X, newTx);
            //model.set(owner, TransformableFigure.TRANSLATE_Y, newTy);
            model.set(owner, TransformableFigure.SCALE_X, newSx);
            model.set(owner, TransformableFigure.SCALE_Y, newSy);
            /* 
     //   System.out.println("expected:"+x+","+y+","+width+","+height);
        
        Transform t = new Scale(sx,sy,x,y);
        t = t.createConcatenation(new Translate(tx,ty));
        Bounds newBounds = t.transform(oldBounds);
        System.out.println(t);
        System.out.println("t:"+(tx)+","+(ty));
       
        System.out.println("actual  :"+newBounds.getMinX()+","+newBounds.getMinX()+","+newBounds.getWidth()+","+
                newBounds.getHeight());
             */

        }
    }

    private static class NorthEastHandle extends AbstractResizeHandle {

        NorthEastHandle(Figure owner) {
            super(owner, RelativeLocator.northEast());
        }

        @Override
        public Cursor getCursor() {
            return Cursor.NE_RESIZE;
        }

        @Override
        protected void resize(Point2D newPoint, Figure owner, Bounds bounds, DrawingModel model, boolean keepAspect) {
            double newX = max(bounds.getMinX(), newPoint.getX());
            double newY = min(bounds.getMaxY(), newPoint.getY());
            double newWidth = newX - bounds.getMinX();
            double newHeight = bounds.getMaxY() - newY;
            if (keepAspect) {
                double newRatio = newHeight / newWidth;
                if (newRatio > preferredAspectRatio) {
                    newHeight = newWidth * preferredAspectRatio;
                } else {
                    newWidth = newHeight / preferredAspectRatio;
                }
            }

            transform(model, owner, bounds.getMinX(), bounds.getMaxY() - newHeight, newWidth, newHeight);
        }
    }

    private static class EastHandle extends AbstractResizeHandle {

        EastHandle(Figure owner) {
            super(owner, RelativeLocator.east());
        }

        @Override
        public Cursor getCursor() {
            return Cursor.E_RESIZE;
        }

        @Override
        protected void resize(Point2D newPoint, Figure owner, Bounds bounds, DrawingModel model, boolean keepAspect) {
            double newWidth = max(newPoint.getX(), bounds.getMinX()) - bounds.getMinX();
            double newHeight = bounds.getMaxY() - bounds.getMinY();
            if (keepAspect) {
                double newRatio = newHeight / newWidth;
                newHeight = newWidth * preferredAspectRatio;
            }
            transform(model, owner, bounds.getMinX(), (bounds.getMinY() + bounds.getMaxY() - newHeight) * 0.5, newWidth, newHeight);
        }

    }

    private static class NorthHandle extends AbstractResizeHandle {

        NorthHandle(Figure owner) {
            super(owner, RelativeLocator.north());
        }

        @Override
        public Cursor getCursor() {
            return Cursor.N_RESIZE;
        }

        @Override
        protected void resize(Point2D newPoint, Figure owner, Bounds bounds, DrawingModel model, boolean keepAspect) {
            double newY = min(bounds.getMaxY(), newPoint.getY());
            double newWidth = bounds.getMaxX() - bounds.getMinX();
            double newHeight = bounds.getMaxY() - newY;
            if (keepAspect) {
                double newRatio = newHeight / newWidth;
                newWidth = newHeight / preferredAspectRatio;
            }
            transform(model, owner, (bounds.getMinX() + bounds.getMaxX() - newWidth) * 0.5, newY, newWidth, newHeight);
        }
    }

    private static class NorthWestHandle extends AbstractResizeHandle {

        NorthWestHandle(Figure owner) {
            super(owner, RelativeLocator.northWest());
        }

        @Override
        public Cursor getCursor() {
            return Cursor.NW_RESIZE;
        }

        @Override
        protected void resize(Point2D newPoint, Figure owner, Bounds bounds, DrawingModel model, boolean keepAspect) {
            double newX = min(bounds.getMaxX(), newPoint.getX());
            double newY = min(bounds.getMaxY(), newPoint.getY());
            double newWidth = bounds.getMaxX() - newX;
            double newHeight = bounds.getMaxY() - newY;
            if (keepAspect) {
                double newRatio = newHeight / newWidth;
                if (newRatio > preferredAspectRatio) {
                    newHeight = newWidth * preferredAspectRatio;
                } else {
                    newWidth = newHeight / preferredAspectRatio;
                }
            }

            transform(model, owner, bounds.getMaxX() - newWidth, bounds.getMaxY() - newHeight, newWidth, newHeight);
        }
    }

    private static class SouthEastHandle extends AbstractResizeHandle {

        SouthEastHandle(Figure owner) {
            super(owner, RelativeLocator.southEast());
        }

        @Override
        public Cursor getCursor() {
            return Cursor.SE_RESIZE;
        }

        @Override
        protected void resize(Point2D newPoint, Figure owner, Bounds bounds, DrawingModel model, boolean keepAspect) {
            double newX = max(bounds.getMinX(), newPoint.getX());
            double newY = max(bounds.getMinY(), newPoint.getY());
            double newWidth = newX - bounds.getMinX();
            double newHeight = newY - bounds.getMinY();
            if (keepAspect) {
                double newRatio = newHeight / newWidth;
                if (newRatio > preferredAspectRatio) {
                    newHeight = newWidth * preferredAspectRatio;
                } else {
                    newWidth = newHeight / preferredAspectRatio;
                }
            }
            transform(model, owner, bounds.getMinX(), bounds.getMinY(), newWidth, newHeight);
        }
    }

    private static class SouthHandle extends AbstractResizeHandle {

        SouthHandle(Figure owner) {
            super(owner, RelativeLocator.south());
        }

        @Override
        public Cursor getCursor() {
            return Cursor.S_RESIZE;
        }

        @Override
        protected void resize(Point2D newPoint, Figure owner, Bounds bounds, DrawingModel model, boolean keepAspect) {
            double newY = max(bounds.getMinY(), newPoint.getY());
            double newWidth = bounds.getMaxX() - bounds.getMinX();
            double newHeight = newY - bounds.getMinY();
            if (keepAspect) {
                newWidth = newHeight / preferredAspectRatio;
            }
            transform(model, owner, (bounds.getMinX() + bounds.getMaxX() - newWidth) * 0.5, bounds.getMinY(), newWidth, newHeight);
        }
    }

    private static class SouthWestHandle extends AbstractResizeHandle {

        SouthWestHandle(Figure owner) {
            super(owner, RelativeLocator.southWest());
        }

        @Override
        public Cursor getCursor() {
            return Cursor.SW_RESIZE;
        }

        @Override
        protected void resize(Point2D newPoint, Figure owner, Bounds bounds, DrawingModel model, boolean keepAspect) {
            double newX = min(bounds.getMaxX(), newPoint.getX());
            double newY = max(bounds.getMinY(), newPoint.getY());
            double newWidth = bounds.getMaxX() - min(bounds.getMaxX(), newX);
            double newHeight = newY - bounds.getMinY();
            if (keepAspect) {
                double newRatio = newHeight / newWidth;
                if (newRatio > preferredAspectRatio) {
                    newHeight = newWidth * preferredAspectRatio;
                } else {
                    newWidth = newHeight / preferredAspectRatio;
                }
            }
            transform(model, owner, bounds.getMaxX() - newWidth, bounds.getMinY(), newWidth, newHeight);
        }
    }

    private static class WestHandle extends AbstractResizeHandle {

        WestHandle(Figure owner) {
            super(owner, RelativeLocator.west());
        }

        @Override
        public Cursor getCursor() {
            return Cursor.W_RESIZE;
        }

        @Override
        protected void resize(Point2D newPoint, Figure owner, Bounds bounds, DrawingModel model, boolean keepAspect) {
            double newX = min(bounds.getMaxX(), newPoint.getX());
            double newWidth = bounds.getMaxX() - newX;
            double newHeight = bounds.getHeight();
            if (keepAspect) {
                double newRatio = newHeight / newWidth;
                newHeight = newWidth * preferredAspectRatio;
            }
            transform(model, owner, newX, (bounds.getMinY() + bounds.getMaxY() - newHeight) * 0.5, newWidth, newHeight);
        }
    }
}
