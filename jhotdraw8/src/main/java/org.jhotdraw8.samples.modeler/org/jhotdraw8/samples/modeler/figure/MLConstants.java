/*
 * @(#)MLConstants.java
 * Copyright © 2020 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.samples.modeler.figure;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.draw.key.BooleanStyleableKey;
import org.jhotdraw8.draw.key.NullableBooleanStyleableKey;
import org.jhotdraw8.draw.key.NullableStringStyleableKey;
import org.jhotdraw8.draw.key.StringStyleableKey;
import org.jhotdraw8.samples.modeler.model.MLCompartmentalizedData;
import org.jhotdraw8.samples.modeler.model.MLCompartmentedDataStyleableFigureKey;

public class MLConstants {
    /**
     * "model" namespace is used for marking model elements.
     */
    @NonNull
    public static final String MODEL_NAMESPACE_PREFIX = "model";
    /**
     * "diagram" namespace is used for marking model elements.
     */
    @NonNull
    public static final String DIAGRAM_NAMESPACE_PREFIX = "diagram";
    /**
     * The name of a model element.
     */
    @NonNull
    public final static StringStyleableKey NAME = new StringStyleableKey(MODEL_NAMESPACE_PREFIX, "name", "unnamed", null);
    /**
     * The keyword of a model element.
     */
    @NonNull
    public final static NullableStringStyleableKey KEYWORD = new NullableStringStyleableKey(MODEL_NAMESPACE_PREFIX, "keyword", null);
    /**
     * The compartment data of a compartmentable model element.
     */
    @NonNull
    public final static MLCompartmentedDataStyleableFigureKey COMPARTMENTS = new MLCompartmentedDataStyleableFigureKey(MLConstants.MODEL_NAMESPACE_PREFIX, "compartments", new MLCompartmentalizedData());
    /**
     * The source owned property of an association model element.
     * <p>
     * This property indicates that the model element at the source of
     * the association has a property for to the target of the association.
     * <p>
     * Visually this is indicated by a small filled circle at the target end
     * of the association.
     * <p>
     * See OMG UML 2.5.1 formal-17-12-05, chapter 11.5.4 Associations.
     */
    @NonNull
    public final static NullableBooleanStyleableKey SOURCE_OWNED = new NullableBooleanStyleableKey(MODEL_NAMESPACE_PREFIX, "sourceOwned", null);
    /**
     * The target owned property of an association model element.
     * <p>
     * This property indicates that the model element at the target of
     * the association has a property for the source of the association.
     * <p>
     * Visually this is indicated by a small filled circle at the source end
     * of the association.
     * <p>
     * See OMG UML 2.5.1 formal-17-12-05, chapter 11.5.4 Associations.
     */
    @NonNull
    public final static NullableBooleanStyleableKey TARGET_OWNED = new NullableBooleanStyleableKey(MODEL_NAMESPACE_PREFIX, "targetOwned", null);

    /**
     * Whether the diagram element shows the keyword label.
     */
    @NonNull
    public final static BooleanStyleableKey KEYWORD_LABEL_VISIBLE = new BooleanStyleableKey(DIAGRAM_NAMESPACE_PREFIX, "keywordLabelVisible", false);

}