package com.stridewell.app.model

import kotlinx.serialization.Serializable

// Weather cards from GET /home/cards. Generated and pre-sorted by the backend.
@Serializable
data class HomeCard(
    val type: String,
    val severity: String,
    val priority: Int,
    val icon: String,
    val title: String,
    val subtitle: String? = null,
    val deep_link: String? = null,
    val external_id: String? = null,
    val details_url: String? = null,
    val expires_at: String? = null
) {
    // Stable carousel id: alert id when present, else type+title.
    val id: String get() = external_id ?: "$type-$title"
}

@Serializable
data class HomeCardAttribution(val provider: String, val url: String)

@Serializable
data class HomeCardsResponse(
    val cards: List<HomeCard>,
    val attribution: HomeCardAttribution
)
