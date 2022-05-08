package org.exthmui.share.shared.preferences;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;

public class IntEditTextPreference extends EditTextPreference implements EditTextPreference.OnBindEditTextListener {
    @Nullable
    private String mText;

    public IntEditTextPreference(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setOnBindEditTextListener(this);
    }

    public IntEditTextPreference(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnBindEditTextListener(this);
    }

    public IntEditTextPreference(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnBindEditTextListener(this);
    }

    public IntEditTextPreference(@NonNull Context context) {
        super(context);
        setOnBindEditTextListener(this);
    }

    /**
     * Saves the text to the current data storage.
     *
     * @param text The text to save
     */
    @Override
    public void setText(@Nullable String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mText = text;

        if (text == null) return;

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
    @Override
    public String getText() {
        return mText;
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
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