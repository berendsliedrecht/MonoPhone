@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.monoapps.monophone.ui

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monophone.data.ContactRow
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

@Composable
fun ContactsTab(
    contacts: List<ContactRow>,
    bottomBar: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val shown = if (!searching || query.isBlank()) contacts
    else contacts.filter { it.displayName.contains(query.trim(), ignoreCase = true) }

    fun openContact(contact: ContactRow) {
        startSafely(
            context,
            Intent(
                Intent.ACTION_VIEW,
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id.toString()),
            ),
        )
    }

    Scaffold(
        topBar = {
            if (searching) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                TopAppBarMMD(
                    navigationIcon = {
                        IconButton(onClick = { searching = false; query = "" }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Stop searching",
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    },
                    title = {
                        Box {
                            if (query.isEmpty()) {
                                TextMMD("Search for contacts", fontSize = 24.sp, color = Color.Gray)
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                ),
                                cursorBrush = SolidColor(Color.Black),
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            )
                        }
                    },
                )
            } else {
                TopAppBarMMD(
                    title = { TextMMD("Contacts", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = {
                            startSafely(
                                context,
                                Intent(Intent.ACTION_INSERT).setType(ContactsContract.Contacts.CONTENT_TYPE),
                            )
                        }) {
                            Icon(
                                Icons.Outlined.PersonAddAlt,
                                contentDescription = "New contact",
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        IconButton(onClick = { searching = true }) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = "Search contacts",
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    },
                )
            }
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumnMMD(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(shown, key = { it.id }) { contact ->
                Column(modifier = Modifier.fillMaxWidth().calmClickable { openContact(contact) }) {
                    TextMMD(
                        text = styledName(contact.displayName),
                        fontSize = 22.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    )
                    DashedDivider(modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}
