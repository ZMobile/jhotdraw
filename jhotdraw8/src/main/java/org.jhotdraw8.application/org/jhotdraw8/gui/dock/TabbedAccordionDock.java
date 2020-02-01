/*
 * @(#)TabbedAccordionDock.java
 * Copyright © The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.gui.dock;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Control;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.gui.CustomSkin;

import static java.lang.Double.max;

/**
 * TabbedAccordionDock.
 *
 * @author Werner Randelshofer
 */
public class TabbedAccordionDock extends Control implements Dock {

    @NonNull
    private ObjectProperty<Track> track = new SimpleObjectProperty<>();

    @NonNull
    private TabPane tabPane = new TabPane();
    @NonNull
    private Accordion accordion = new Accordion();
    @NonNull
    private TitledPane titlePane = new TitledPane();
    @NonNull
    private ResizePane resizePane = new ResizePane();
    @NonNull
    private ObservableList<DockItem> items = FXCollections.observableArrayList();

    public TabbedAccordionDock() {

        setSkin(new CustomSkin<>(this));
        getStyleClass().add("dock");
        getChildren().add(accordion);
        accordion.getPanes().add(titlePane);
        accordion.setExpandedPane(titlePane);

        SplitPane.setResizableWithParent(this, Boolean.FALSE);
        VBox.setVgrow(this, Priority.SOMETIMES);

        accordion.setStyle("-fx-background-color:transparent;-fx-border:none;-fx-padding:0;");
        titlePane.setStyle("-fx-background-color:transparent;-fx-border:none;-fx-padding:0;");
        accordion.setMinWidth(100.0);

        getItems().addListener((ListChangeListener<DockItem>) c -> {
            while (c.next()) {
                for (DockItem remitem : c.getRemoved()) {
                    remitem.setDock(null);
                }
                for (DockItem additem : c.getAddedSubList()) {
                    additem.setDock(TabbedAccordionDock.this);
                }
            }
            updateView();
        });

        trackProperty().addListener((o, oldv, newv) -> {
            resizePane.setUserResizable(newv != null && !newv.resizesItems());
        });
    }

    @Override
    protected double computePrefHeight(double width) {
        return accordion.prefHeight(width);
    }

    @Override
    protected double computePrefWidth(double height) {
        return accordion.prefWidth(height);
    }

    @NonNull
    @Override
    public ObservableList<DockItem> getItems() {
        return items;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @NonNull
    @Override
    public ObjectProperty<Track> trackProperty() {
        return track;
    }

    private void updateView() {
        switch (items.size()) {
            case 0: {
                resizePane.setCenter(null);
                tabPane.getTabs().clear();
                titlePane.setText(null);
                titlePane.setGraphic(null);
                titlePane.setContent(null);
                break;
            }
            case 1: {
                tabPane.getTabs().clear();
                DockItem i = items.get(0);
                titlePane.setText(i.getText());
                titlePane.setGraphic(i.getGraphic());
                titlePane.setContent(resizePane);
                resizePane.setCenter(i.getContent());
                break;
            }
            default: {
                resizePane.setCenter(tabPane);
                titlePane.setGraphic(null);
                tabPane.getTabs().setAll(items);
                StringBuilder b = new StringBuilder();
                double minHeight = 0;
                for (DockItem i : items) {
                    Node content = i.getContent();
                    if (content instanceof Region) {
                        minHeight = max(minHeight, content.minHeight(-1));
                    }
                    if (b.length() > 0) {
                        b.append(", ");
                    }
                    b.append(i.getText());
                }
                tabPane.setMinHeight(minHeight + 44);
                titlePane.setText(b.toString());
                titlePane.setContent(resizePane);
                break;
            }
        }
    }
}
