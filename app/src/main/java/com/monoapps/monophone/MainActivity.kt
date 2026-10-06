package com.monoapps.monophone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monoapps.monophone.ui.ContactsTab
import com.monoapps.monophone.ui.DialTab
import com.monoapps.monophone.ui.FavoritesTab
import com.monoapps.monophone.ui.PermissionScreen
import com.monoapps.monophone.ui.RecentsTab
import com.monoapps.monophone.ui.VoicemailNumberSheet
import com.monoapps.monophone.ui.canCallDirectly
import com.monoapps.monophone.ui.placeCall
import com.monoapps.monophone.ui.placeVoicemailCall
import com.monoapps.monophone.ui.systemVoicemailNumber
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

    // Installs that predate the voicemail badge never saw it on the first-run
    // screen; ask for phone state here and start the indicator on grant.
    val phoneStatePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.onPermissionsGranted() }
    LaunchedEffect(Unit) {
        val missing = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) != PackageManager.PERMISSION_GRANTED
        if (missing) phoneStatePermission.launch(Manifest.permission.READ_PHONE_STATE)
    }

    var tab by rememberSaveable {
        mutableStateOf(if (initialDial.isNotEmpty()) Tab.Dial else Tab.Favorites)
    }

    var pendingCall by remember { mutableStateOf<(() -> Unit)?>(null) }
    val callPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingCall?.invoke()
        pendingCall = null
    }

    fun whenCallable(action: () -> Unit) {
        if (canCallDirectly(context)) {
            action()
        } else {
            pendingCall = action
            callPermission.launch(Manifest.permission.CALL_PHONE)
        }
    }

    fun call(number: String) = whenCallable { placeCall(context, number) }

    // Voicemail: prefer the SIM's number, then the saved one, else ask for it.
    var askVoicemailNumber by remember { mutableStateOf(false) }
    fun callVoicemail() {
        when {
            systemVoicemailNumber(context) != null -> whenCallable { placeVoicemailCall(context) }
            viewModel.voicemailNumber.isNotBlank() -> call(viewModel.voicemailNumber)
            else -> askVoicemailNumber = true
        }
    }
    if (askVoicemailNumber) {
        VoicemailNumberSheet(
            onSave = { number ->
                askVoicemailNumber = false
                viewModel.saveVoicemailNumber(number)
                call(number)
            },
            onDismiss = { askVoicemailNumber = false },
        )
    }

    val bottomBar: @Composable () -> Unit = {
        NavigationBarMMD {
            Tab.entries.forEach { t ->
                NavigationBarItemMMD(
                    icon = {
                        Box {
                            Icon(t.icon, contentDescription = t.label)
                            // Dot on the Dial icon while voicemail is waiting.
                            if (t == Tab.Dial && viewModel.voicemailWaiting) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 5.dp, y = (-3).dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )
                            }
                        }
                    },
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
            voicemailWaiting = viewModel.voicemailWaiting,
            onCall = ::call,
            onCallVoicemail = ::callVoicemail,
            bottomBar = bottomBar,
        )
    }
}
