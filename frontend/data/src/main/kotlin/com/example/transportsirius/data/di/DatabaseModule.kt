package com.example.transportsirius.data.di

import android.content.Context
import androidx.room.Room
import com.example.transportsirius.data.db.AppDatabase
import com.example.transportsirius.data.db.dao.RouteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "transport_sirius.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideRouteDao(database: AppDatabase): RouteDao {
        return database.routeDao()
    }
} 