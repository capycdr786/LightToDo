package com.capycdr786.todo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.capycdr786.todo.ui.theme.MinimalisttodoTheme
import java.time.LocalDate
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderCalendarBar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    taskCountsByDate: Map<LocalDate, Int> = emptyMap(),
    onBackupData: () -> Unit = {},
    onRestoreData: () -> Unit = {},
) {
    var showDatePickerDialog by remember { mutableStateOf(value = false) }
    var menuExpanded by remember { mutableStateOf(value = false) }

    val locale = LocalConfiguration.current.locales[0]
    val monthName = remember(selectedDate, locale) {
        selectedDate.month.getDisplayName(TextStyle.FULL, locale)
    }
    val year = selectedDate.year

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "$monthName $year",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            IconButton(
                onClick = { onDateSelected(LocalDate.now()) },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Today,
                    contentDescription = "Return to Today",
                    tint = if (selectedDate == LocalDate.now()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(
                onClick = { showDatePickerDialog = true },
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = "Open Calendar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { menuExpanded = true },
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Backup Data") },
                    onClick = {
                        menuExpanded = false
                        onBackupData()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Restore Data") },
                    onClick = {
                        menuExpanded = false
                        onRestoreData()
                    },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        modifier = modifier,
    )

    if (showDatePickerDialog) {
        SelectDateCalendarDialog(
            selectedDate = selectedDate,
            taskCountsByDate = taskCountsByDate,
            onDateSelected = onDateSelected,
            onDismiss = { showDatePickerDialog = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeaderCalendarBarPreview() {
    MinimalisttodoTheme {
        HeaderCalendarBar(
            selectedDate = LocalDate.now(),
            onDateSelected = {},
            taskCountsByDate = mapOf(
                LocalDate.now() to 2,
                LocalDate.now().plusDays(2) to 5,
            ),
        )
    }
}
