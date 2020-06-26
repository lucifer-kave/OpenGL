package com.example.opengl

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper : SQLiteOpenHelper {

    constructor(
        context: Context?,
        name: String?,
        factory: SQLiteDatabase.CursorFactory?,
        version: Int
    ) : super(context, name, factory, version)

    override fun onCreate(db: SQLiteDatabase?) {
        TODO("Not yet implemented")
        val sql:String = "create table student_table(_id integer primary key autoincrement, name text, age integer);"
        db?.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        super.onDowngrade(db, oldVersion, newVersion)
    }
}