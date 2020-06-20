/*
 * @(#)GrapherApplication.java
 * Copyright © 2020 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.samples.grapher;

import javafx.collections.ObservableMap;
import javafx.stage.Screen;
import org.jhotdraw8.app.SimpleFileBasedApplication;
import org.jhotdraw8.app.action.Action;
import org.jhotdraw8.app.action.file.ExportFileAction;
import org.jhotdraw8.app.action.file.PrintFileAction;
import org.jhotdraw8.app.action.file.RevertFileAction;
import org.jhotdraw8.draw.gui.DrawingExportOptionsPane;
import org.jhotdraw8.draw.io.BitmapExportOutputFormat;
import org.jhotdraw8.draw.io.XMLEncoderOutputFormat;
import org.jhotdraw8.gui.FileURIChooser;
import org.jhotdraw8.gui.URIExtensionFilter;
import org.jhotdraw8.macos.MacOSPreferences;
import org.jhotdraw8.samples.grapher.action.GrapherAboutAction;
import org.jhotdraw8.svg.io.SvgFullSceneGraphExporter;
import org.jhotdraw8.svg.io.SvgTinySceneGraphExporter;

import java.util.ArrayList;
import java.util.List;

import static org.jhotdraw8.app.action.file.ExportFileAction.EXPORT_CHOOSER_FACTORY_KEY;
import static org.jhotdraw8.io.DataFormats.registerDataFormat;

/**
 * GrapherApplication.
 *
 * @author Werner Randelshofer
 */
public class GrapherApplication extends SimpleFileBasedApplication {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (Screen.getPrimary().getOutputScaleX() >= 2.0) {
            // The following settings improve font rendering quality on
            // retina displays (no color fringes around characters).
            System.setProperty("prism.subpixeltext", "on");
            System.setProperty("prism.lcdtext", "false");
        } else {
            // The following settings improve font rendering on
            // low-res lcd displays (less color fringes around characters).
            System.setProperty("prism.text", "t2k");
            System.setProperty("prism.lcdtext", "true");
        }

        launch(args);
    }

    @Override
    protected void initActions() {
        super.initActions();
        ObservableMap<String, Action> map = getActions();
        map.put(RevertFileAction.ID, new RevertFileAction(this, null));
        map.put(PrintFileAction.ID, new PrintFileAction(this, null));
        map.put(ExportFileAction.ID, new ExportFileAction(this, DrawingExportOptionsPane::createDialog));
        map.put(GrapherAboutAction.ID, new GrapherAboutAction(this));
    }

    @Override
    protected void initFactories() {
        setActivityFactory(GrapherActivity::new);
        setMenuBarFactory(createFxmlNodeSupplier(getClass().getResource("GrapherMenuBar.fxml"), getResourceBundle()));
    }

    @Override
    protected void initProperties() {
        super.initProperties();
        put(NAME_KEY, "Grapher");
        put(COPYRIGHT_KEY, "Copyright © 2020 The authors and contributors of JHotDraw.");
        put(LICENSE_KEY, "MIT License.");

        List<URIExtensionFilter> exportExtensions = new ArrayList<>();
        exportExtensions.add(new URIExtensionFilter("SVG Full", registerDataFormat(SvgFullSceneGraphExporter.SVG_MIME_TYPE_WITH_VERSION), "*.svg"));
        exportExtensions.add(new URIExtensionFilter("SVG Tiny", registerDataFormat(SvgTinySceneGraphExporter.SVG_MIME_TYPE_WITH_VERSION), "*.svg"));
        exportExtensions.add(new URIExtensionFilter("PNG", registerDataFormat(BitmapExportOutputFormat.PNG_MIME_TYPE), "*.png"));
        exportExtensions.add(new URIExtensionFilter("XMLSerialized", registerDataFormat(XMLEncoderOutputFormat.XML_SERIALIZER_MIME_TYPE), "*.ser.xml"));
        put(EXPORT_CHOOSER_FACTORY_KEY, () -> new FileURIChooser(FileURIChooser.Mode.OPEN, exportExtensions));
    }

    @Override
    protected void initResourceBundle() {
        setResourceBundle(GrapherLabels.getResources().asResourceBundle());
    }

    @Override
    protected void startUserAgentStylesheet() {
        final Object value = MacOSPreferences.get(MacOSPreferences.GLOBAL_PREFERENCES, "AppleInterfaceStyle");
        if ("Dark".equals(value)) {
            getStylesheets().add(getClass().getResource("dark-theme.css").toString());
        } else {
            getStylesheets().add(getClass().getResource("light-theme.css").toString());
        }
    }
}
