package com.example.pruebav.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    @TypeConverter
    fun fromParametroList(value: List<Parametro>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toParametroList(value: String): List<Parametro> {
        val listType = object : TypeToken<List<Parametro>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
