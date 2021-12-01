package com.example.googlemaps.features.googlemaps.domain

import javax.inject.Inject

class WaadsuInteractor @Inject constructor(
    private val waadsuRepo: WaadsuRepository
) {
    suspend fun getCoordinates() = waadsuRepo.getCoordinates()

}
