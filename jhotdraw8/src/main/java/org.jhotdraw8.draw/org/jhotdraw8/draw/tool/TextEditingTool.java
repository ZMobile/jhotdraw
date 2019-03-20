/* @(#)CreationTool.java
 * Copyright © The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.tool;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import org.jhotdraw8.annotation.Nonnull;
import org.jhotdraw8.css.CssPoint2D;
import org.jhotdraw8.css.CssSize;
import org.jhotdraw8.draw.DrawingEditor;
import org.jhotdraw8.draw.DrawingView;
import org.jhotdraw8.draw.constrain.Constrainer;
import org.jhotdraw8.draw.figure.AnchorableFigure;
import org.jhotdraw8.draw.figure.Drawing;
import org.jhotdraw8.draw.figure.Figure;
import org.jhotdraw8.draw.figure.Layer;
import org.jhotdraw8.draw.figure.LayerFigure;
import org.jhotdraw8.draw.figure.TextEditableFigure;
import org.jhotdraw8.draw.handle.HandleType;
import org.jhotdraw8.draw.model.DrawingModel;
import org.jhotdraw8.geom.Geom;
import org.jhotdraw8.util.Resources;

import java.util.function.Supplier;

import static java.lang.Math.abs;
import static java.lang.Math.max;

/**
 * TextEditingTool.
 *
 * @author Werner Randelshofer
 * @version $Id$
 * @design.pattern CreationTool AbstractFactory, Client. Creation tools use
 * abstract factories (Supplier) for creating new {@link Figure}s.
 */
public class TextEditingTool extends AbstractCreationTool<Figure> {


    private double defaultWidth = 100;
    private double defaultHeight = 100;
    private TextArea textArea = new TextArea();
    private TextEditableFigure.TextEditorData editorData;
    /**
     * The rubber band.
     */
    private double x1, y1, x2, y2;

    /**
     * The minimum size of a created figure (in view coordinates.
     */
    private double minSize = 2;

    public TextEditingTool(String name, Resources rsrc, Supplier<TextEditableFigure> factory) {
        this(name, rsrc, factory, LayerFigure::new);
    }

