package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlphaScreen(
    modifier: Modifier = Modifier,
    onMicClick: () -> Unit = {},
    onEmergencyCall: () -> Unit = {},
    isListening: Boolean = false,
    onContactsClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {},
    onScheduledMessageClick: () -> Unit = {},
    onGeofenceClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var phoneNumber by remember {
        mutableStateOf(EmergencyManager.getEmergencyNumber(context))
    }
    var saved by remember { mutableStateOf(false) }
    var savedNumber by remember { mutableStateOf(EmergencyManager.getEmergencyNumber(context)) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Emergency Contact", fontSize = 22.sp, modifier = Modifier.padding(bottom = 24.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                saved = false
            },
            label = { Text("Phone number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        FilledTonalButton(
            onClick = {
                EmergencyManager.setEmergencyNumber(context, phoneNumber)
                savedNumber = phoneNumber
                saved = true
            },
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
        ) {
            Text("Save Contact")
        }

        if (saved) {
            Text("Saved! $savedNumber", modifier = Modifier.padding(top = 8.dp), color = Color.Green)
        }

        FilledTonalButton(
            onClick = onEmergencyCall,
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth()
        ) {
            Text("Emergency Now")
        }

        FilledTonalButton(
            onClick = onMicClick,
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isListening) Color(0xFF2E7D32) else Color(0xFF424242),
                contentColor = Color.White
            )
        ) {
            Text(if (isListening) "🎙 Listening..." else "Start Listening")
        }

        FilledTonalButton(
            onClick = onContactsClick,
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
        ) {
            Text("Manage Contacts")
        }

        FilledTonalButton(
            onClick = onAlertsClick,
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
        ) {
            Text("National Alerts")
        }

        FilledTonalButton(
            onClick = onScheduledMessageClick,
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
        ) {
            Text("Scheduled Message")
        }

        FilledTonalButton(
            onClick = onGeofenceClick,
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
        ) {
            Text("Geofence Zone")
        }
    }
}