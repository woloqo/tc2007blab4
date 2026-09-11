package mx.tec.sabores.data.remote

import mx.tec.sabores.domain.Restaurant
import mx.tec.sabores.domain.Review
import mx.tec.sabores.domain.RatingSummary

fun RestaurantDto.toDomain() = Restaurant(
    id = id,
    name = name,
    cuisine = cuisine,
    address = address,
    description = description,
    priceLevel = priceLevel,
    emoji = emoji
)

fun ReviewDto.toDomain() = Review(
    id = id,
    restaurantId = restaurantId,
    author = author,
    stars = stars,
    comment = comment
)

fun RestaurantDto.toSummary() = RatingSummary(average = ratingAverage, count = ratingCount)