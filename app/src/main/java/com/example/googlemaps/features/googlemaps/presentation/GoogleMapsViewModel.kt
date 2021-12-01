package com.example.googlemaps.features.googlemaps.presentation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.googlemaps.features.googlemaps.domain.WaadsuInteractor
import com.example.googlemaps.utils.asLiveData
import com.google.maps.android.SphericalUtil
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonMultiPolygon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class GoogleMapsViewModel @Inject constructor(
    private val waadsuInteractor: WaadsuInteractor
) : ViewModel() {

    private val _coordinates = MutableLiveData<JSONObject>()
    val coordinates get() = _coordinates.asLiveData()

    private val _distance = MutableLiveData<Double>()
    val distance get() = _distance.asLiveData()

    private val _isLoading = MutableLiveData(true)
    val isLoading = _isLoading.asLiveData()

    init {
        loadCoordinates(_isLoading)
    }

    /**
     * Получение координат
     */
    private fun loadCoordinates(indicator: MutableLiveData<Boolean>) {
        indicator.value = true
        viewModelScope.launch {
            _coordinates.value = JSONObject(waadsuInteractor.getCoordinates())
            indicator.value = false
        }
    }

    /**
     * Вычисление дистанции
     */
    fun onMapReady(layer: GeoJsonLayer) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.Default) {
            _distance.postValue(layer.features.sumOf { feature ->
                (feature.geometry as GeoJsonMultiPolygon).polygons.sumOf { polygon ->
                    SphericalUtil.computeLength(polygon.outerBoundaryCoordinates)
                }
            } / 1000)
            _isLoading.postValue(false)
        }
    }
}
