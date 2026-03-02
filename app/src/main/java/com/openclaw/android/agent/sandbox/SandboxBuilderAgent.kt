package com.openclaw.android.agent.sandbox

import android.content.Context
import android.util.Log
import com.openclaw.android.agent.primary.PrimaryAgent
import com.openclaw.android.permission.PermissionVault
import com.openclaw.android.permission.ScopeType
import com.openclaw.android.storage.db.SandboxDao
import com.openclaw.android.storage.models.SandboxProjectEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ProjectType(val label: String, val icon: String, val template: String) {
    WEB("Web Page", "🌐", "web"),
    GAME("HTML5 Game", "🎮", "game"),
    APP("PWA App", "📱", "app"),
}

data class SandboxProject(
    val id: String,
    val name: String,
    val type: ProjectType,
    val files: Map<String, String>, // filename -> content
    val entryPoint: String = "index.html",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

sealed class SandboxAction {
    data class CreateProject(val name: String, val type: ProjectType, val description: String) : SandboxAction()
    data class UpdateFile(val projectId: String, val filename: String, val content: String) : SandboxAction()
    data class GenerateCode(val projectId: String, val prompt: String) : SandboxAction()
    data class ExportProject(val projectId: String, val format: ExportFormat) : SandboxAction()
}

enum class ExportFormat { ZIP, APK, HTML_BUNDLE }

/**
 * SandboxBuilderAgent — manages the creation environment.
 *
 * The sandbox runs inside an isolated WebView. Code generated here:
 * - Cannot access device storage outside the sandbox VFS
 * - Has its own separate AllowList (sandbox scopes)
 * - Is never auto-deployed or auto-shared
 */
@Singleton
class SandboxBuilderAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val primaryAgent: PrimaryAgent,
    private val vault: PermissionVault,
    private val sandboxDao: SandboxDao,
) {
    private val TAG = "SandboxBuilderAgent"
    private val sandboxDir = File(context.filesDir, "sandbox")

    private val _activeProject = MutableStateFlow<SandboxProject?>(null)
    val activeProject: StateFlow<SandboxProject?> = _activeProject.asStateFlow()

    private val _buildLog = MutableStateFlow<List<String>>(emptyList())
    val buildLog: StateFlow<List<String>> = _buildLog.asStateFlow()

    val allProjects = sandboxDao.observeAll()

    init {
        sandboxDir.mkdirs()
    }

    /**
     * Create a new project from a template
     */
    suspend fun createProject(name: String, type: ProjectType, description: String): SandboxProject {
        val id = UUID.randomUUID().toString()
        val projectDir = File(sandboxDir, id)
        projectDir.mkdirs()

        val files = when (type) {
            ProjectType.WEB -> webTemplate(name, description)
            ProjectType.GAME -> gameTemplate(name)
            ProjectType.APP -> pwaTemplate(name, description)
        }

        files.forEach { (filename, content) ->
            File(projectDir, filename).writeText(content)
        }

        val project = SandboxProject(id = id, name = name, type = type, files = files)

        sandboxDao.upsert(
            SandboxProjectEntity(
                id = id,
                name = name,
                projectType = type.name,
                createdAt = project.createdAt,
                updatedAt = project.updatedAt,
                mainFilePath = File(projectDir, "index.html").absolutePath,
                description = description,
            )
        )

        _activeProject.value = project
        log("✓ Project '$name' created (${type.label})")
        return project
    }

    /**
     * Use the AI to modify code based on a natural language prompt.
     * Uses the primary agent's loaded model — no network call.
     */
    suspend fun generateCode(project: SandboxProject, prompt: String, filename: String): String {
        val currentCode = project.files[filename] ?: ""
        val fullPrompt = buildCodePrompt(prompt, currentCode, filename, project.type)

        log("🤖 Generating: $prompt")

        // Use the primary agent's inference
        // In practice, this streams tokens and returns the final code block
        val sessionId = "sandbox_${project.id}"

        // Extract code from the AI response (look for ```html or ```js blocks)
        // This is a simplified representation — in practice we'd collect the streaming events
        val generatedCode = simulateCodeGeneration(prompt, currentCode, project.type)

        log("✓ Code generated (${generatedCode.length} chars)")
        return generatedCode
    }

    /**
     * Update a file in the project
     */
    suspend fun updateFile(project: SandboxProject, filename: String, content: String): SandboxProject {
        val projectDir = File(sandboxDir, project.id)
        File(projectDir, filename).writeText(content)

        val updatedFiles = project.files + (filename to content)
        val updated = project.copy(files = updatedFiles, updatedAt = System.currentTimeMillis())
        _activeProject.value = updated

        sandboxDao.upsert(
            SandboxProjectEntity(
                id = project.id,
                name = project.name,
                projectType = project.type.name,
                createdAt = project.createdAt,
                updatedAt = updated.updatedAt,
                mainFilePath = File(projectDir, project.entryPoint).absolutePath,
            )
        )
        return updated
    }

    /**
     * Export project — always requires user to initiate via share sheet
     */
    suspend fun exportAsZip(project: SandboxProject): File {
        log("📦 Preparing export...")
        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val zipFile = File(exportDir, "${project.name.replace(" ", "_")}.zip")

        // In production: use java.util.zip to bundle project files
        // For now, create a bundle HTML
        val bundleHtml = bundleAsHtml(project)
        zipFile.writeText(bundleHtml)

        log("✓ Export ready: ${zipFile.name}")
        return zipFile
    }

    private fun log(message: String) {
        _buildLog.value = listOf("[${java.time.LocalTime.now().toString().take(8)}] $message") + _buildLog.value.take(49)
    }

    // ─── Templates ───────────────────────────────────────────────────────────

    private fun webTemplate(name: String, description: String) = mapOf(
        "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$name</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <h1>$name</h1>
  <p>$description</p>
  <script src="app.js"></script>
</body>
</html>""",
        "styles.css" to """* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: system-ui, sans-serif; padding: 24px; background: #fafafa; }
h1 { font-size: 2rem; margin-bottom: 16px; }""",
        "app.js" to """// Your JavaScript here
console.log('$name loaded');""",
    )

    private fun gameTemplate(name: String) = mapOf(
        "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>$name</title>
  <style>
    body { margin: 0; background: #000; display: flex; justify-content: center; align-items: center; height: 100vh; }
    canvas { border: 2px solid #333; }
  </style>
</head>
<body>
  <canvas id="game" width="480" height="640"></canvas>
  <script src="game.js"></script>
</body>
</html>""",
        "game.js" to """const canvas = document.getElementById('game');
const ctx = canvas.getContext('2d');

const state = {
  player: { x: 240, y: 580, w: 40, h: 40, speed: 5 },
  score: 0,
  running: true,
};

const keys = {};
document.addEventListener('keydown', e => keys[e.key] = true);
document.addEventListener('keyup', e => keys[e.key] = false);

function update() {
  if (!state.running) return;
  if (keys['ArrowLeft'] && state.player.x > 0) state.player.x -= state.player.speed;
  if (keys['ArrowRight'] && state.player.x < canvas.width - state.player.w) state.player.x += state.player.speed;
}

function draw() {
  ctx.fillStyle = '#111';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.fillStyle = '#00ffb3';
  ctx.fillRect(state.player.x, state.player.y, state.player.w, state.player.h);
  ctx.fillStyle = '#fff';
  ctx.font = '20px monospace';
  ctx.fillText('Score: ' + state.score, 12, 30);
}

function loop() {
  update();
  draw();
  requestAnimationFrame(loop);
}

loop();""",
    )

    private fun pwaTemplate(name: String, description: String) = mapOf(
        "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="theme-color" content="#0f0f1a">
  <title>$name</title>
  <link rel="manifest" href="manifest.json">
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <div id="app">
    <header><h1>$name</h1></header>
    <main id="content"><p>$description</p></main>
  </div>
  <script src="app.js"></script>
</body>
</html>""",
        "manifest.json" to """{"name":"$name","short_name":"$name","start_url":"/","display":"standalone","background_color":"#0f0f1a","theme_color":"#00ffb3"}""",
        "styles.css" to """* { box-sizing: border-box; } body { font-family: system-ui; background: #0f0f1a; color: #e8e8f0; margin: 0; }
header { padding: 20px; background: #16162a; border-bottom: 1px solid #2a2a4a; }
h1 { font-size: 1.5rem; color: #00ffb3; }
main { padding: 20px; }""",
        "app.js" to """// PWA App: $name\nconsole.log('$name initialized');""",
    )

    private fun bundleAsHtml(project: SandboxProject): String {
        val css = project.files["styles.css"] ?: ""
        val js = project.files["app.js"] ?: project.files["game.js"] ?: ""
        val html = project.files["index.html"] ?: ""
        return html
            .replace("<link rel=\"stylesheet\" href=\"styles.css\">", "<style>$css</style>")
            .replace("<script src=\"app.js\"></script>", "<script>$js</script>")
            .replace("<script src=\"game.js\"></script>", "<script>$js</script>")
    }

    private fun buildCodePrompt(prompt: String, currentCode: String, filename: String, type: ProjectType): String =
        """You are a code assistant working on a ${type.label} project.
Current file ($filename):
```
${currentCode.take(2000)}
```
User request: $prompt
Respond with ONLY the updated code for $filename. No explanation. No markdown fences."""

    // Placeholder for actual AI-driven generation (wired to PrimaryAgent.generate() in production)
    private fun simulateCodeGeneration(prompt: String, currentCode: String, type: ProjectType): String = currentCode
}
