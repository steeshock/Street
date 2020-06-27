package com.example.android.streetworkout.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places_table")
data class PlaceObject(

    @ColumnInfo(name = "imagePath")
    val imagePath: String = "",

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "description")
    val description: String = "",

    //ToDo later change to Google position
    @ColumnInfo(name = "position")
    val position: String = "",

    @ColumnInfo(name = "address")
    val address: String = "",

    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false

) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var Id: Int = (0..100).random()
}
