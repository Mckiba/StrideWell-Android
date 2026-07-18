package com.stridewell.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridewell.R
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.SofiaSansFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

// Weather/home card. Forked from ActivityBannerView so it renders identically in
// the banner carousel, but kept separate so weather cards can diverge later.
@Composable
fun WeatherBannerView(
    title1: String,           // headline, e.g. "High UV (10)"
    title2: String,           // advice, e.g. "Wear sunscreen."
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int? = null,
    onTap: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .width(300.dp)
            .fillMaxWidth()
            .height(99.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(CornerRadius.sm),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(CornerRadius.sm)
            )
            .clickable(enabled = onTap != null) { onTap?.invoke() }
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top
    ) {
        imageRes?.let { resId ->
            Icon(
                painter = painterResource(resId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface, // black (light) / white (dark)
                modifier = Modifier.size(60.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = title1,
                style = weatherTextStyle(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (title2.isNotEmpty()) {
                Text(
                    text = title2,
                    style = weatherTextStyle(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun weatherTextStyle() = TextStyle(
    fontFamily = SofiaSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

@Preview(showBackground = true)
@Composable
private fun WeatherBannerViewPreview() {
    StridewellTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            WeatherBannerView(
                title1 = "High UV (10)",
                title2 = "Wear sunscreen.",
                imageRes = R.drawable.onboarding_background,
                onTap = {}
            )
            WeatherBannerView(
                title1 = "Severe Thunderstorm Warning",
                title2 = "Tap to learn more.",
                imageRes = R.drawable.onboarding_background,
                onTap = {}
            )
        }
    }
}
