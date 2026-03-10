package com.champengine.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.champengine.android.network.Job
import com.champengine.android.ui.viewmodel.ProjectViewModel

@Composable
fun ProjectScreen(viewModel: ProjectViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var selectedAutonomy by remember { mutableStateOf("FULL_AUTO") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(16.dp)
    ) {
        Text(
            text = "Projects",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Input area
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text("Describe your project...", color = Color(0xFF666666)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00FF88),
                unfocusedBorderColor = Color(0xFF333333),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF00FF88)
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Autonomy selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("FULL_AUTO", "GUIDED", "COLLABORATIVE").forEach { mode ->
                val selected = selectedAutonomy == mode
                OutlinedButton(
                    onClick = { selectedAutonomy = mode },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) Color(0xFF00FF88) else Color.Transparent,
                        contentColor = if (selected) Color.Black else Color(0xFF00FF88)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (selected) Color(0xFF00FF88) else Color(0xFF333333)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(
                        text = mode.replace("_", "\n"),
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Submit button
        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    viewModel.createProject(inputText.trim(), selectedAutonomy)
                    inputText = ""
                }
            },
            enabled = inputText.isNotBlank() && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00FF88),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF1A3A2A),
                disabledContentColor = Color(0xFF446655)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Launch Project", fontWeight = FontWeight.Bold)
            }
        }

        uiState.error?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = err, color = Color(0xFFFF4444), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Refresh button + header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Projects", color = Color(0xFF888888), fontSize = 14.sp)
            TextButton(onClick = { viewModel.loadProjects() }) {
                Text("Refresh", color = Color(0xFF00FF88), fontSize = 12.sp)
            }
        }

        // Job list
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.jobs) { job ->
                JobCard(job = job)
            }
            if (uiState.jobs.isEmpty() && !uiState.isLoading) {
                item {
                    Text(
                        "No projects yet. Launch something above.",
                        color = Color(0xFF444444),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun JobCard(job: Job) {
    val statusColor = when (job.status) {
        "complete" -> Color(0xFF00FF88)
        "running" -> Color(0xFFFFAA00)
        "failed" -> Color(0xFFFF4444)
        "queued" -> Color(0xFF4488FF)
        else -> Color(0xFF666666)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = job.projectType.uppercase(),
                color = Color(0xFF00FF88),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = job.status.uppercase(),
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = job.description.take(120) + if (job.description.length > 120) "…" else "",
            color = Color.White,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "ID: ${job.jobId}", color = Color(0xFF555555), fontSize = 11.sp)
            Text(text = job.priceDisplay, color = Color(0xFF888888), fontSize = 11.sp)
        }

        if (job.logs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = job.logs.last(),
                color = Color(0xFF555555),
                fontSize = 11.sp
            )
        }

        if (job.deliverable != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "✓ Deliverable ready",
                color = Color(0xFF00FF88),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
