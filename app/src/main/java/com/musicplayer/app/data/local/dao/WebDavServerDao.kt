package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.WebDavServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebDavServerDao {
    @Query("SELECT * FROM webdav_servers ORDER BY name ASC")
    fun observeServers(): Flow<List<WebDavServerEntity>>

    @Query("SELECT * FROM webdav_servers WHERE id = :serverId")
    suspend fun getServer(serverId: String): WebDavServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(server: WebDavServerEntity)

    @Query("DELETE FROM webdav_servers WHERE id = :serverId")
    suspend fun delete(serverId: String)
}
