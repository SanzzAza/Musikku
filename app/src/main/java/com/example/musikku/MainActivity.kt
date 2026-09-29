package com.example.musikku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.musikku.ui.MusikkuApp
import com.example.musikku.ui.theme.MusikkuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusikkuTheme {
                MusikkuApp()
            }
        }
    }
}
