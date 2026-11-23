package com.capycdr786.todo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.capycdr786.todo.ui.theme.DoneBadgeContainerDark
import com.capycdr786.todo.ui.theme.DoneBadgeContainerLight
import com.capycdr786.todo.ui.theme.DoneBadgeContentDark
import com.capycdr786.todo.ui.theme.DoneBadgeContentLight
import com.capycdr786.todo.ui.theme.MinimalisttodoTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.WeekFields

@Composable
fun SelectDateCalendarDialog(
    selectedDate: LocalDate,
    taskCountsByDate: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentYearMonth by remember(selectedDate) {
        mutableStateOf(YearMonth.from(selectedDate))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                val locale = LocalConfiguration.current.locales[0]
                val monthYearFormatter = remember(locale) {
                    DateTimeFormatter.ofPattern("MMMM yyyy", locale)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentYearMonth.format(monthYearFormatter),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
                val daysOfWeek = remember(firstDayOfWeek) {
                    DayOfWeek.entries.let { days ->
                        val shift = days.indexOf(firstDayOfWeek)
                        days.drop(shift) + days.take(shift)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    daysOfWeek.forEach { dayOfWeek ->
                        Text(
                            text = dayOfWeek.getDisplayName(JavaTextStyle.SHORT, locale),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val firstOfMonth = currentYearMonth.atDay(1)
                val daysInMonth = currentYearMonth.lengthOfMonth()
                val firstDayOfWeekInMonth = firstOfMonth.dayOfWeek
                val emptySlots = (((firstDayOfWeekInMonth.value - firstDayOfWeek.value) % 7) + 7) % 7
                val totalCells = emptySlots + daysInMonth
                val numRows = (totalCells + 6) / 7

                val isDark = isSystemInDarkTheme()
                val greenBg = if (isDark) DoneBadgeContainerDark else DoneBadgeContainerLight
                val greenBorder = Color(0xFF22C55E)
                val greenText = if (isDark) DoneBadgeContentDark else DoneBadgeContentLight
                val greenBadgeBg = if (isDark) Color(0xFF22C55E) else Color(0xFF15803D)
                val greenBadgeText = if (isDark) Color(0xFF052E16) else Color.White

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (rowIndex in 0 until numRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            for (colIndex in 0..6) {
                                val cellIndex = (rowIndex * 7) + colIndex
                                val dayNumber = (cellIndex - emptySlots) + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val date = currentYearMonth.atDay(dayNumber)
                                    val taskCount = taskCountsByDate[date] ?: 0
                                    val isSelected = (date == selectedDate)
                                    val isToday = (date == LocalDate.now())
                                    val hasTasks = taskCount > 0

                                    val containerColor = when {
                                        hasTasks && isSelected -> Color(0xFF15803D)
                                        hasTasks -> greenBg
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else -> Color.Transparent
                                    }

                                    val borderStroke = when {
                                        hasTasks -> BorderStroke(1.5.dp, greenBorder)
                                        isToday && !isSelected -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        else -> null
                                    }

                                    val textColor = when {
                                        hasTasks && isSelected -> Color.White
                                        hasTasks -> greenText
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onDateSelected(date)
                                                onDismiss()
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = containerColor,
                                        border = borderStroke,
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (hasTasks) {
                                                Surface(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(top = 2.dp, end = 2.dp),
                                                    shape = CircleShape,
                                                    color = if (isSelected) Color.White else greenBadgeBg,
                                                ) {
                                                    Text(
                                                        text = taskCount.toString(),
                                                        style = TextStyle(
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        ),
                                                        color = if (isSelected) Color(0xFF15803D) else greenBadgeText,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                                    )
                                                }
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                            ) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected || hasTasks || isToday) {
                                                            FontWeight.Bold
                                                        } else {
                                                            FontWeight.Normal
                                                        },
                                                        fontSize = 14.sp,
                                                    ),
                                                    color = textColor,
                                                )

                                                if (hasTasks) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .background(
                                                                color = if (isSelected) Color.White else greenBorder,
                                                                shape = CircleShape,
                                                            ),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            val today = LocalDate.now()
                            currentYearMonth = YearMonth.from(today)
                            onDateSelected(today)
                            onDismiss()
                        },
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectDateCalendarDialogPreview() {
    MinimalisttodoTheme {
        SelectDateCalendarDialog(
            selectedDate = LocalDate.now(),
            taskCountsByDate = mapOf(
                LocalDate.now() to 3,
                LocalDate.now().plusDays(2) to 1,
                LocalDate.now().minusDays(5) to 4,
            ),
            onDateSelected = {},
            onDismiss = {},
        )
    }
}
