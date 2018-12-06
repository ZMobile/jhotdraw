/* @(#)ActivityManagerEvent.java
 * Copyright © The authors and contributors of JHotDraw.
 * You may not use, copy or modify this file, except in compliance with the  
 * license agreement you entered into with the copyright holders. For details
 * see accompanying license terms.
 */
package org.jhotdraw.gui.event;

import java.util.EventObject;
import org.jhotdraw.gui.ActivityModel;

/**
 * An event generated by the {@link org.jhotdraw.gui.ActivityManager}.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-09-08 Created.
 */
public class ActivityManagerEvent extends EventObject {
    private static final long serialVersionUID = 1L;

    private ActivityModel activity;

    public ActivityManagerEvent(Object source, ActivityModel activity) {
        super(source);
        this.activity = activity;
    }

    public ActivityModel getActivityModel() {
        return activity;
    }
}
