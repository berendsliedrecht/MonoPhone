package com.monoapps.monophone.ui

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monophone.R
import com.mudita.mmd.components.text.TextMMD

private val KEYS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("*", "0", "#"),
)

@Composable
fun DialTab(
    initialNumber: String,
    onCall: (String) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var number by rememberSaveable { mutableStateOf(initialNumber) }

    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    // Short tick so key presses are felt; e-ink gives no visual feedback.
    fun buzz() {
        vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    Scaffold(bottomBar = bottomBar) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TextMMD(
                    number,
                    fontSize = if (number.length > 12) 32.sp else 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
            KEYS.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(44.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    row.forEach { digit ->
                        DialKey(
                            digit = digit,
                            onPress = { buzz(); number += digit },
                            onLongPress = if (digit == "0") {
                                { buzz(); number += "+" }
                            } else null,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { buzz(); number = number.dropLast(1) },
                    enabled = number.isNotEmpty(),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 56.dp),
                ) {
                    if (number.isNotEmpty()) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Backspace,
                            contentDescription = "Delete digit",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                // Outlined call button, per the no-infill rule.
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color.Black, CircleShape)
                        .calmClickable { buzz(); if (number.isNotBlank()) onCall(number) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_call),
                        contentDescription = "Call",
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKey(
    digit: String,
    onPress: () -> Unit,
    onLongPress: (() -> Unit)?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 76.dp, height = 64.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onPress,
                onLongClick = onLongPress,
            ),
    ) {
        TextMMD(digit, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    }
}
