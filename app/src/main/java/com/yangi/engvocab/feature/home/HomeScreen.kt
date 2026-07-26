package com.yangi.engvocab.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangi.engvocab.ui.components.ActionCard
import com.yangi.engvocab.ui.components.AppTopBar
import com.yangi.engvocab.ui.components.EmptyState
import com.yangi.engvocab.ui.components.MetricCard
import com.yangi.engvocab.ui.components.SectionHeader

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onStartDue: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onPhotoImport: () -> Unit,
    onBooks: () -> Unit,
    onImportant: () -> Unit,
    onWrong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state,
        onStartDue,
        onOpenBook,
        onPhotoImport,
        onBooks,
        onImportant,
        onWrong,
        modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartDue: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onPhotoImport: () -> Unit,
    onBooks: () -> Unit,
    onImportant: () -> Unit,
    onWrong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = "오늘의 학습") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.loading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 8.dp,
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        "오늘도 한 걸음,\n꾸준히 쌓아볼까요?",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
                item {
                    ReviewHeroCard(
                        count = state.dueCount,
                        onStart = onStartDue,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MetricCard(
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            label = "미학습 단어",
                            value = "${state.unstudiedCount}개",
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        MetricCard(
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            label = "최근 단어장",
                            value = "${state.recentBooks.size}개",
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                item { SectionHeader("빠른 실행") }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ActionCard(
                            icon = Icons.Rounded.CameraAlt,
                            title = "사진으로 만들기",
                            subtitle = "교재를 찍어 빠르게 추가",
                            onClick = onPhotoImport,
                            modifier = Modifier.weight(1f),
                        )
                        ActionCard(
                            icon = Icons.Rounded.Add,
                            title = "직접 추가",
                            subtitle = "단어장을 만들고 입력",
                            onClick = onBooks,
                            modifier = Modifier.weight(1f),
                            accentColor = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ActionCard(
                            icon = Icons.Rounded.Star,
                            title = "중요 단어",
                            subtitle = "표시한 단어만 모아보기",
                            onClick = onImportant,
                            modifier = Modifier.weight(1f),
                            accentColor = MaterialTheme.colorScheme.secondary,
                        )
                        ActionCard(
                            icon = Icons.Rounded.Favorite,
                            title = "틀린 단어",
                            subtitle = "헷갈린 단어 다시 보기",
                            onClick = onWrong,
                            modifier = Modifier.weight(1f),
                            accentColor = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                item { SectionHeader("최근 단어장", actionLabel = "전체 보기", onAction = onBooks) }
                if (state.recentBooks.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.AutoStories,
                            title = "아직 단어장이 없어요",
                            body = "첫 단어장을 만들고 학습을 시작해 보세요.",
                        )
                    }
                } else {
                    items(state.recentBooks, key = { it.id }) { book ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenBook(book.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    book.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "단어장 열기",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewHeroCard(
    count: Int,
    onStart: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Rounded.Schedule, contentDescription = null)
            Text("오늘 복습", style = MaterialTheme.typography.titleMedium)
            Text(
                if (count > 0) "${count}개의 단어가 기다려요" else "오늘 복습을 모두 마쳤어요",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = onStart,
                enabled = count > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.AutoStories, contentDescription = null)
                Text(if (count > 0) "복습 시작" else "복습 완료", Modifier.padding(start = 8.dp))
            }
        }
    }
}
