package com.musicplayer.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.musicplayer.app.data.local.converter.Converters
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.CommentDao
import com.musicplayer.app.data.local.dao.PlayRecordDao
import com.musicplayer.app.data.local.dao.PlaylistDao
import com.musicplayer.app.data.local.dao.ScrapeCacheDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.dao.WebDavServerDao
import com.musicplayer.app.data.local.entity.AlbumEntity
import com.musicplayer.app.data.local.entity.ArtistEntity
import com.musicplayer.app.data.local.entity.CommentEntity
import com.musicplayer.app.data.local.entity.PlayRecordEntity
import com.musicplayer.app.data.local.entity.PlaylistEntity
import com.musicplayer.app.data.local.entity.PlaylistSongEntity
import com.musicplayer.app.data.local.entity.ScrapeCacheEntity
import com.musicplayer.app.data.local.entity.SongEntity
import com.musicplayer.app.data.local.entity.WebDavServerEntity

@Database(
    entities = [
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        WebDavServerEntity::class,
        ScrapeCacheEntity::class,
        CommentEntity::class,
        PlayRecordEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun webDavServerDao(): WebDavServerDao
    abstract fun scrapeCacheDao(): ScrapeCacheDao
    abstract fun commentDao(): CommentDao
    abstract fun playRecordDao(): PlayRecordDao
}
