/* @(#)LayerCell.java
 * Copyright (c) 2015 by the authors and contributors of JHotDraw.
 * You may only use this file in compliance with the accompanying license terms.
 */
package org.jhotdraw.draw.gui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import org.jhotdraw.draw.Drawing;

/**
 * FXML Controller class
 *
 * @author werni
 */
public class LayerCell extends AbstractDrawingInspector {

   public LayerCell() {
        this(LayerInspector.class.getResource("LayerCell.fxml"));
    }
    public LayerCell(URL fxmlUrl) {
        init(fxmlUrl);
    }

    private void init(URL fxmlUrl) {
        FXMLLoader loader = new FXMLLoader();
        loader.setController(this);

        try(InputStream in=fxmlUrl.openStream()) {
            setCenter(loader.load(in));
        } catch (IOException ex) {
            throw new InternalError(ex);
        }
    }
    
    protected void onDrawingChanged(Drawing oldValue, Drawing newValue) {
        
    }
}