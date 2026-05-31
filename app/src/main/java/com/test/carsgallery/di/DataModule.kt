package com.test.carsgallery.di

import com.test.carsgallery.data.datasource.RemoteDataSource
import com.test.carsgallery.data.remote.RemoteDataSourceImpl
import com.test.carsgallery.data.repository.GalleryRepositoryImpl
import com.test.carsgallery.domain.repository.GalleryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindRemoteDataSource(impl: RemoteDataSourceImpl): RemoteDataSource

    @Binds
    @Singleton
    abstract fun bindGalleryRepository(impl: GalleryRepositoryImpl): GalleryRepository
}
