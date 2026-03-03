package com.openclaw.android.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.openclaw.android.skill.BuiltinSkills
import com.openclaw.android.skill.SkillCategory
import com.openclaw.android.skill.SkillManifest
import com.openclaw.android.ui.theme.*
import com.openclaw.android.ui.viewmodel.SkillsViewModel

@Composable
fun SkillsScreen(viewModel: SkillsViewModel = hiltViewModel()) {
    val activeSkills by viewModel.activeSkills.collectAsState()
    val skillsByCategory = BuiltinSkills.ALL.groupBy { it.category }
    var showBuilder by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("SKILL ENGINE", color = ClawGreen, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
            OutlinedButton(
                onClick = { showBuilder = true },
                border = androidx.compose.foundation.BorderStroke(1.dp, ClawPurple),
                shape = RoundedCornerShape(4.dp),
            ) { Text("+ BUILD SKILL", color = ClawPurple, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
        Spacer(Modifier.height(4.dp))
        Text("${activeSkills.size} skills active", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            skillsByCategory.forEach { (category, skills) ->
                item {
                    Text(category.name.replace("_", " "), color = ClawGreen.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                items(skills) { skill ->
                    SkillCard(skill = skill, isActive = activeSkills.any { it.id == skill.id }, onToggle = { viewModel.toggleSkill(skill.id) })
                }
            }
        }
    }

    if (showBuilder) {
        SkillBuilderDialog(onDismiss = { showBuilder = false }, onBuild = { name, desc, prompt ->
            viewModel.buildCustomSkill(name, desc, prompt); showBuilder = false
        })
    }
}

@Composable
fun SkillCard(skill: SkillManifest, isActive: Boolean, onToggle: () -> Unit) {
    Surface(
        color = if (isActive) SurfaceDark else BgDark,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, if (isActive) ClawGreen.copy(alpha = 0.3f) else BorderDark, RoundedCornerShape(6.dp))
            .clickable { onToggle() },
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(skill.name, color = TextPrimary, fontSize = 14.sp)
                Text(skill.description, color = TextMuted, fontSize = 11.sp)
            }
            Switch(checked = isActive, onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = ClawGreen, checkedTrackColor = ClawGreen.copy(alpha = 0.3f)))
        }
    }
}

@Composable
fun SkillBuilderDialog(onDismiss: () -> Unit, onBuild: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("BUILD NEW SKILL", color = ClawGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Skill Name", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ClawGreen, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ClawGreen, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("System Prompt Extension", color = TextMuted) }, minLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ClawGreen, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank() && prompt.isNotBlank()) onBuild(name, description, prompt) }) { Text("BUILD", color = ClawGreen, fontFamily = FontFamily.Monospace) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextMuted) } }
    )
}
