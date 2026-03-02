package com.openclaw.android.ui.screens

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.openclaw.android.agent.sandbox.ProjectType
import com.openclaw.android.agent.sandbox.SandboxProject
import com.openclaw.android.ui.theme.*
import com.openclaw.android.ui.viewmodel.SandboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxScreen(
    onBack: () -> Unit,
    viewModel: SandboxViewModel = hiltViewModel(),
) {
    val activeProject by viewModel.activeProject.collectAsState()
    val buildLog by viewModel.buildLog.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf("index.html") }
    var showPreview by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🧪", fontSize = 16.sp)
                        Text(
                            activeProject?.name?.uppercase() ?: "SANDBOX",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            color = TextPrimary,
                        )
                        if (activeProject != null) {
                            Surface(
                                color = ClawPurple.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(2.dp),
                            ) {
                                Text(
                                    activeProject!!.type.label.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = ClawPurple,
                                    letterSpacing = 1.sp,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextMuted)
                    }
                },
                actions = {
                    if (activeProject != null) {
                        IconButton(onClick = { showPreview = !showPreview }) {
                            Icon(
                                if (showPreview) Icons.Default.Code else Icons.Default.Preview,
                                "Toggle Preview",
                                tint = if (showPreview) ClawGreen else TextMuted,
                            )
                        }
                        IconButton(onClick = { viewModel.exportProject() }) {
                            Icon(Icons.Default.Share, "Export", tint = ClawPurple)
                        }
                    }
                    IconButton(onClick = { showNewProjectDialog = true }) {
                        Icon(Icons.Default.Add, "New Project", tint = ClawGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        }
    ) { padding ->
        if (activeProject == null) {
            SandboxEmptyState(
                onNew = { showNewProjectDialog = true },
                modifier = Modifier.padding(padding),
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // File tabs
                if (activeProject != null) {
                    FileTabs(
                        files = activeProject!!.files.keys.toList(),
                        selected = selectedFile,
                        onSelect = { selectedFile = it },
                    )
                }

                // Main editor / preview area
                if (showPreview) {
                    SandboxPreview(activeProject!!, modifier = Modifier.weight(1f))
                } else {
                    SandboxEditor(
                        project = activeProject!!,
                        filename = selectedFile,
                        onContentChange = { content -> viewModel.updateFile(selectedFile, content) },
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(color = BorderDark)

                // AI assistant prompt bar
                AiAssistBar(
                    prompt = aiPrompt,
                    onPromptChange = { aiPrompt = it },
                    isGenerating = isGenerating,
                    onGenerate = {
                        if (aiPrompt.isNotBlank()) {
                            viewModel.generateCode(aiPrompt, selectedFile)
                            aiPrompt = ""
                        }
                    },
                )

                // Build log
                if (buildLog.isNotEmpty()) {
                    BuildLogPanel(buildLog)
                }
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, type, desc ->
                viewModel.createProject(name, type, desc)
                showNewProjectDialog = false
            },
        )
    }
}

@Composable
private fun FileTabs(files: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2Dark)
            .horizontalScroll(rememberScrollState()),
    ) {
        files.forEach { filename ->
            val isSelected = filename == selected
            Box(
                modifier = Modifier
                    .clickable { onSelect(filename) }
                    .background(if (isSelected) SurfaceDark else Color.Transparent)
                    .border(
                        width = 0.dp,
                        color = Color.Transparent,
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    filename,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (isSelected) ClawGreen else TextMuted,
                )
            }
            if (isSelected) {
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(ClawGreen))
            }
        }
    }
}

@Composable
private fun SandboxEditor(
    project: SandboxProject,
    filename: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // In production: CodeMirror 6 in a sandboxed WebView with bidirectional JS bridge
    // Here we use a simple TextField with monospace styling as the scaffold
    val content = project.files[filename] ?: ""

    OutlinedTextField(
        value = content,
        onValueChange = onContentChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(0.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = TextPrimary,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = BgDark,
            unfocusedContainerColor = BgDark,
            cursorColor = ClawGreen,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        ),
        shape = RoundedCornerShape(0.dp),
    )
}

@Composable
private fun SandboxPreview(project: SandboxProject, modifier: Modifier = Modifier) {
    // Isolated WebView — no cookies, no storage, no cross-origin
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false    // no persistent storage
                    databaseEnabled = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    allowFileAccess = false
                    allowContentAccess = false
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                setBackgroundColor(android.graphics.Color.parseColor("#09090F"))
            }
        },
        update = { webView ->
            val html = project.files["index.html"] ?: ""
            val css = project.files["styles.css"] ?: ""
            val js = project.files["app.js"] ?: project.files["game.js"] ?: ""
            // Bundle inline for sandbox — no file:// access
            val bundled = html
                .replace("<link rel=\"stylesheet\" href=\"styles.css\">", "<style>$css</style>")
                .replace("<script src=\"app.js\"></script>", "<script>$js</script>")
                .replace("<script src=\"game.js\"></script>", "<script>$js</script>")
            webView.loadDataWithBaseURL(null, bundled, "text/html", "UTF-8", null)
        }
    )
}

@Composable
private fun AiAssistBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2Dark)
            .padding(8.dp)
            .imePadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🤖", fontSize = 16.sp)
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Describe a change... (on-device AI)", color = TextMuted, fontSize = 13.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ClawPurple,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = ClawPurple,
            ),
            shape = RoundedCornerShape(4.dp),
        )
        Button(
            onClick = onGenerate,
            enabled = prompt.isNotBlank() && !isGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = ClawPurple, contentColor = Color.White),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("GO", fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun BuildLogPanel(log: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color(0xFF080812))
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
    ) {
        log.forEach { entry ->
            Text(entry, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun SandboxEmptyState(onNew: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🧪", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("BUILD ANYTHING", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 4.sp, color = TextMuted)
        Spacer(Modifier.height(8.dp))
        Text("Apps · Games · Websites — all on-device", color = TextPrimary, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNew,
            colors = ButtonDefaults.buttonColors(containerColor = ClawPurple, contentColor = Color.White),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text("NEW PROJECT", fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, ProjectType, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ProjectType.WEB) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2Dark,
        title = {
            Text("NEW PROJECT", fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 3.sp, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClawPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = ClawPurple,
                    ),
                    shape = RoundedCornerShape(4.dp),
                )
                // Project type selection
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text("${type.icon} ${type.label}", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ClawPurple.copy(alpha = 0.2f),
                                selectedLabelColor = ClawPurple,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)", color = TextMuted) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClawPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = ClawPurple,
                    ),
                    shape = RoundedCornerShape(4.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, selectedType, description) },
                colors = ButtonDefaults.buttonColors(containerColor = ClawPurple),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text("CREATE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        },
    )
}
