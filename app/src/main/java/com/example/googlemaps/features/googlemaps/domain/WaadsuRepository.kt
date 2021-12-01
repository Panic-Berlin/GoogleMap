package com.example.googlemaps.features.googlemaps.domain

import java.io.IOException

interface WaadsuRepository {

    /**
     * @Throws
     * В реализациях может вернуть IOException
     */

    @Throws(IOException::class)
    suspend fun getCoordinates() : String
}
