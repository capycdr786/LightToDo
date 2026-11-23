package com.capycdr786.todo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capycdr786.todo.ui.theme.DoneBadgeContainerLight
import com.capycdr786.todo.ui.theme.DoneBadgeContentLight
import com.capycdr786.todo.ui.theme.MinimalisttodoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HorizontalDateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    taskCountsByDate: Map<LocalDate, Int> = emptyMap(),
) {
    val today = LocalDate.now()
    val locale = LocalConfiguration.current.locales[0]

    val dates = remember(selectedDate, today) {
        val minDate = if (selectedDate.isBefore(today.minusDays(30))) {
            selectedDate.minusDays(15)
        } else {
            today.minusDays(30)
        }
        val maxDate = if (selectedDate.isAfter(today.plusDays(60))) {
            selectedDate.plusDays(30)
        } else {
            today.plusDays(60)
        }
        val list = mutableListOf<LocalDate>()
        var curr = minDate
        while (!curr.isAfter(maxDate)) {
            list.add(curr)
            curr = curr.plusDays(1)
        }
        list
    }

    val selectedIndex = remember(dates, selectedDate) {
        dates.indexOf(selectedDate).coerceAtLeast(0)
    }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in dates.indices) {
            val targetIndex = (selectedIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
    val headerFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedDate.format(headerFormatter),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = dates,
                key = { it.toString() },
                contentType = { "date_item" },
            ) { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val taskCount = taskCountsByDate[date] ?: 0

                val backgroundColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }

                val contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                Surface(
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onDateSelected(date) },
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor,
                    border = if ((taskCount > 0) && (!isSelected)) {
                        BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f))
                    } else {
                        null
                    },
                    tonalElevation = if (isSelected) 4.dp else 1.dp,
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (taskCount > 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp),
                                shape = CircleShape,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                                } else {
                                    DoneBadgeContainerLight
                                },
                                border = if (!isSelected) BorderStroke(0.5.dp, Color(0xFF86EFAC)) else null,
                            ) {
                                Text(
                                    text = taskCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else DoneBadgeContentLight,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = date.format(dayFormatter).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                ),
                                color = contentColor.copy(alpha = if (isSelected) 0.9f else 0.7f),
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                ),
                                color = contentColor,
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            if (taskCount > 0) {
                                val taskCountText = if (taskCount == 1) "1 task" else "$taskCount tasks"
                                Text(
                                    text = taskCountText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f)
                                    } else {
                                        Color(0xFF16A34A)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (taskCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFF4ADE80) else Color(0xFF22C55E),
                                            shape = CircleShape,
                                        ),
                                )
                            } else if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            shape = CircleShape,
                                        ),
                                )
                            } else {
                                Spacer(modifier = Modifier.size(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HorizontalDateSelectorPreview() {
    MinimalisttodoTheme {
        HorizontalDateSelector(
            selectedDate = LocalDate.now(),
            onDateSelected = {},
            taskCountsByDate = mapOf(
                LocalDate.now() to 3,
                LocalDate.now().plusDays(1) to 1,
            ),
        )
    }
}
