/* @(#)TextAreaViewController.java
 * Copyright (c) 2015 by the authors and contributors of JHotDraw.
 * You may only use this file in compliance with the accompanying license terms.
 */
package org.jhotdraw8.samples.teddy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.input.DataFormat;
import org.jhotdraw8.app.AbstractDocumentProject;
import org.jhotdraw8.app.action.Action;
import org.jhotdraw8.collection.HierarchicalMap;
import org.jhotdraw8.concurrent.FXWorker;
import org.jhotdraw8.app.DocumentProject;

/**
 * TeddyProject.
 *
 * @author Werner Randelshofer
 * @version $Id$
 */
public class TeddyProject extends AbstractDocumentProject implements DocumentProject, Initializable {

  @FXML
  private URL location;

  private Node node;
  @FXML
  private ResourceBundle resources;
  @FXML
  private TextArea textArea;

  @Override
  public CompletionStage<Void> clear() {
    textArea.setText(null);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void clearModified() {
    modified.set(false);
  }

  @Override
  public Node getNode() {
    return node;
  }

  @Override
  protected void initActionMap(HierarchicalMap<String, Action> map) {
    // empty
  }

  @Override
  public void initView() {
    FXMLLoader loader = new FXMLLoader();
    loader.setController(this);

    try {
      node = loader.load(getClass().getResourceAsStream("TeddyProject.fxml"));
    } catch (IOException ex) {
      throw new InternalError(ex);
    }
  }

  /**
   * Initializes the controller class.
   */
  @FXML
  @Override
  public void initialize(URL location, ResourceBundle resources) {

    textArea.textProperty().addListener((observable -> modified.set(true)));
  }

  @Override
  public CompletionStage<Void> print(PrinterJob job) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public CompletionStage<Void> read(URI uri, DataFormat format, boolean append) {
    return FXWorker.supply(() -> {
      StringBuilder builder = new StringBuilder();
      char[] cbuf = new char[8192];
      try (Reader in = new InputStreamReader(new FileInputStream(new File(uri)), StandardCharsets.UTF_8)) {
        for (int count = in.read(cbuf, 0, cbuf.length); count != -1; count = in.read(cbuf, 0, cbuf.length)) {
          builder.append(cbuf, 0, count);
        }
      }
      return builder.toString();
    }).thenAccept(value -> {
      if (append) {
        textArea.appendText(value);
      } else {
        textArea.setText(value);
      }
    });
  }

  @Override
  public CompletionStage<Void> write(URI uri, DataFormat format) {
    final String text = textArea.getText();
    return FXWorker.run(() -> {
      try (Writer out = new OutputStreamWriter(new FileOutputStream(new File(uri)), StandardCharsets.UTF_8)) {
        out.write(text);
      }
    });
  }

}