package com.example.googlemaps.features.googlemaps.data

import com.example.googlemaps.features.googlemaps.domain.WaadsuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

class WaadsuRepositoryImpl @Inject constructor() : WaadsuRepository {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun getCoordinates() = withContext(Dispatchers.IO) {
        URL(BASE_URL).readText()
    }

    private companion object {
        private const val BASE_URL = "https://waadsu.com/api/russia.geo.json"
    }
}
