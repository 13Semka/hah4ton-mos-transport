package com.example.transportsirius.data.di

import com.example.transportsirius.data.repository.GeocoderRepositoryImpl
import com.example.transportsirius.data.repository.RouteRepositoryImpl
import com.example.transportsirius.domain.repository.GeocoderRepository
import com.example.transportsirius.domain.repository.RouteRepository
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
    abstract fun bindRouteRepository(
        repositoryImpl: RouteRepositoryImpl
    ): RouteRepository
    
    @Binds
    @Singleton
    abstract fun bindGeocoderRepository(
        impl: GeocoderRepositoryImpl
    ): GeocoderRepository
} 