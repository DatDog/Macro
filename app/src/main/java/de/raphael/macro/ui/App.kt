package de.raphael.macro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import de.raphael.macro.data.Model

@Composable
fun MacroApp(model: Model) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current
    BackHandler(enabled = adding) { adding = false }

    Scaffold(
        containerColor = C.Bg,
        contentColor = C.Text,
        bottomBar = {
            if (!adding) {
                BottomBar(tab) {
                    if (it != tab && model.settings.haptics) view.tickOff()
                    tab = it
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(240, delayMillis = 70)) +
                        slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { (it * 0.07f).toInt() * dir })
                        .togetherWith(
                            fadeOut(tween(110)) +
                                slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { -(it * 0.07f).toInt() * dir }
                        )
                },
                label = "tab",
            ) { t ->
                when (t) {
                    0 -> TodayScreen(model, onAdd = { adding = true }, onSetup = { tab = 2 })
                    1 -> HistoryScreen(model)
                    else -> MoreScreen(model)
                }
            }
            AnimatedVisibility(
                visible = adding,
                enter = slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(240)),
                exit = slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it / 3 } + fadeOut(tween(180)),
            ) {
                AddScreen(model, onClose = { adding = false })
            }
        }
    }
}

@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    NavigationBar(containerColor = C.Bg, contentColor = C.Text, tonalElevation = 0.dp) {
        listOf("Heute", "Verlauf", "Mehr").forEachIndexed { i, label ->
            NavigationBarItem(
                selected = tab == i,
                onClick = { onTab(i) },
                icon = { TabIcon(i) },
                label = { T(label, Ty.Label, Color.Unspecified) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = C.Accent,
                    selectedTextColor = C.Accent,
                    indicatorColor = C.CardHi,
                    unselectedIconColor = C.Muted,
                    unselectedTextColor = C.Muted,
                ),
            )
        }
    }
}

@Composable
private fun TabIcon(kind: Int) {
    val col = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val sw = 2.2.dp.toPx()
        when (kind) {
            0 -> {
                val m = 3.dp.toPx()
                drawArc(
                    color = col, startAngle = 130f, sweepAngle = 280f, useCenter = false,
                    topLeft = Offset(m, m), size = Size(size.width - 2 * m, size.height - 2 * m),
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
                drawCircle(color = col, radius = 2.2.dp.toPx(), center = center)
            }
            1 -> {
                val heights = listOf(0.35f, 0.7f, 0.5f)
                val bottom = size.height * 0.85f
                heights.forEachIndexed { i, h ->
                    val x = size.width * (0.24f + i * 0.26f)
                    drawLine(
                        color = col, start = Offset(x, bottom), end = Offset(x, bottom - size.height * h),
                        strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round,
                    )
                }
            }
            else -> {
                for (i in 0..2) {
                    drawCircle(color = col, radius = 2.2.dp.toPx(), center = Offset(size.width * (0.22f + i * 0.28f), center.y))
                }
            }
        }
    }
}
