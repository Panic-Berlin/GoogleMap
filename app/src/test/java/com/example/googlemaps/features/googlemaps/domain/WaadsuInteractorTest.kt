package com.example.googlemaps.features.googlemaps.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalCoroutinesApi
class WaadsuInteractorTest {

    private val waadsuRepository: WaadsuRepository = mock()
    private val interactor: WaadsuInteractor = WaadsuInteractor(waadsuRepository)

    @Test
    fun getCoordinates() = runBlockingTest {
        val json = "{}"
        whenever(waadsuRepository.getCoordinates()).thenReturn(json)

        val response = interactor.getCoordinates()

        verify(waadsuRepository).getCoordinates()
        Assert.assertEquals(json, response)
    }

    @Test
    fun getCoordinatesNegative() = runBlockingTest {
        whenever(waadsuRepository.getCoordinates())
            .thenThrow(IOException("Test exception"))

        Assert.assertThrows(IOException::class.java) {
            runBlocking {
                interactor.getCoordinates()
            }
        }

        verify(waadsuRepository).getCoordinates()
    }
}
