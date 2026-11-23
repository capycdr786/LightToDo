package com.capycdr786.todo.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capycdr786.todo.data.model.Task
import com.capycdr786.todo.ui.theme.DoneBadgeContainerDark
import com.capycdr786.todo.ui.theme.DoneBadgeContainerLight
import com.capycdr786.todo.ui.theme.DoneBadgeContentDark
import com.capycdr786.todo.ui.theme.DoneBadgeContentLight
import com.capycdr786.todo.ui.theme.MinimalisttodoTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

@Composable
fun DeleteRecurringTaskDialog(
    task: Task,
    allTasks: List<Task>,
    onConfirmDeleteAll: (repeatGroupId: String) -> Unit,
    onConfirmDeleteRange: (repeatGroupId: String, startDate: LocalDate, endDate: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val repeatGroupId = task.repeatGroupId ?: run {
        onDismiss()
        return
    }

    val recurringTasks = remember(allTasks, repeatGroupId) {
        allTasks.filter { it.repeatGroupId == repeatGroupId }
    }

    val recurringDates = remember(recurringTasks) {
        recurringTasks.asSequence().map { t -> t.date }.distinct().sorted().toList()
    }

    val recurringDatesSet = remember(recurringDates) {
        recurringDates.toSet()
    }

    val earliestDate = remember(recurringDates, task) {
        recurringDates.firstOrNull() ?: task.date
    }

    val latestDate = remember(recurringDates, task) {
        recurringDates.lastOrNull() ?: task.date
    }

    var deleteAll by remember { mutableStateOf(value = true) }
    var startDate by remember(recurringDates) { mutableStateOf(earliestDate) }
    var endDate by remember(recurringDates) { mutableStateOf(latestDate) }
    var isSelectingEnd by remember { mutableStateOf(value = false) }

    var displayedYearMonth by remember(startDate) {
        mutableStateOf(YearMonth.from(startDate))
    }

    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    val totalCount = recurringTasks.size

    val matchingTasksCount = remember(recurringTasks, startDate, endDate, deleteAll) {
        if (deleteAll) {
            recurringTasks.size
        } else {
            recurringTasks.count { t -> !t.date.isBefore(startDate) && !t.date.isAfter(endDate) }
        }
    }

    val onDateClicked: (LocalDate) -> Unit = { clickedDate ->
        if (deleteAll) {
            deleteAll = false
            startDate = clickedDate
            endDate = clickedDate
            isSelectingEnd = true
        } else {
            if (isSelectingEnd) {
                if (clickedDate >= startDate) {
                    endDate = clickedDate
                    isSelectingEnd = false
                } else {
                    endDate = startDate
                    startDate = clickedDate
                    isSelectingEnd = false
                }
            } else {
                startDate = clickedDate
                endDate = clickedDate
                isSelectingEnd = true
            }
        }
    }

    val deleteButtonText = remember(deleteAll, matchingTasksCount, totalCount) {
        if (deleteAll || (matchingTasksCount == totalCount)) {
            if (totalCount == 1) "Delete 1 Selected Task" else "Delete All $totalCount Selected Tasks"
        } else {
            if (matchingTasksCount == 1) "Delete 1 Selected Task" else "Delete $matchingTasksCount Selected Tasks"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteSweep,
                contentDescription = "Delete Recurring Task",
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = "Delete Recurring Task",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Recurring task instances are marked in green on the calendar. Tap dates to select range:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                CalendarMonthView(
                    displayedYearMonth = displayedYearMonth,
                    startDate = startDate,
                    endDate = endDate,
                    recurringDatesSet = recurringDatesSet,
                    onDateClicked = onDateClicked,
                    onPrevMonthClicked = { displayedYearMonth = displayedYearMonth.minusMonths(1) },
                    onNextMonthClicked = { displayedYearMonth = displayedYearMonth.plusMonths(1) },
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E)),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Recurring",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Selected Range",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Selected Range:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (startDate == endDate) {
                                startDate.format(dateFormatter)
                            } else {
                                "${startDate.format(dateFormatter)} – ${endDate.format(dateFormatter)}"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            deleteAll = !deleteAll
                            if (deleteAll) {
                                startDate = earliestDate
                                endDate = latestDate
                                isSelectingEnd = false
                            }
                        }
                        .padding(vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = deleteAll,
                        onCheckedChange = { checked ->
                            deleteAll = checked
                            if (checked) {
                                startDate = earliestDate
                                endDate = latestDate
                                isSelectingEnd = false
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.error,
                            checkmarkColor = MaterialTheme.colorScheme.onError,
                        ),
                    )
                    Text(
                        text = "Select All / Delete All ($totalCount instances)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (deleteAll || (matchingTasksCount == totalCount)) {
                            "All $totalCount recurring instances across all dates will be deleted."
                        } else {
                            "$matchingTasksCount of $totalCount recurring instance(s) in selected range will be deleted."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (deleteAll || (matchingTasksCount == totalCount)) {
                        onConfirmDeleteAll(repeatGroupId)
                    } else {
                        onConfirmDeleteRange(repeatGroupId, startDate, endDate)
                    }
                    onDismiss()
                },
                enabled = matchingTasksCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(deleteButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun CalendarMonthView(
    displayedYearMonth: YearMonth,
    startDate: LocalDate,
    endDate: LocalDate,
    recurringDatesSet: Set<LocalDate>,
    onDateClicked: (LocalDate) -> Unit,
    onPrevMonthClicked: () -> Unit,
    onNextMonthClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val monthYearFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevMonthClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Text(
                    text = displayedYearMonth.format(monthYearFormatter),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                IconButton(onClick = onNextMonthClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
            val daysOfWeek = remember(firstDayOfWeek) {
                DayOfWeek.entries.let { days ->
                    val shift = days.indexOf(firstDayOfWeek)
                    days.drop(shift) + days.take(shift)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                daysOfWeek.forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val firstOfMonth = displayedYearMonth.atDay(1)
            val daysInMonth = displayedYearMonth.lengthOfMonth()
            val firstDayOfWeekInMonth = firstOfMonth.dayOfWeek
            val emptySlots = (((firstDayOfWeekInMonth.value - firstDayOfWeek.value) % 7) + 7) % 7

            val totalGridCells = emptySlots + daysInMonth
            val numRows = (totalGridCells + 6) / 7

            for (row in 0 until numRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = (row * 7) + col
                        val dayNumber = (cellIndex - emptySlots) + 1

                        if (dayNumber in 1..daysInMonth) {
                            val date = displayedYearMonth.atDay(dayNumber)
                            val isRecurring = recurringDatesSet.contains(date)
                            val isInRange = !date.isBefore(startDate) && !date.isAfter(endDate)
                            val isStart = date == startDate
                            val isEnd = date == endDate
                            val isSingleDayRange = isStart && isEnd

                            CalendarDayCell(
                                date = date,
                                dayNumber = dayNumber,
                                isRecurring = isRecurring,
                                isInRange = isInRange,
                                isStart = isStart,
                                isEnd = isEnd,
                                isSingleDayRange = isSingleDayRange,
                                onDateClicked = onDateClicked,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    dayNumber: Int,
    isRecurring: Boolean,
    isInRange: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    isSingleDayRange: Boolean,
    onDateClicked: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()

    val rangeStripColor = if (isStart || isEnd) {
        MaterialTheme.colorScheme.error
    } else if (isInRange) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    } else {
        Color.Transparent
    }

    val textColor = when {
        isStart || isEnd -> MaterialTheme.colorScheme.onError
        isInRange -> MaterialTheme.colorScheme.onErrorContainer
        isRecurring -> if (isDarkTheme) DoneBadgeContentDark else DoneBadgeContentLight
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderStroke = when {
        isStart || isEnd -> null
        isRecurring && !isInRange -> BorderStroke(
            1.dp,
            if (isDarkTheme) Color(0xFF22C55E) else Color(0xFF16A34A),
        )
        else -> null
    }

    val containerBackground = when {
        isRecurring && !isInRange -> if (isDarkTheme) DoneBadgeContainerDark else DoneBadgeContainerLight
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .clickable { onDateClicked(date) },
        contentAlignment = Alignment.Center,
    ) {
        if (isInRange) {
            val rangeShape = when {
                isSingleDayRange -> CircleShape
                isStart -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                isEnd -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                else -> RoundedCornerShape(0.dp)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 3.dp)
                    .clip(rangeShape)
                    .background(rangeStripColor),
            )
        }

        if (isRecurring && !isInRange) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(containerBackground)
                    .then(
                        if (borderStroke != null) Modifier.border(borderStroke, CircleShape) else Modifier,
                    ),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isRecurring || isStart || isEnd) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                ),
                color = textColor,
            )

            if (isRecurring) {
                Spacer(modifier = Modifier.height(2.dp))
                val dotColor = when {
                    isStart || isEnd -> MaterialTheme.colorScheme.onError
                    isInRange -> Color(0xFF22C55E)
                    else -> if (isDarkTheme) Color(0xFF4ADE80) else Color(0xFF16A34A)
                }
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeleteRecurringTaskDialogPreview() {
    MinimalisttodoTheme {
        DeleteRecurringTaskDialog(
            task = Task(
                id = "1",
                title = "Daily Gym Session",
                date = LocalDate.now(),
                repeatGroupId = "group_1",
            ),
            allTasks = listOf(
                Task(id = "1", title = "Daily Gym Session", date = LocalDate.now(), repeatGroupId = "group_1"),
                Task(id = "2", title = "Daily Gym Session", date = LocalDate.now().plusDays(1), repeatGroupId = "group_1"),
                Task(id = "3", title = "Daily Gym Session", date = LocalDate.now().plusDays(2), repeatGroupId = "group_1"),
                Task(id = "4", title = "Daily Gym Session", date = LocalDate.now().plusDays(3), repeatGroupId = "group_1"),
            ),
            onConfirmDeleteAll = {},
            onConfirmDeleteRange = { _, _, _ -> },
        ) {}
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeleteRecurringTaskDialogDarkPreview() {
    MinimalisttodoTheme(darkTheme = true) {
        DeleteRecurringTaskDialog(
            task = Task(
                id = "1",
                title = "Daily Gym Session",
                date = LocalDate.now(),
                repeatGroupId = "group_1",
            ),
            allTasks = listOf(
                Task(id = "1", title = "Daily Gym Session", date = LocalDate.now(), repeatGroupId = "group_1"),
                Task(id = "2", title = "Daily Gym Session", date = LocalDate.now().plusDays(1), repeatGroupId = "group_1"),
                Task(id = "3", title = "Daily Gym Session", date = LocalDate.now().plusDays(2), repeatGroupId = "group_1"),
            ),
            onConfirmDeleteAll = {},
            onConfirmDeleteRange = { _, _, _ -> },
        ) {}
    }
}
