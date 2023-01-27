package org.exthmui.share.taskMgr.converters;

import androidx.room.TypeConverter;

public class EnumConverter {
    @TypeConverter
    public String enumToString(Enum<?> anEnum) {
        return anEnum.getClass().getCanonicalName() + ":" + anEnum.name();
    }

    @TypeConverter
    public <T extends Enum<T>> T stringToEnum(String string) {
        String[] strArr = string.split(":");
        try {
            //noinspection unchecked
            return Enum.valueOf((Class<T>) Class.forName(strArr[0]), strArr[1]);
        } catch (ClassCastException | ClassNotFoundException e) {
            return null;
        }
    }
}
