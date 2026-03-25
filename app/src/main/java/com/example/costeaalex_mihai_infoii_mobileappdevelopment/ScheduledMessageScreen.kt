package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ScheduledMessageScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current

    var message by remember { mutableStateOf("") }
    var timerMinutes by remember { mutableFloatStateOf(5f) }
    var isRunning by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var sent by remember { mutableStateOf(false) }

    // countdown ticker
    LaunchedEffect(isRunning) {
        if (isRunning) {
            secondsLeft = (timerMinutes * 60).toInt()
            while (secondsLeft > 0 && isRunning) {
                delay(1000L)
                secondsLeft--
            }
            if (isRunning && secondsLeft == 0) {
                // timer expired — send the message
                val contacts = ContactsManager.getContacts(context).take(5)
                val smsManager = android.telephony.SmsManager.getDefault()
                contacts.forEach { contact ->
                    try {
                        val parts = smsManager.divideMessage(message)
                        smsManager.sendMultipartTextMessage(
                            contact.phone, null, parts, null, null
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ScheduledMsg", "Failed: ${e.message}")
                    }
                }
                isRunning = false
                sent = true
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        FilledTonalButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("← Back")
        }

        Text(
            "Scheduled Message",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text("Message to send:", modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { message = it; sent = false },
            label = { Text("Your message") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            enabled = !isRunning
        )

        Text(
            "Timer: ${timerMinutes.toInt()} minute(s)",
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Slider(
            value = timerMinutes,
            onValueChange = { timerMinutes = it },
            valueRange = 1f..60f,
            steps = 58,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning
        )

        if (isRunning) {
            val mins = secondsLeft / 60
            val secs = secondsLeft % 60
            Text(
                "Sending in: %02d:%02d".format(mins, secs),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE64A19),
                modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
            )

            FilledTonalButton(
                onClick = { isRunning = false; sent = false },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFB71C1C),
                    contentColor = Color.White
                )
            ) {
                Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        } else {
            FilledTonalButton(
                onClick = {
                    if (message.isNotBlank()) {
                        isRunning = true
                        sent = false
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                enabled = message.isNotBlank()
            ) {
                Text("Start Timer")
            }
        }

        if (sent) {
            Text(
                "✓ Message sent to your top 5 contacts!",
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Text(
            "Message will be sent to your top 5 emergency contacts when the timer expires.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}