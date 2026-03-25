package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlertsScreen(
    alerts: List<NationalAlert>,
    onBack: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        FilledTonalButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("← Back")
        }

        Text(
            "National Alerts",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (alerts.isEmpty()) {
            Text("No active alerts in your area.", color = Color.Gray)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(alerts, key = { it.id }) { alert ->
                val cardColor = when (alert.severity.lowercase()) {
                    "extreme" -> Color(0xFFB71C1C)
                    "severe" -> Color(0xFFE64A19)
                    "moderate" -> Color(0xFFF57F17)
                    else -> Color(0xFF1565C0)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            alert.event,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            alert.headline,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "Area: ${alert.area}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "Severity: ${alert.severity}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}