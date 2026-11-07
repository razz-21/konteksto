package com.example.konteksto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.R
import com.example.konteksto.ui.theme.KontekstoTheme

/** Marks a session the host has started, laid over its banner image. */
@Composable
fun LiveOnAirBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.onError, CircleShape),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(id = R.string.host_live_on_air).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            ),
            color = MaterialTheme.colorScheme.onError,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveOnAirBadgeLightPreview() {
    KontekstoTheme(darkTheme = false) {
        LiveOnAirBadge()
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveOnAirBadgeDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        LiveOnAirBadge()
    }
}
