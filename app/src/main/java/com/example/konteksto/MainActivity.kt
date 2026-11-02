package com.example.konteksto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konteksto.ui.theme.KontekstoBlue
import com.example.konteksto.ui.theme.KontekstoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KontekstoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val isDarkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val horizontalLogoRes = if (isDarkBackground || isSystemInDarkTheme()) {
        R.drawable.ic_logo_horizontal_light
    } else {
        R.drawable.ic_logo_horizontal
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Horizontal Logo Header
            Image(
                painter = painterResource(id = horizontalLogoRes),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(60.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Logo Icon Display Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_icon),
                        contentDescription = "Konteksto Logo Icon",
                        modifier = Modifier.size(100.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = KontekstoBlue
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenLightPreview() {
    KontekstoTheme(darkTheme = false) {
        HomeScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenDarkPreview() {
    KontekstoTheme(darkTheme = true) {
        HomeScreen()
    }
}
