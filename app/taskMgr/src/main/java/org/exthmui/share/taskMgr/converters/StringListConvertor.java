package org.exthmui.share.taskMgr.converters;

import android.util.Log;

import androidx.room.TypeConverter;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class StringListConvertor {
    @TypeConverter
    public List<String> stringToStringList(String string) {
        try {
            JSONArray jsonArray = new JSONArray(string);
            ArrayList<String> strings = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) strings.add(jsonArray.getString(i));
            return strings;
        } catch (JSONException e) {
            Log.e("StringListConvertor",
                    String.format("Failed converting string to string list.\nMessage: %s\nContent: %s", e.getMessage(), string));
            return null;
        }
    }

    @TypeConverter
    public String stringListToString(List<String> strings) {
        JSONArray jsonArray = new JSONArray(strings);
        return jsonArray.toString();
    }
}