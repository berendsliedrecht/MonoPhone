@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.monoapps.monophone.ui

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monophone.data.ContactRow
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

@Composable
fun FavoritesTab(
    favorites: List<ContactRow>,
    onCall: (ContactRow) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBarMMD(
                title = { TextMMD("Favorites", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (favorites.isEmpty()) {
                TextMMD(
                    "No favorites yet…",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
                    items(favorites, key = { it.id }) { contact ->
                        Column(modifier = Modifier.fillMaxWidth().calmClickable { onCall(contact) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextMMD(
                                    text = styledName(contact.displayName),
                                    fontSize = 22.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 20.dp, vertical = 20.dp),
                                )
                                IconButton(onClick = {
                                    startSafely(
                                        context,
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.withAppendedPath(
                                                ContactsContract.Contacts.CONTENT_URI,
                                                contact.id.toString(),
                                            ),
                                        ),
                                    )
                                }) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = "Contact details",
                                        modifier = Modifier.size(30.dp),
                                    )
                                }
                            }
                            DashedDivider(modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }
                }
            }
        }
    }
}
