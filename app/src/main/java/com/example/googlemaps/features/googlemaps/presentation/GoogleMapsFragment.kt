package com.example.googlemaps.features.googlemaps.presentation


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.example.googlemaps.R
import com.example.googlemaps.databinding.FragmentGoogleMapsBinding
import com.example.googlemaps.utils.await
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.collections.GroundOverlayManager
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.collections.PolygonManager
import com.google.maps.android.collections.PolylineManager
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonMultiPolygon
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoogleMapsFragment : Fragment(R.layout.fragment_google_maps), OnMapReadyCallback {

    private val viewBinding: FragmentGoogleMapsBinding by viewBinding(FragmentGoogleMapsBinding::bind)
    private val viewModel: GoogleMapsViewModel by viewModels()
    private lateinit var mapView: MapView
    private lateinit var placesClient: PlacesClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the SDK
        Places.initialize(
            requireContext().applicationContext,
            getString(R.string.google_maps_key)
        )

        // Create a new PlacesClient instance
        placesClient = Places.createClient(requireContext())

        mapView = viewBinding.mpMap
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        observe()
    }

    /**
     * Получение GeoJson и расчет расстояния
     */
    override fun onMapReady(googleMap: GoogleMap) {
        val markerManager = MarkerManager(googleMap)
        val polygonManager = MyPolygonManager(googleMap)
        val polylineManager = PolylineManager(googleMap)
        val groundOverlayManager = GroundOverlayManager(googleMap)
        val collection = groundOverlayManager.newCollection("regions")
        val decoder = Geocoder(requireContext())
        googleMap.setOnMapClickListener { latLng ->
            val addresses = decoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses.isEmpty()) {
                Toast.makeText(requireContext(), "Ничего не найдено", Toast.LENGTH_SHORT).show()
            } else {
                selectAdminArea(addresses.first(), collection)
            }
        }

        viewModel.coordinates.observe(viewLifecycleOwner, { json ->
            val layer = GeoJsonLayer(
                googleMap,
                json,
                markerManager,
                polygonManager,
                polylineManager,
                groundOverlayManager
            )
            (layer.features.first().geometry as GeoJsonMultiPolygon).let {
                for (polygon in it.polygons) {
                    val outer = polygon.outerBoundaryCoordinates
                    polygon.coordinates.clear()
                    (polygon.coordinates as ArrayList<ArrayList<LatLng>>).add(outer)
                }
            }

            layer.defaultPolygonStyle.strokeColor = Color.BLUE
            layer.addLayerToMap()
            animateCameraToRandomFeaturePosition(googleMap, layer)
            viewModel.onMapReady(layer)
        })
    }

    /**
     * Выделения субъекта нажатой зоны
     */
    private fun selectAdminArea(address: Address, collection: GroundOverlayManager.Collection) {
        lifecycleScope.launch {
            val response = placesClient.findAutocompletePredictions(
                FindAutocompletePredictionsRequest.builder()
                    .setQuery(address.adminArea)
                    .build()
            ).await()
            if (response.autocompletePredictions.isEmpty()) return@launch
            val placeResponse = placesClient.fetchPlace(
                FetchPlaceRequest.builder(
                    response.autocompletePredictions.first().placeId,
                    listOf(Place.Field.VIEWPORT)
                ).build()
            ).await()
            placeResponse.place.viewport?.let { bounds ->
                collection.clear()
                collection.addGroundOverlay(
                    GroundOverlayOptions()
                        .positionFromBounds(bounds)
                        .image(BitmapDescriptorFactory.fromBitmap(createFrameBitmap()))
                )
            }
        }
    }

    /**
     * Рисование квадрата для выделения субъекта
     */
    private fun createFrameBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(1024, 720, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.RED
            flags = Paint.ANTI_ALIAS_FLAG
            style = Paint.Style.STROKE
            strokeWidth = 32f
        }
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
        return bitmap
    }

    /**
     * Перемещение камеры в первую по списку точку
     */
    private fun animateCameraToRandomFeaturePosition(googleMap: GoogleMap, layer: GeoJsonLayer) {
        (layer.features.first().geometry as? GeoJsonMultiPolygon)?.polygons
            ?.firstOrNull()
            ?.outerBoundaryCoordinates
            ?.firstOrNull()
            ?.let {
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        it,
                        googleMap.cameraPosition.zoom
                    )
                )
            }
    }

    /**
     * Наблюдение за изменениями
     */
    private fun observe() {
        viewModel.distance.observe(viewLifecycleOwner, {
            viewBinding.tvDistance.text = getString(R.string.distance, it)
        })
        viewModel.isLoading.observe(viewLifecycleOwner) {
            viewBinding.viewProgress.civLoading.isVisible = it
        }
    }

    override fun onResume() {
        mapView.onResume()
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}

// Переопределяем стандартный PolygonManager
class MyPolygonManager(map: GoogleMap?) : PolygonManager(map) {
    override fun newCollection(): PolygonManager.Collection {
        return object : Collection() {
            // Переопределяем метод добавления нового полигона,
            // выключаем кликабельность, что бы оно не захватывало клики,
            // мешая вызову OnMapClickListener
            override fun addPolygon(opts: PolygonOptions?): Polygon {
                return super.addPolygon(opts?.clickable(false))
            }
        }
    }
}
