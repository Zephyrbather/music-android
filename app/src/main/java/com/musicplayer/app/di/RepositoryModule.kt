package com.musicplayer.app.di

import com.musicplayer.app.data.repository.MusicRepositoryImpl
import com.musicplayer.app.data.repository.PlaylistRepositoryImpl
import com.musicplayer.app.data.repository.SettingsRepositoryImpl
import com.musicplayer.app.data.repository.WebDavRepositoryImpl
import com.musicplayer.app.data.ai.DeepSeekMetadataAdvisor
import com.musicplayer.app.data.scraper.CompositeMetadataScraper
import com.musicplayer.app.data.scraper.MetadataScraper
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.PlaylistRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import com.musicplayer.app.domain.repository.WebDavRepository
import com.musicplayer.app.domain.ai.AiMetadataAdvisor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindWebDavRepository(impl: WebDavRepositoryImpl): WebDavRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindMetadataScraper(impl: CompositeMetadataScraper): MetadataScraper

    @Binds
    @Singleton
    abstract fun bindAiMetadataAdvisor(impl: DeepSeekMetadataAdvisor): AiMetadataAdvisor
}
