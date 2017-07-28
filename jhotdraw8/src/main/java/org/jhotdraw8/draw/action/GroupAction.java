/* @(#)GroupAction.java
 * Copyright (c) 2015 by the authors and contributors of JHotDraw.
 * You may only use this file in compliance with the accompanying license terms.
 */
package org.jhotdraw8.draw.action;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Supplier;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import org.jhotdraw8.app.Application;
import org.jhotdraw8.draw.figure.Drawing;
import org.jhotdraw8.draw.DrawingEditor;
import org.jhotdraw8.draw.DrawingView;
import org.jhotdraw8.draw.figure.Layer;
import org.jhotdraw8.draw.figure.Figure;
import org.jhotdraw8.draw.model.DrawingModel;
import org.jhotdraw8.util.Resources;
import org.jhotdraw8.tree.TreeNode;
import org.jhotdraw8.app.Project;

/**
 * GroupAction.
 *
 * @author Werner Randelshofer
 */
public class GroupAction extends AbstractSelectedAction {

    public static final String ID = "edit.group";
    public static final String COMBINE_PATHS_ID = "edit.combinePaths";
    public final Supplier<Figure> groupFactory;

    /**
     * Creates a new instance.
     *
     * @param app the application
     * @param editor the drawing editor
     * @param groupFactory the group factory
     */
    public GroupAction(Application app, DrawingEditor editor, Supplier<Figure> groupFactory) {
        this(ID, app, editor, groupFactory);
    }

    public GroupAction(String id, Application app, DrawingEditor editor, Supplier<Figure> groupFactory) {
        super(app, editor);
        Resources labels
                = Resources.getResources("org.jhotdraw8.draw.Labels");
        labels.configureAction(this, id);
        this.groupFactory = groupFactory;
        if (groupFactory == null) {
            addDisabler("groupFactory==null");
        }
    }

    @Override
    protected void handleActionPerformed(ActionEvent e, Project project) {
        final DrawingView view = getView();
        if (view == null) {
            return;
        }
        final LinkedList<Figure> figures = new LinkedList<>(view.getSelectedFigures());
        group(view, figures, groupFactory);

    }

    public static void group(DrawingView view, Collection<Figure> figures, Supplier<Figure> groupFactory) {
        // We don't add an empty group
        if (figures.isEmpty()) {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Empty selection can not be grouped");
            alert.getDialogPane().setMaxWidth(640.0);
            alert.showAndWait();
            return;
        }
        Figure first = figures.iterator().next();

        Drawing drawing = view.getDrawing();
        for (Figure child : figures) {
            if (child instanceof Layer) {
                // FIXME internationalize me
                final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Layers can not be grouped");
                alert.getDialogPane().setMaxWidth(640.0);
                alert.showAndWait();
                return;
            }
            if (child.getDrawing() != drawing) {
                // FIXME internationalize me
                final Alert alert = new Alert(Alert.AlertType.INFORMATION, " Only figures in primary drawing can be grouped");
                alert.getDialogPane().setMaxWidth(640.0);
                alert.showAndWait();
                return;
            }
            Figure parent = child.getParent();
            if (parent != null && (!parent.isEditable() || !parent.isDecomposable())) {
                // FIXME internationalize me
                final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Only figures in editable and decomposable parents can be grouped");
                alert.getDialogPane().setMaxWidth(640.0);
                alert.showAndWait();
                return;
            }
        }

        Figure parent = first.getParent();
        DrawingModel model = view.getModel();
        Figure group = groupFactory.get();
        model.addChildTo(group, parent);

        // Note: we iterate here over all figures because we must add
        //       the selected figures from back to front to the group
        for (Figure child : TreeNode.toList(drawing.breadthFirstIterable())) {
            if (!figures.contains(child)) {
                continue;
            }
            model.addChildTo(child, group);
        }

        view.getSelectedFigures().clear();
        view.getSelectedFigures().add(group);
    }
}
