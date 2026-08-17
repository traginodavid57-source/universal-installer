package app.pwhs.universalinstaller.presentation.composable

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.install.InstallActivity
import app.pwhs.universalinstaller.presentation.manage.ManageActivity
import app.pwhs.universalinstaller.presentation.setting.SettingActivity
import app.pwhs.universalinstaller.ui.theme.LocalCustomGradientTheme
import app.pwhs.universalinstaller.ui.theme.LocalLiquidGlassStyle
import app.pwhs.universalinstaller.util.extension.disableSceneTransition

enum class BottomBarItem(
    val activityClass: Class<*>,
    val label: Int,
    val icon: ImageVector,
) {
    Install(InstallActivity::class.java, R.string.txt_install, Icons.Rounded.InstallMobile),
    Manage(ManageActivity::class.java, R.string.txt_manage, Icons.Rounded.Apps),
    Settings(SettingActivity::class.java, R.string.txt_setting, Icons.Rounded.Settings)
}

@Composable
fun BottomBar(
    currentTab: BottomBarItem
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val liquidGlass = LocalLiquidGlassStyle.current.enabled
    val itemColors = if (liquidGlass) {
        NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = Color.White,
            indicatorColor = Color.White.copy(alpha = 0.14f),
            unselectedIconColor = Color.White.copy(alpha = 0.70f),
            unselectedTextColor = Color.White.copy(alpha = 0.70f),
        )
    } else {
        NavigationBarItemDefaults.colors(
            selectedIconColor = colors.onPrimaryContainer,
            selectedTextColor = colors.primary,
            indicatorColor = colors.primaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        )
    }
    val customGradient = LocalCustomGradientTheme.current
    val glassShape = RoundedCornerShape(36.dp)
    val barModifier = if (liquidGlass) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(glassShape)
            .background(
                brush = Brush.linearGradient(
                    colors = if (customGradient.enabled) {
                        listOf(
                            Color.Black.copy(alpha = 0.74f),
                            (customGradient.colors.firstOrNull() ?: Color.White).copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.72f),
                        )
                    } else {
                        listOf(
                            Color(0xFF101010).copy(alpha = 0.88f),
                            colors.surfaceContainerHighest.copy(alpha = 0.34f),
                            Color(0xFF050505).copy(alpha = 0.86f),
                        )
                    }
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), glassShape)
    } else {
        Modifier.fillMaxWidth()
    }

    Box(modifier = if (liquidGlass) Modifier.background(Color.Transparent) else Modifier) {
        NavigationBar(
            modifier = barModifier,
            containerColor = if (liquidGlass) Color.Transparent else colors.surface,
            windowInsets = if (liquidGlass) WindowInsets.navigationBars else NavigationBarDefaults.windowInsets,
        ) {
            BottomBarItem.entries.forEach { destination ->
                val isSelected = currentTab == destination
                NavigationBarItem(
                    selected = isSelected,
                    colors = itemColors,
                    onClick = {
                        if (!isSelected) {
                            val intent = Intent(context, destination.activityClass).apply {
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                            }
                            context.startActivity(intent)
                            (context as? android.app.Activity)?.disableSceneTransition()
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.label)
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        }
    }
}