    public TextEditingTool(String name, Resources rsrc, Supplier<TextEditableFigure> figureFactory, Supplier<Layer> layerFactory) {
        super(name, rsrc, figureFactory, layerFactory);
        node.setCursor(Cursor.CROSSHAIR);

        textArea.setWrapText(true);
        textArea.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                keyEvent.consume();
                stopEditing();
            }
        });
    }

    public double getDefaultHeight() {
        return defaultHeight;
    }

    public void setDefaultHeight(double defaultHeight) {
        this.defaultHeight = defaultHeight;
    }

    public double getDefaultWidth() {
        return defaultWidth;
    }

    public void setDefaultWidth(double defaultWidth) {
        this.defaultWidth = defaultWidth;
    }


    @Override
    protected void stopEditing() {
        if (editorData != null) {
            node.getChildren().remove(textArea);
            DrawingView drawingView = getDrawingView();
            if (drawingView != null) {
                DrawingModel model = drawingView.getModel();
                model.set(editorData.figure, editorData.textKey, textArea.getText());
            }
            editorData = null;
        }
        createdFigure = null;
    }

    @Override
    protected void handleMousePressed(@Nonnull MouseEvent event, @Nonnull DrawingView view) {
        if (editorData != null) {
            stopEditing();
            return;
        }

        x1 = event.getX();
        y1 = event.getY();

        Figure figure = view.findFigure(x1, y1);
        if (figure instanceof TextEditableFigure) {
            TextEditableFigure f = (TextEditableFigure) figure;
            TextEditableFigure.TextEditorData data = f.getTextEditorDataFor(f.worldToLocal(new Point2D(x1, y1)));
            if (data != null) {
                startEditing(data, view);
                return;
            }
        }


        x2 = x1;
        y2 = y1;
        createdFigure = createFigure();

        double anchorX = Geom.clamp(createdFigure.getNonnull(AnchorableFigure.ANCHOR_X), 0, 1);
        double anchorY = Geom.clamp(createdFigure.getNonnull(AnchorableFigure.ANCHOR_Y), 0, 1);


        CssPoint2D c = view.getConstrainer().constrainPoint(createdFigure, new CssPoint2D(view.viewToWorld(new Point2D(x1, y1))));
        createdFigure.reshapeInLocal(
                anchorX == 0 ? c.getX() : c.getX().subtract(new CssSize(defaultWidth).multiply(anchorX)),
                anchorY == 0 ? c.getY() : c.getY().subtract(new CssSize(defaultHeight).multiply(anchorY)),
                new CssSize(defaultWidth), new CssSize(defaultHeight));
        DrawingModel dm = view.getModel();
        Drawing drawing = dm.getDrawing();

        Layer layer = getOrCreateLayer(view, createdFigure);
        view.setActiveLayer(layer);

        dm.addChildTo(createdFigure, layer);
        event.consume();
    }


    private void startEditing(@Nonnull TextEditableFigure.TextEditorData data, @Nonnull DrawingView dv) {
        dv.getSelectedFigures().clear();
        dv.setHandleType(HandleType.SELECT);
        dv.getSelectedFigures().add(data.figure);
        editorData = data;
        textArea.setManaged(false);
        node.getChildren().add(textArea);
        Bounds bounds = dv.worldToView(data.figure.localToWorld(data.boundsInLocal));
        textArea.resizeRelocate(bounds.getMinX(), bounds.getMinY(), max(80, max(textArea.getMinWidth(), bounds.getWidth())),
                max(40, max(textArea.getMinHeight(), bounds.getHeight())));
        textArea.setText(data.figure.get(editorData.textKey));
        textArea.requestFocus();
    }

    @Override
    protected void handleMouseReleased(@Nonnull MouseEvent event, @Nonnull DrawingView dv) {
        if (editorData != null) {
            return;
        }

        if (createdFigure != null) {
            event.consume();
            if (abs(x2 - x1) < minSize && abs(y2 - y1) < minSize) {
                CssPoint2D c1 = dv.getConstrainer().constrainPoint(createdFigure, new CssPoint2D(dv.viewToWorld(x1, y1)));
                CssPoint2D c2 = dv.getConstrainer().translatePoint(createdFigure, new CssPoint2D(dv.viewToWorld(x1
                        + defaultWidth, y1 + defaultHeight)), Constrainer.DIRECTION_NEAREST);
                if (c2.equals(c1)) {
                    c2 = dv.getConstrainer().constrainPoint(createdFigure, new CssPoint2D(c1.getX().getConvertedValue() + defaultWidth, c1.getY().getConvertedValue() + defaultHeight));
                }
                DrawingModel dm = dv.getModel();
                dm.reshapeInLocal(createdFigure, c1.getX(), c1.getY(),
                        c2.getX().subtract(c1.getX()),
                        c2.getY().subtract(c1.getY()));
            }
            dv.selectedFiguresProperty().clear();
            dv.selectedFiguresProperty().add(createdFigure);
            TextEditableFigure.TextEditorData data = ((TextEditableFigure) createdFigure).getTextEditorDataFor(null);
            createdFigure = null;
            if (data != null) {
                startEditing(data, dv);
            } else {
                fireToolDone();
            }
            event.consume();
        }
    }

    @Override
    protected void handleMouseDragged(@Nonnull MouseEvent event, @Nonnull DrawingView dv) {
        if (editorData != null) {
            return;
        }
        if (createdFigure != null) {
            x2 = event.getX();
            y2 = event.getY();
            CssPoint2D c1 = dv.getConstrainer().constrainPoint(createdFigure, new CssPoint2D(dv.viewToWorld(x1, y1)));
            CssPoint2D c2 = dv.getConstrainer().constrainPoint(createdFigure, new CssPoint2D(dv.viewToWorld(x2, y2)));
            CssSize newWidth = c2.getX().subtract(c1.getX());
            CssSize newHeight = c2.getY().subtract(c1.getY());
            // shift keeps the aspect ratio
            boolean keepAspect = event.isShiftDown();
            if (keepAspect) {
                double preferredAspectRatio = createdFigure.getPreferredAspectRatio();
                double newRatio = newHeight.getConvertedValue() / newWidth.getConvertedValue();
                if (newRatio > preferredAspectRatio) {
                    newHeight = new CssSize(newWidth.getConvertedValue() * preferredAspectRatio);
                } else {
                    newWidth = new CssSize(newHeight.getConvertedValue() / preferredAspectRatio);
                }
            }

            DrawingModel dm = dv.getModel();
            dm.reshapeInLocal(createdFigure, c1.getX(), c1.getY(), newWidth, newHeight);
        }
        event.consume();
    }

    @Override
    protected void handleMouseMoved(MouseEvent event, DrawingView view) {
        if (editorData != null) {
            return;
        }
        Figure figure = view.findFigure(event.getX(), event.getY());
        if (figure instanceof TextEditableFigure) {
            TextEditableFigure f = (TextEditableFigure) figure;
            TextEditableFigure.TextEditorData data = f.getTextEditorDataFor(f.getWorldToLocal().transform(event.getX(), event.getY()));
            if (data != null) {
                node.setCursor(Cursor.TEXT);
                return;
            }
        }
        node.setCursor(Cursor.CROSSHAIR);
    }

    @Override
    protected void handleMouseClicked(MouseEvent event, DrawingView dv) {
    }


    /**
     * This implementation is empty.
     */
    @Override
    public void activate(DrawingEditor editor) {
        requestFocus();
        super.activate(editor);
    }

    @Override
    public String getHelpText() {
        return "CreationTool"
                + "\n  Click on the drawing view. The tool will create a new figure with default size at the clicked location."
                + "\nOr:"
                + "\n  Press and drag the mouse over the drawing view to define the diagonal of a rectangle. The tool will create a new figure that fits into the rectangle.";
    }

}