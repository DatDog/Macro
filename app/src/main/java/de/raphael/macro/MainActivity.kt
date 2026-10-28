package de.raphael.macro

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.raphael.macro.data.Model
import de.raphael.macro.ui.MacroApp
import de.raphael.macro.ui.MacroTheme

class MainActivity : ComponentActivity() {
    private lateinit var model: Model

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        model = Model(applicationContext)
        setContent { MacroTheme { MacroApp(model) } }
    }

    override fun onResume() {
        super.onResume()
        model.refreshToday()
        model.refreshHealthLocal()
        model.refreshKochbuch()
        model.refreshHc()
        de.raphael.macro.widget.MacroWidgets.refresh(this)
    }
}
