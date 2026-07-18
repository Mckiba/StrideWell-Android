package com.stridewell.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of PUT /profile/units. `measurement_system` is "metric" or "imperial". */
@Serializable
data class UserUnitsRequest(
    @SerialName("measurement_system")
    val measurementSystem: String,
)

@Serializable
data class UserUnitsResponse(
    @SerialName("measurement_system")
    val measurementSystem: String,
)
