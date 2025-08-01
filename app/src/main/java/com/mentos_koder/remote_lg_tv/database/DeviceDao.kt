package com.mentos_koder.remote_lg_tv.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mentos_koder.remote_lg_tv.model.Device
import com.mentos_koder.remote_lg_tv.model.Favorite
@Dao
interface DeviceDao {
    @Query("SELECT COUNT(*) FROM devices WHERE address = :address")
    fun countDevicesWithAddress(address: String): Int

    @Query("SELECT * FROM devices WHERE address = :address LIMIT 1")
    fun getDeviceByAddress(address: String): Device?

    @get:Query("SELECT * FROM devices ORDER BY lastDateConnect Desc Limit 1")
    val lastDateConnect: Device?
    @Query("SELECT token FROM devices WHERE address = :address LIMIT 1")
    fun getTokenByAddress(address: String): String
    @Insert
    fun insert(device: Device)

    @Query("SELECT COUNT(*) FROM favorite WHERE id = :id")
    fun countFavouriteWithId(id: String?): Int

    @Query("SELECT * FROM favorite WHERE isFavourite = 1 AND ipAddress = :ipAddress")
    fun getFavouritesByIp(ipAddress: String?): MutableList<Favorite?>?

    @Insert
    fun insertFavourite(favorite: Favorite)


    @Query("UPDATE favorite SET isFavourite = :isFavourite WHERE id = :id")
    fun updateFavourite(isFavourite: Boolean, id: String?)

    @Query("DELETE from favorite WHERE id = :id")
    fun deleteFavourite(id: String?)

    @Update
    fun update(device: Device)

    @Delete
    fun delete(device: Device)
}