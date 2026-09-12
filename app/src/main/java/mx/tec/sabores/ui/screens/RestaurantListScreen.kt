package mx.tec.sabores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.tec.sabores.domain.RestaurantEnLista
import mx.tec.sabores.ui.components.RestaurantCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantListScreen(
    restaurants: List<RestaurantEnLista>,
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRestaurantClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val restaurantesFiltrados = remember(restaurants, busqueda) {
        if (busqueda.isBlank()) {
            restaurants
        } else {
            restaurants.filter { item ->
                item.restaurant.name.contains(busqueda, ignoreCase = true) ||
                        item.restaurant.cuisine.contains(busqueda, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = busqueda,
            onValueChange = onBusquedaChange,
            placeholder = { Text("Buscar por nombre o cocina...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (busqueda.isNotEmpty()) {
                    IconButton(onClick = { onBusquedaChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(restaurantesFiltrados, key = { it.restaurant.id }) { item ->
                    RestaurantCard(
                        restaurant = item.restaurant,
                        summary = item.summary,
                        onClick = { onRestaurantClick(item.restaurant.id) }
                    )
                }
            }
        }
    }
}
