package com.inscopelabs.abx.xtools.plugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.inscopelabs.abx.xtools.ui.theme.XToolsTheme

class PluginHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XToolsTheme {
                // Plugin Host Activity container
            }
        }
    }
}
