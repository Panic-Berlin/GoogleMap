package com.example.googlemaps.features.googlemaps.di

import com.example.googlemaps.features.googlemaps.data.WaadsuRepositoryImpl
import com.example.googlemaps.features.googlemaps.domain.WaadsuRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
interface WaadsuBindsModule {

    @Binds
    @ViewModelScoped
    fun bindsWaadsuRepository(impl: WaadsuRepositoryImpl): WaadsuRepository
}
