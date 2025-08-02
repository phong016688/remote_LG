package com.mentos_koder.remote_lg_tv.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "favorite")
class Favorite {
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: String = ""

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "iconLink")
    var iconLink: String? = null

    @ColumnInfo(name = "iconLocal")
    var iconLocal: String? = null

    @ColumnInfo(name = "ipAddress")
    var ipAddress: String? = null

    @ColumnInfo(name = "type")
    var type: String? = null

    @ColumnInfo(name = "namespace")
    var namespace: String? = null

    @ColumnInfo(name = "message")
    var message: String? = null

    @ColumnInfo(name = "typeTVConnect")
    var typeTVConnect: String? = null

    @ColumnInfo(name = "recentDate")
    var recentDate: String? = null

    @ColumnInfo(name = "isFavourite")
    var favourite = false
}
