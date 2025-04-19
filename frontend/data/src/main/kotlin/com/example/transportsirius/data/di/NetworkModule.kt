package com.example.transportsirius.data.di

import com.example.transportsirius.data.api.GeocoderApi
import com.example.transportsirius.data.api.RouteApi
import com.example.transportsirius.data.mapper.GeocoderMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val BASE_URL = "https://api.transport-sirius.com/api/v1/"
    private const val DGIS_BASE_URL = "https://catalog.api.2gis.com/3.0/"
    
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("mainApi")
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    @Named("dgisApi")
    fun provideDgisRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DGIS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRouteApi(@Named("mainApi") retrofit: Retrofit): RouteApi {
        return retrofit.create(RouteApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideGeocoderApi(@Named("dgisApi") retrofit: Retrofit): GeocoderApi {
        return retrofit.create(GeocoderApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideGeocoderMapper(): GeocoderMapper {
        return GeocoderMapper()
    }
} 