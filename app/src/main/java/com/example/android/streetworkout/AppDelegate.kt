package com.example.android.streetworkout

import android.app.Application
import com.example.android.streetworkout.data.database.PlacesDatabase
import com.example.android.streetworkout.data.Repository

class AppDelegate : Application() {

    //private var mRepository: Repository? = null

    override fun onCreate() {
        super.onCreate()
//
//        val database: PlacesDatabase = PlacesDatabase.getInstance(this)
//
//        mRepository = Repository(database.placesDao())
    }

//    fun getRepository(): Repository? {
//        return mRepository
//    }
}