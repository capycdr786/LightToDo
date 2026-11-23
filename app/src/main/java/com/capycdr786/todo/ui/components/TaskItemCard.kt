package com.capycdr786.todo.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capycdr786.todo.data.model.Task
import com.capycdr786.todo.data.model.TaskStatus
import com.capycdr786.todo.ui.theme.MinimalisttodoTheme
import com.capycdr786.todo.ui.theme.getStatusBadgeColors
import java.time.LocalDate

@Composable
fun TaskItemCard(
    task: Task,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onOpenOptions,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenOptions()
                },
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.None,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                Spacer(modifier = Modifier.width(8.dp))

                StatusChip(status = task.status)
            }

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if ((task.status == TaskStatus.CANCELLED) && !task.cancellationReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reason: ${task.cancellationReason}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                    ),
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    status: TaskStatus,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val (containerColor, contentColor) = remember(status, isDark) {
        getStatusBadgeColors(status, isDark)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
    ) {
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            ),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Preview(name = "Light Theme", showBackground = true)
@Composable
fun TaskItemCardPreview() {
    MinimalisttodoTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskItemCard(
                task = Task(
                    id = "1",
                    title = "Design Jetpack Compose UI",
                    description = "Implement M3 components and responsive layouts for tasks.",
                    status = TaskStatus.ONGOING,
                    date = LocalDate.now(),
                    isCompleted = false,
                ),
                onOpenOptions = {},
            )
            TaskItemCard(
                task = Task(
                    id = "2",
                    title = "Write Unit Tests",
                    description = "Ensure ViewModel flows work as expected.",
                    status = TaskStatus.DONE,
                    date = LocalDate.now(),
                    isCompleted = true,
                ),
                onOpenOptions = {},
            )
            TaskItemCard(
                task = Task(
                    id = "3",
                    title = "Review PR",
                    description = "Check pending code changes.",
                    status = TaskStatus.PENDING,
                    date = LocalDate.now(),
                    isCompleted = false,
                ),
                onOpenOptions = {},
            )
            TaskItemCard(
                task = Task(
                    id = "4",
                    title = "Old Feature Request",
                    description = "Cancelled due to design updates.",
                    status = TaskStatus.CANCELLED,
                    cancellationReason = "Requirement changed",
                    date = LocalDate.now(),
                    isCompleted = false,
                ),
                onOpenOptions = {},
            )
        }
    }
}

@Preview(name = "Dark Theme", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TaskItemCardDarkPreview() {
    MinimalisttodoTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskItemCard(
                task = Task(
                    id = "1",
                    title = "Design Jetpack Compose UI",
                    description = "Implement M3 components and responsive layouts for tasks.",
                    status = TaskStatus.ONGOING,
                    date = LocalDate.now(),
                    isCompleted = false,
                ),
                onOpenOptions = {},
            )
            TaskItemCard(
                task = Task(
                    id = "2",
                    title = "Write Unit Tests",
                    description = "Ensure ViewModel flows work as expected.",
                    status = TaskStatus.DONE,
                    date = LocalDate.now(),
                    isCompleted = true,
                ),
                onOpenOptions = {},
            )
            TaskItemCard(
                task = Task(
                    id = "3",
                    title = "Review PR",
                    description = "Check pending code changes.",
                    status = TaskStatus.PENDING,
                    date = LocalDate.now(),
                    isCompleted = false,
                ),
                onOpenOptions = {},
            )
            TaskItemCard(
                task = Task(
                    id = "4",
                    title = "Old Feature Request",
                    description = "Cancelled due to design updates.",
                    status = TaskStatus.CANCELLED,
                    cancellationReason = "Requirement changed",
                    date = LocalDate.now(),
                    isCompleted = false,
                ),
                onOpenOptions = {},
            )
        }
    }
}
