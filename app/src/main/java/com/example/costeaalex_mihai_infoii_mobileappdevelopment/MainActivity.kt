package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.costeaalex_mihai_infoii_mobileappdevelopment.ui.theme.CosteaAlexMihai_InfoII_MobileAppDevelopmentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CosteaAlexMihai_InfoII_MobileAppDevelopmentTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

interface Notifiable {
    fun sendNotification(): String;
}

abstract class Notification(message: String) {

    private var _message: String = message;

    fun getMessage(): String = _message;

    fun setMessage(newMessage: String) {
        require(newMessage.isNotBlank()) { "Message cannot be blank." };
        _message = newMessage;
    }

    fun displayNotification(): String =
        "Notification → ${getMessage()}";
}

class SMSNotification(
    message: String,
    private val phoneNumber: String
) : Notification(message), Notifiable {

    override fun sendNotification(): String =
        "${displayNotification()}\n📱[SMS] To: $phoneNumber\n\"${getMessage()}\"";
}

class EmailNotification(
    message: String,
    private val emailAddress: String,
    private val subject: String = "No Subject"
) : Notification(message), Notifiable {

    override fun sendNotification(): String =
        "${displayNotification()}\n [Email] To: $emailAddress\nSubject: \"$subject\"\n\"${getMessage()}\"";
}

class PushNotification(
    message: String,
    private val deviceToken: String,
    private val title: String = "New Notification"
) : Notification(message), Notifiable {

    override fun sendNotification(): String =
        "${displayNotification()}\n [Push] Device: $deviceToken\nTitle: \"$title\"\n\"${getMessage()}\"";
}
fun capsSorter(input: String): String {
    val chars = input.toCharArray()
    for(j in 0 until chars.size - 1) {
        for (i in 0 until chars.size - 1) {
            if (chars[i].isUpperCase()) {
                val temp = chars[i]
                chars[i] = chars[i + 1]
                chars[i + 1] = temp
            }
        }
    }
    return String(chars)
}

fun friendlyNumbers(number1: Int, number2: Int): String {
    var sum1 = 0;
    var sum2 = 0;
    for (i in 1 until number1/2+1) {
        if(number1 % i == 0)
            sum1 += i
    }
    for (i in 1 until number2/2+1) {
        if(number2 % i == 0)
            sum2 += i
    }
    if(sum1 == number2 && sum2 == number1)
        return "YES"
    else
        return "NO"
}

fun hexToDec(input: String): Int {
    return input.toInt(16)
}

fun valleyCounter(input: String): Int {
    val chars = input.toCharArray()
    var level = 0;
    var valleys = 0;
    var prev = 0;
    for(j in 0 until chars.size - 1) {
        prev = level;
        if(chars[j] == 'D')
            level--
        else if(chars[j] == 'U')
            level++
        if(level == 0 && prev < 0)
            valleys++
    }
    return valleys;
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    // Pre-built notifications
    val notifications: List<Notifiable> = listOf(
        SMSNotification(
            message     = "Your OTP is 482910. Valid for 5 minutes.",
            phoneNumber = "+40712345678"
        ),
        EmailNotification(
            message      = "Welcome! Your account has been created successfully.",
            emailAddress = "user@example.com",
            subject      = "Account Created"
        ),
        PushNotification(
            message     = "You have a new message from Alice.",
            deviceToken = "device_token_abc123",
            title       = "New Message"
        )
    )

    var log by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Utility functions section ──
        SectionTitle("Utility Functions")

        InfoCard("capsSorter(\"Hello, World!\")")  { capsSorter("Hello, World!") }
        InfoCard("friendlyNumbers(220, 284)")       { friendlyNumbers(220, 284) }
        InfoCard("hexToDec(\"1A\")")                { hexToDec("1A").toString() }
        InfoCard("valleyCounter(\"DDUUDDUUDUU\")") { valleyCounter("DDUUDDUUDUU").toString() }

        Spacer(Modifier.height(8.dp))

        // ── Notification system section ──
        SectionTitle("Notification System")

        notifications.forEach { notif ->
            val label = when (notif) {
                is SMSNotification   -> "Send SMS"
                is EmailNotification -> "Send Email"
                is PushNotification  -> "Send Push"
                else                 -> "Send"
            }
            Button(
                onClick = { log = notif.sendNotification() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(label)
            }
        }

        if (log.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text     = log,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// UI Made by Ai xp
@Composable
fun SectionTitle(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun InfoCard(label: String, result: () -> String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            Text(text = result(), fontSize = 15.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CosteaAlexMihai_InfoII_MobileAppDevelopmentTheme {
        MainScreen()
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    Text(
        text = capsSorter("Hello, World!") + "\n" + friendlyNumbers(220, 284) +
        "\n" + hexToDec("1A"),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CosteaAlexMihai_InfoII_MobileAppDevelopmentTheme {
        Greeting()
    }
}
