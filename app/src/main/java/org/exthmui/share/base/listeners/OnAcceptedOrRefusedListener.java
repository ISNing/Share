package org.exthmui.share.base.listeners;

import java.util.EventListener;

public interface OnAcceptedOrRefusedListener extends EventListener {
    void onAcceptedOrRefused(boolean accepted);
}
