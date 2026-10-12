package de.raphael.macro

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.raphael.macro.ui.BigButton
import de.raphael.macro.ui.C
import de.raphael.macro.ui.MacroTheme
import de.raphael.macro.ui.T
import de.raphael.macro.ui.Ty

/** Erklärung für Health Connect: wofür Macro die Berechtigung zum Schreiben von Ernährungsdaten braucht. */
class RationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
        )
        setContent {
            MacroTheme {
                Column(Modifier.fillMaxSize().background(C.Bg).systemBarsPadding().padding(24.dp)) {
                    Spacer(Modifier.height(16.dp))
                    T("Health Connect", Ty.Title)
                    Spacer(Modifier.height(16.dp))
                    T(
                        "Macro schreibt deine eingetragenen Mahlzeiten (Kalorien, Eiweiß, Kohlenhydrate, Fett) in Health Connect, " +
                            "damit andere Apps auf deinem Handy sie nutzen können, z. B. Health Local für die Energiebilanz.",
                        Ty.Body,
                    )
                    Spacer(Modifier.height(12.dp))
                    T(
                        "Macro liest nichts aus Health Connect und sendet nichts ins Internet. Du kannst das Schreiben jederzeit " +
                            "unter Mehr in Macro ausschalten oder die Berechtigung in Health Connect entziehen.",
                        Ty.Body, C.Muted,
                    )
                    Spacer(Modifier.height(24.dp))
                    BigButton("Schließen") { finish() }
                }
            }
        }
    }
}
