package org.exthmui.share.shared.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

public class ClickableStringPreference extends Preference {
    private String mValue;
    public ClickableStringPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ClickableStringPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ClickableStringPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ClickableStringPreference(@NonNull Context context) {
        super(context);
    }

    public void setValue(String val) {
        mValue = val;
        persistString(val);
    }

    public String getValue() {
        return getPersistedString(null);
    }
}
