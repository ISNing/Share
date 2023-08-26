package org.exthmui.share.taskMgr.converters

import android.util.Log
import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONException

class StringListConvertor {
    @TypeConverter
    fun stringToStringList(string: String): MutableList<String> {
        return try {
            val jsonArray = JSONArray(string)
            val strings = ArrayList<String>()
            for (i in 0 until jsonArray.length()) strings.add(jsonArray.getString(i))
            strings
        } catch (e: JSONException) {
            Log.e(
                "StringListConvertor",
                String.format(
                    "Failed converting string to string list.\nMessage: %s\nContent: %s",
                    e.message,
                    string
                )
            )
            throw e
        }
    }

    @TypeConverter
    fun stringListToString(strings: MutableList<String>): String {
        val jsonArray = JSONArray(strings)
        return jsonArray.toString()
    }
}