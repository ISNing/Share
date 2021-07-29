package org.exthmui.share.shared.preferences;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;

public class IntEditTextPreference extends EditTextPreference implements EditTextPreference.OnBindEditTextListener {
    private String mText;

    public IntEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOnBindEditTextListener(this);
    }

    public IntEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnBindEditTextListener(this);
    }

    public IntEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnBindEditTextListener(this);
    }

    public IntEditTextPreference(Context context) {
        super(context);
        setOnBindEditTextListener(this);
    }

    /**
     * Saves the text to the current data storage.
     *
     * @param text The text to save
     */
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mText = text;

        int value = Integer.parseInt(text);

        persistInt(value);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }

        notifyChanged();
    }

    /**
     * Gets the text from the current data storage.
     *
     * @return The current preference value
     */
    public String getText() {
        return mText;
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        int value;
        if (defaultValue != null) {
            String strDefaultValue = (String) defaultValue;

            int defaultIntValue = Integer.parseInt(strDefaultValue);
            value = getPersistedInt(defaultIntValue);
        } else {
            value = getPersistedInt(0);
        }

        setText(Integer.toString(value));
    }

    @Override
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(mText) || super.shouldDisableDependents();
    }

    @Override
    public void onBindEditText(@NonNull EditText editText) {
        editText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
    }
}