/* @(#)SetBoundsEdit.java
 * Copyright © The authors and contributors of JHotDraw. MIT License.
 */


package org.jhotdraw.draw.event;

import org.jhotdraw.draw.*;
import javax.swing.undo.*;
import java.awt.geom.*;
/**
 * SetBoundsEdit.
 *
 * @author Werner Randelshofer
 * @version $Id$
 */
public class SetBoundsEdit extends AbstractUndoableEdit {
    private static final long serialVersionUID = 1L;
    private AbstractFigure owner;
    private Point2D.Double oldAnchor, oldLead;
    private Point2D.Double newAnchor, newLead;
    
    /** Creates a new instance. */
    public SetBoundsEdit(AbstractFigure owner, Point2D.Double oldAnchor, Point2D.Double oldLead, Point2D.Double newAnchor, Point2D.Double newLead) {
        this.owner = owner;
        this.oldAnchor = oldAnchor;
        this.oldLead = oldLead;
        this.newAnchor = newAnchor;
        this.newLead = newLead;
    }
    @Override
    public String getPresentationName() {
        // XXX - Localize me
        return "Abmessungen \u00e4ndern";
    }
    
    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        if (anEdit instanceof SetBoundsEdit) {
            SetBoundsEdit that = (SetBoundsEdit) anEdit;
            if (that.owner == this.owner) {
                this.newAnchor = that.newAnchor;
                this.newLead = that.newLead;
                that.die();
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean replaceEdit(UndoableEdit anEdit) {
        if (anEdit instanceof SetBoundsEdit) {
            SetBoundsEdit that = (SetBoundsEdit) anEdit;
            if (that.owner == this.owner) {
                that.oldAnchor = this.oldAnchor;
                that.oldLead = this.oldLead;
                this.die();
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        owner.willChange();
        owner.setBounds(newAnchor, newLead);
        owner.changed();
    }
    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        owner.willChange();
        owner.setBounds(oldAnchor, oldLead);
        owner.changed();
    }
}

