package com.openclaw.android.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openclaw.android.memory.Memory
import com.openclaw.android.ui.theme.*
import com.openclaw.android.ui.viewmodel.MemoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoryScreen(viewModel: MemoryViewModel = hiltViewModel()) {
    val memories by viewModel.memories.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LONG-TERM MEMORY", color = ClawGreen, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
            TextButton(onClick = { viewModel.clearAll() }) { Text("CLEAR ALL", color = ClawRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
        Text("${memories.size} memories stored on-device", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        if (memories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No memories yet.\nAs you chat, important context is remembered here.", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memories) { memory ->
                    MemoryCard(memory = memory, onDelete = { viewModel.deleteMemory(memory.id) })
                }
            }
        }
    }
}

@Composable
fun MemoryCard(memory: Memory, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    Surface(color = SurfaceDark, shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(6.dp))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(memory.category.uppercase(), color = ClawPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(fmt.format(Date(memory.timestamp)), color = TextMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(memory.summary, color = TextPrimary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("importance: ${"%.0f".format(memory.importance * 100)}%", color = TextMuted, fontSize = 10.sp)
                TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) { Text("DELETE", color = ClawRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
            }
        }
    }
}
