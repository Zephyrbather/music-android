package com.musicplayer.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.musicplayer.app.data.local.AppDatabase
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.CommentDao
import com.musicplayer.app.data.local.dao.PlayRecordDao
import com.musicplayer.app.data.local.dao.PlaylistDao
import com.musicplayer.app.data.local.dao.ScrapeCacheDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.dao.WebDavServerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "music-player.db")
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSongDao(database: AppDatabase): SongDao = database.songDao()

    @Provides
    fun provideAlbumDao(database: AppDatabase): AlbumDao = database.albumDao()

    @Provides
    fun provideArtistDao(database: AppDatabase): ArtistDao = database.artistDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideWebDavServerDao(database: AppDatabase): WebDavServerDao = database.webDavServerDao()

    @Provides
    fun provideScrapeCacheDao(database: AppDatabase): ScrapeCacheDao = database.scrapeCacheDao()

    @Provides
    fun provideCommentDao(database: AppDatabase): CommentDao = database.commentDao()

    @Provides
    fun providePlayRecordDao(database: AppDatabase): PlayRecordDao = database.playRecordDao()

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE albums ADD COLUMN releaseYear INTEGER")
            db.execSQL("ALTER TABLE albums ADD COLUMN releaseMonth INTEGER")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE songs ADD COLUMN categoryTag TEXT")
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_albumTitle_discNumber_trackNumber_title` ON `songs` (`albumTitle`, `discNumber`, `trackNumber`, `title`)")
        }
    }
}
