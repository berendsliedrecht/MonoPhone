package com.monoapps.monophone

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monoapps.monophone.ui.ContactsTab
import com.monoapps.monophone.ui.DialTab
import com.monoapps.monophone.ui.FavoritesTab
import com.monoapps.monophone.ui.PermissionScreen
import com.monoapps.monophone.ui.RecentsTab
import com.monoapps.monophone.ui.canCallDirectly
import com.monoapps.monophone.ui.placeCall
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.components.nav_bar.NavigationBarItemMMD
import com.mudita.mmd.components.nav_bar.NavigationBarMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.launch

enum class Tab(val label: String, val icon: ImageVector) {
    Favorites("Favorites", Icons.Outlined.StarBorder),
    Recents("Recents", Icons.Outlined.Schedule),
    Contacts("Contacts", Icons.Outlined.Group),
    Dial("Dial", Icons.Outlined.Dialpad),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dialNumber = intent
            ?.takeIf { it.action == Intent.ACTION_DIAL }
            ?.data?.schemeSpecificPart
            .orEmpty()
        setContent { ThemeMMD { App(dialNumber) } }
    }
}

@Composable
private fun App(initialDial: String) {
    val viewModel: PhoneViewModel = viewModel()
    if (!viewModel.hasPermissions) {
        PermissionScreen(onGranted = { viewModel.onPermissionsGranted() })
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable {
        mutableStateOf(if (initialDial.isNotEmpty()) Tab.Dial else Tab.Favorites)
    }

    var pendingNumber by remember { mutableStateOf<String?>(null) }
    val callPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingNumber?.let { placeCall(context, it) }
        pendingNumber = null
    }

    fun call(number: String) {
        if (canCallDirectly(context)) {
            placeCall(context, number)
        } else {
            pendingNumber = number
            callPermission.launch(Manifest.permission.CALL_PHONE)
        }
    }

    val bottomBar: @Composable () -> Unit = {
        NavigationBarMMD {
            Tab.entries.forEach { t ->
                NavigationBarItemMMD(
                    icon = { Icon(t.icon, contentDescription = t.label) },
                    label = {
                        TextMMD(
                            t.label,
                            fontSize = 16.sp,
                            fontWeight = if (tab == t) FontWeight.Black else FontWeight.Medium,
                        )
                    },
                    selected = tab == t,
                    onClick = { tab = t },
                )
            }
        }
    }

    when (tab) {
        Tab.Favorites -> FavoritesTab(
            favorites = viewModel.contacts.filter { it.starred },
            onCall = { contact ->
                scope.launch { viewModel.primaryNumber(contact.id)?.let(::call) }
            },
            bottomBar = bottomBar,
        )

        Tab.Recents -> RecentsTab(
            recents = viewModel.recents,
            onCall = ::call,
            bottomBar = bottomBar,
        )

        Tab.Contacts -> ContactsTab(
            contacts = viewModel.contacts,
            bottomBar = bottomBar,
        )

        Tab.Dial -> DialTab(
            initialNumber = initialDial,
            onCall = ::call,
            bottomBar = bottomBar,
        )
    }
}
