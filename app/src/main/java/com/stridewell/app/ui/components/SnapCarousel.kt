package com.stridewell.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stridewell.R
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

data class BannerItem(
    val id: String,
    val title1: String,
    val subtitle: String,
    val onTap: () -> Unit = {},
)

@Composable
fun <T> SnapCarousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 300.dp,
    cardHeight: Dp = 99.dp,
    key: (T) -> Any = { it.hashCode() },
    content: @Composable (T) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
        ) {
            val sideInset = maxOf(Spacing.md, (maxWidth - cardWidth) / 2)

            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(cardWidth),
                pageSpacing = Spacing.md,
                contentPadding = PaddingValues(horizontal = sideInset),
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                key(key(items[page])) {
                    content(items[page])
                }
            }
        }

        if (items.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(items.size) { index ->
                    PagerDash(selected = index == pagerState.currentPage)
                }
            }
        }
    }
}

@Composable
private fun PagerDash(
    selected: Boolean
) {
    Spacer(
        modifier = Modifier
            .width(24.dp)
            .height(3.dp)
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                },
                shape = RoundedCornerShape(1.5.dp)
            )
    )
}

@Preview(showBackground = true)
@Composable
private fun SnapCarouselPreview() {
    val items = listOf(
        BannerItem("a", "Great work out there!", "Let's talk about that last run"),
        BannerItem("b", "Let's Review the Plan", "Your plan has been updated"),
        BannerItem("c", "Time to check In!", "Lets check in to see how you're doing")
    )

    StridewellTheme {
        SnapCarousel(items = items) { item ->
            ActivityBannerView(
                title1 = item.title1,
                subtitle = item.subtitle,
                imageRes = R.drawable.onboarding_background
            )
        }
    }
}
