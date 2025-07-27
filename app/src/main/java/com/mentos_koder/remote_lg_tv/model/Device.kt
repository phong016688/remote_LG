package com.mentos_koder.remote_lg_tv.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
class Device {
    @PrimaryKey
    @ColumnInfo(name = "address")
    var address: String = ""

    @ColumnInfo(name = "model")
    var model: String? = null

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "firstDateConnect")
    var firstDateConnect: String? = null

    @ColumnInfo(name = "lastDateConnect")
    var lastDateConnect: String? = null

    @ColumnInfo(name = "typeConnect")
    var typeConnect: String? = null

    @ColumnInfo(name = "typeDevice")
    var typeDevice: String? = null

    @ColumnInfo(name = "key")
    var key: String? = null

    @ColumnInfo(name = "token")
    var token: String? = null

    @ColumnInfo(name = "logo")
    var logo: String? = null

    @ColumnInfo(name = "keyCartToken")
    var keyCartToken: String? = null
}
