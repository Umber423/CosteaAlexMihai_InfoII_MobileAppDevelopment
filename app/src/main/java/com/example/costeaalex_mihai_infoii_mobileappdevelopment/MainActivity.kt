package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.costeaalex_mihai_infoii_mobileappdevelopment.ui.theme.CosteaAlexMihai_InfoII_MobileAppDevelopmentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CosteaAlexMihai_InfoII_MobileAppDevelopmentTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
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
