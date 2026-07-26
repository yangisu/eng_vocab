package com.yangi.engvocab.feature.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yangi.engvocab.ui.components.StatusBanner

@Composable
fun EntryEditorScreen(
    state: BookDetailUiState,
    onExpressionChange: (String) -> Unit,
    onMeaningChange: (String) -> Unit,
    onImportantChange: (Boolean) -> Unit,
    onFillMeaning: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
        title = { Text(if (state.editingWordId == null) "단어 직접 추가" else "단어 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.book?.let { Text("저장 위치 · ${it.name}") }
                OutlinedTextField(
                    value = state.editorExpression,
                    onValueChange = onExpressionChange,
                    label = { Text("영어 단어 또는 문구") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.editorMeaning,
                    onValueChange = onMeaningChange,
                    label = { Text("뜻") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.editorImportant, onCheckedChange = onImportantChange)
                    Icon(Icons.Rounded.Star, contentDescription = null)
                    Text("중요 단어")
                }
                OutlinedButton(
                    onClick = onFillMeaning,
                    enabled = !state.editorLoadingMeaning && state.editorExpression.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Text(
                        if (state.editorLoadingMeaning) "뜻 찾는 중…" else "AI로 뜻 채우기",
                        Modifier.padding(start = 8.dp),
                    )
                }
                state.editorError?.let { StatusBanner(it, isError = true) }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !state.editorLoadingMeaning,
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
