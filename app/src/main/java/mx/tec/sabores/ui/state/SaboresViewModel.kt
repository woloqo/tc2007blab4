package mx.tec.sabores.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mx.tec.sabores.data.RestaurantRepository
import mx.tec.sabores.domain.RatingSummary
import mx.tec.sabores.domain.Restaurant
import mx.tec.sabores.domain.RestaurantEnLista
import mx.tec.sabores.domain.Review
import okio.IOException
import retrofit2.HttpException

data class MyReviewItem(val restaurantName: String, val review: Review)

/** El restaurante y sus reseñas, que la pantalla de detalle necesita juntos. */
data class Detalle(
    val restaurant: Restaurant,
    val reviews: List<Review>
) {
    // La regla del dominio sigue viva: el promedio se calcula aquí, no se hereda
    // del servidor, para que cambie al instante al publicar tu reseña.
    val summary: RatingSummary = RatingSummary.from(reviews)
}


class SaboresViewModel(
    private val repository: RestaurantRepository = RestaurantRepository()
) : ViewModel() {

    private var cachedRestaurantes: List<RestaurantEnLista>? = null

    var restaurantes by mutableStateOf<UiState<List<RestaurantEnLista>>>(UiState.Cargando)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var busqueda by mutableStateOf("")
        private set

    var detalle by mutableStateOf<UiState<Detalle>>(UiState.Cargando)
        private set

    var mias by mutableStateOf<List<MyReviewItem>>(emptyList())
        private set

    var snackbarMessage by mutableStateOf<String?>(null)
        private set

    init {
        cargarRestaurantes()
        cargarMisResenas()
    }

    fun onBusquedaChange(nuevaBusqueda: String) {
        busqueda = nuevaBusqueda
    }

    fun dismissSnackbar() {
        snackbarMessage = null
    }

    fun cargarRestaurantes(esRefresh: Boolean = false) {
        viewModelScope.launch {
            if (cachedRestaurantes == null) {
                restaurantes = UiState.Cargando
            }
            if (esRefresh) {
                isRefreshing = true
            }
            try {
                val lista = repository.getAllForList()
                cachedRestaurantes = lista
                restaurantes = UiState.Exito(lista)
            } catch (e: IOException) {
                if (cachedRestaurantes == null) {
                    restaurantes = UiState.Error("No hay conexión. Revisa tu internet.")
                }
            } catch (e: HttpException) {
                if (cachedRestaurantes == null) {
                    restaurantes = UiState.Error("El servidor respondió ${e.code()}.")
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    fun cargarMisResenas() {
        viewModelScope.launch {
            try {
                val reviews = repository.getMyReviews()
                val list = cachedRestaurantes ?: emptyList()
                mias = reviews.map { review ->
                    val nombre = list.find { it.restaurant.id == review.restaurantId }?.restaurant?.name
                        ?: "Restaurante #${review.restaurantId}"
                    MyReviewItem(nombre, review)
                }
            } catch (e: Exception) {
                // Si falla, se queda con la lista local actual
            }
        }
    }

    fun cargarDetalle(id: Int) {
        viewModelScope.launch {
            detalle = UiState.Cargando
            detalle = pedir { Detalle(repository.getById(id), repository.getReviews(id)) }
        }
    }

    fun borrarResena(reviewId: Int) {
        val prevMias = mias
        val prevDetalle = detalle

        // Actualización optimista inmediata
        mias = mias.filter { it.review.id != reviewId }
        if (detalle is UiState.Exito) {
            val d = (detalle as UiState.Exito).datos
            val nuevasReviews = d.reviews.filter { it.id != reviewId }
            detalle = UiState.Exito(d.copy(reviews = nuevasReviews))
        }

        viewModelScope.launch {
            try {
                val exito = repository.deleteReview(reviewId)
                if (!exito) {
                    // Revertir si el servidor responde que no se pudo
                    mias = prevMias
                    detalle = prevDetalle
                    snackbarMessage = "No se pudo borrar la reseña. No tienes permiso."
                }
            } catch (e: Exception) {
                // Revertir en caso de falla de red o error HTTP
                mias = prevMias
                detalle = prevDetalle
                snackbarMessage = "Error al borrar la reseña. Se ha restaurado."
            }
        }
    }

    private suspend fun <T> pedir(block: suspend () -> T): UiState<T> = try {
        UiState.Exito(block())
    } catch (e: IOException) {
        UiState.Error("No hay conexión. Revisa tu internet.")
    } catch (e: HttpException) {
        UiState.Error(mensajeDe(e))
    }
}