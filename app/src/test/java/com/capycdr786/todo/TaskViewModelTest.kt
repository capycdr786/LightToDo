package com.capycdr786.todo

import com.capycdr786.todo.data.local.AppDatabase
import com.capycdr786.todo.data.model.Task
import com.capycdr786.todo.data.model.TaskStatus
import com.capycdr786.todo.data.repository.TaskRepository
import com.capycdr786.todo.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

class FakeTaskRepository : TaskRepository {
    private val tasksStream = MutableStateFlow<List<Task>>(emptyList())

    override fun getAllTasks(): Flow<List<Task>> = tasksStream

    override fun getTasksByDate(date: LocalDate): Flow<List<Task>> =
        tasksStream.map { list -> list.filter { task -> task.date == date } }

    override fun getTaskCountsByDate(): Flow<Map<LocalDate, Int>> =
        tasksStream.map { list -> list.groupingBy { it.date }.eachCount() }

    override suspend fun getTaskById(id: String): Task? =
        tasksStream.value.find { task -> task.id == id }

    override suspend fun addTask(task: Task) {
        tasksStream.value += task
    }

    override suspend fun insertTasks(tasks: List<Task>) {
        tasksStream.value += tasks
    }

    override suspend fun updateTask(task: Task) {
        tasksStream.value = tasksStream.value.map { if (it.id == task.id) task else it }
    }

    override suspend fun deleteTask(task: Task) {
        tasksStream.value = tasksStream.value.filterNot { it.id == task.id }
    }

    override suspend fun deleteTaskById(id: String) {
        tasksStream.value = tasksStream.value.filterNot { it.id == id }
    }

    override suspend fun deleteTasksInRange(startDate: LocalDate, endDate: LocalDate): Int {
        val toDelete = tasksStream.value.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
        tasksStream.value = tasksStream.value.filterNot { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
        return toDelete.size
    }

    override suspend fun deleteTasksByRepeatGroupId(repeatGroupId: String): Int {
        val toDelete = tasksStream.value.filter { it.repeatGroupId == repeatGroupId }
        tasksStream.value = tasksStream.value.filterNot { it.repeatGroupId == repeatGroupId }
        return toDelete.size
    }

    override suspend fun deleteRepeatGroupTasksInRange(
        repeatGroupId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Int {
        val toDelete = tasksStream.value.filter { task ->
            (task.repeatGroupId == repeatGroupId) &&
                (!task.date.isBefore(startDate)) &&
                (!task.date.isAfter(endDate))
        }
        tasksStream.value = tasksStream.value.filterNot { task ->
            (task.repeatGroupId == repeatGroupId) &&
                (!task.date.isBefore(startDate)) &&
                (!task.date.isAfter(endDate))
        }
        return toDelete.size
    }

    override suspend fun toggleTaskCompletion(taskId: String) {
        tasksStream.value = tasksStream.value.map { task ->
            if (task.id == taskId) {
                val newIsCompleted = !task.isCompleted
                val newStatus = if (newIsCompleted) TaskStatus.DONE else TaskStatus.PENDING
                task.copy(isCompleted = newIsCompleted, status = newStatus)
            } else {
                task
            }
        }
    }

    override suspend fun rescheduleTask(taskId: String, newDate: LocalDate) {
        tasksStream.value = tasksStream.value.map { task ->
            if (task.id == taskId) {
                task.copy(date = newDate, status = TaskStatus.PENDING, isCompleted = false)
            } else {
                task
            }
        }
    }

    override suspend fun setTaskStatus(taskId: String, status: TaskStatus, cancellationReason: String?) {
        tasksStream.value = tasksStream.value.map { task ->
            if (task.id == taskId) {
                val isCompleted = (status == TaskStatus.DONE)
                val reason = if (status == TaskStatus.CANCELLED) cancellationReason else null
                task.copy(status = status, isCompleted = isCompleted, cancellationReason = reason)
            } else {
                task
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTaskRepository
    private lateinit var viewModel: TaskViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTaskRepository()
        viewModel = TaskViewModel(repository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultSelectedDate_isToday() {
        assertEquals(LocalDate.now(), viewModel.selectedDate.value)
    }

    @Test
    fun taskStatusEnum_containsAllRequiredStatuses() {
        val statuses = TaskStatus.entries.map { it.displayName }
        assertEquals(4, TaskStatus.entries.size)
        assertTrue(statuses.contains("Pending"))
        assertTrue(statuses.contains("Ongoing"))
        assertTrue(statuses.contains("Done"))
        assertTrue(statuses.contains("Cancelled"))
    }

    @Test
    fun setSelectedDate_updatesDate() {
        val newDate = LocalDate.now().plusDays(2)
        viewModel.setSelectedDate(newDate)
        assertEquals(newDate, viewModel.selectedDate.value)
    }

    @Test
    fun addTask_defaultStatusIsPending_andDefaultDateIsToday() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.addTask(title = "Default Task")
        advanceUntilIdle()

        val todayTasks = viewModel.tasksForSelectedDate.value
        assertEquals(1, todayTasks.size)
        val added = todayTasks[0]
        assertEquals("Default Task", added.title)
        assertEquals(LocalDate.now(), added.date)
        assertEquals(TaskStatus.PENDING, added.status)
        assertFalse(added.isCompleted)
    }

    @Test
    fun addTask_usesSelectedDate_andPendingStatus() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val futureDate = LocalDate.now().plusDays(5)
        viewModel.setSelectedDate(futureDate)

        viewModel.addTask(title = "Future Task", description = "Details")
        advanceUntilIdle()

        val tasks = viewModel.tasksForSelectedDate.value
        assertEquals(1, tasks.size)
        val added = tasks[0]
        assertEquals("Future Task", added.title)
        assertEquals("Details", added.description)
        assertEquals(futureDate, added.date)
        assertEquals(TaskStatus.PENDING, added.status)
        assertFalse(added.isCompleted)
    }

    @Test
    fun addTask_insertsTaskAndUpdatesSelectedDate() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        viewModel.addTask(title = "Today Task", description = "Test", date = today, status = TaskStatus.PENDING)
        advanceUntilIdle()

        val todayTasks = viewModel.tasksForSelectedDate.value
        assertEquals(1, todayTasks.size)
        assertEquals("Today Task", todayTasks[0].title)

        viewModel.addTask(title = "Tomorrow Task", description = "Test", date = tomorrow, status = TaskStatus.ONGOING)
        advanceUntilIdle()

        assertEquals(tomorrow, viewModel.selectedDate.value)
        val tomorrowTasks = viewModel.tasksForSelectedDate.value
        assertEquals(1, tomorrowTasks.size)
        assertEquals("Tomorrow Task", tomorrowTasks[0].title)
        assertEquals(TaskStatus.PENDING, tomorrowTasks[0].status)
    }

    @Test
    fun addTask_blankTitle_doesNotInsert() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.addTask(title = "   ", description = "Test", date = LocalDate.now(), status = TaskStatus.PENDING)
        advanceUntilIdle()

        val tasks = viewModel.tasksForSelectedDate.value
        assertEquals(0, tasks.size)
    }

    @Test
    fun addRepeatingTask_createsTasksForAllowedDaysWithSharedRepeatGroupId() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        var startDate = LocalDate.now()
        while (startDate.dayOfWeek != DayOfWeek.MONDAY) {
            startDate = startDate.plusDays(1)
        }
        val endDate = startDate.plusDays(6)

        val daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        viewModel.addRepeatingTask(
            title = "Gym Session",
            description = "Workout",
            startDate = startDate,
            endDate = endDate,
            daysOfWeek = daysOfWeek,
        )
        advanceUntilIdle()

        val allTasks = viewModel.allTasks.value
        assertEquals(3, allTasks.size)

        val firstTask = allTasks[0]
        assertNotNull(firstTask.repeatGroupId)
        assertTrue(firstTask.repeatGroupId!!.isNotBlank())

        allTasks.forEach { task ->
            assertEquals("Gym Session", task.title)
            assertEquals("Workout", task.description)
            assertEquals(TaskStatus.PENDING, task.status)
            assertFalse(task.isCompleted)
            assertEquals(firstTask.repeatGroupId, task.repeatGroupId)
            assertTrue(daysOfWeek.contains(task.date.dayOfWeek))
        }
    }

    @Test
    fun toggleTaskCompletion_togglesState() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val task = Task(id = "1", title = "Test", isCompleted = false, status = TaskStatus.PENDING)
        repository.addTask(task)
        advanceUntilIdle()

        viewModel.toggleTaskCompletion("1")
        advanceUntilIdle()

        var updatedTask = repository.getTaskById("1")
        assertNotNull(updatedTask)
        assertTrue(updatedTask!!.isCompleted)
        assertEquals(TaskStatus.DONE, updatedTask.status)

        viewModel.toggleTaskCompletion("1")
        advanceUntilIdle()

        updatedTask = repository.getTaskById("1")
        assertNotNull(updatedTask)
        assertFalse(updatedTask!!.isCompleted)
        assertEquals(TaskStatus.PENDING, updatedTask.status)
    }

    @Test
    fun rescheduleTask_updatesDateAndResetsStatusToPending() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val today = LocalDate.now()
        val nextWeek = today.plusDays(7)
        val task = Task(id = "1", title = "Test", date = today, status = TaskStatus.ONGOING)
        repository.addTask(task)
        advanceUntilIdle()

        viewModel.rescheduleTask("1", nextWeek)
        advanceUntilIdle()

        assertEquals(nextWeek, viewModel.selectedDate.value)
        val updatedTask = repository.getTaskById("1")
        assertEquals(nextWeek, updatedTask?.date)
        assertEquals(TaskStatus.PENDING, updatedTask?.status)
        assertFalse(updatedTask!!.isCompleted)
    }

    @Test
    fun setTaskStatus_updatesStatusAndCompletion() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val task = Task(id = "1", title = "Test", status = TaskStatus.PENDING)
        repository.addTask(task)
        advanceUntilIdle()

        viewModel.setTaskStatus("1", TaskStatus.ONGOING)
        advanceUntilIdle()

        var updatedTask = repository.getTaskById("1")
        assertEquals(TaskStatus.ONGOING, updatedTask?.status)
        assertFalse(updatedTask!!.isCompleted)

        viewModel.setTaskStatus("1", TaskStatus.DONE)
        advanceUntilIdle()

        updatedTask = repository.getTaskById("1")
        assertEquals(TaskStatus.DONE, updatedTask?.status)
        assertTrue(updatedTask!!.isCompleted)
    }

    @Test
    fun setTaskStatus_cancelled_storesCancellationReason() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val task = Task(id = "1", title = "Test Task", status = TaskStatus.PENDING)
        repository.addTask(task)
        advanceUntilIdle()

        viewModel.setTaskStatus("1", TaskStatus.CANCELLED, "Not needed anymore")
        advanceUntilIdle()

        val updatedTask = repository.getTaskById("1")
        assertNotNull(updatedTask)
        assertEquals(TaskStatus.CANCELLED, updatedTask?.status)
        assertEquals("Not needed anymore", updatedTask?.cancellationReason)
        assertFalse(updatedTask!!.isCompleted)
    }

    @Test
    fun updateTask_updatesTaskAndSelectedDate() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val task = Task(id = "1", title = "Original Title", date = today, status = TaskStatus.ONGOING)
        repository.addTask(task)
        advanceUntilIdle()

        val updatedTask = task.copy(title = "Updated Title", date = tomorrow)
        viewModel.updateTask(updatedTask)
        advanceUntilIdle()

        assertEquals(tomorrow, viewModel.selectedDate.value)
        val fetched = repository.getTaskById("1")
        assertNotNull(fetched)
        assertEquals("Updated Title", fetched!!.title)
        assertEquals(tomorrow, fetched.date)
        assertEquals(TaskStatus.PENDING, fetched.status)
        assertFalse(fetched.isCompleted)
    }

    @Test
    fun updateTask_onlyTitleAndDescription_preservesDateStatusAndCancellationReason() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val date = LocalDate.now().plusDays(3)
        val task = Task(
            id = "1",
            title = "Original Title",
            description = "Original Desc",
            date = date,
            status = TaskStatus.CANCELLED,
            cancellationReason = "Duplicate",
            isCompleted = false,
        )
        repository.addTask(task)
        advanceUntilIdle()

        val editedTask = task.copy(title = "New Title", description = "New Desc")
        viewModel.updateTask(editedTask)
        advanceUntilIdle()

        val fetched = repository.getTaskById("1")
        assertNotNull(fetched)
        assertEquals("New Title", fetched!!.title)
        assertEquals("New Desc", fetched.description)
        assertEquals(date, fetched.date)
        assertEquals(TaskStatus.CANCELLED, fetched.status)
        assertEquals("Duplicate", fetched.cancellationReason)
        assertFalse(fetched.isCompleted)
    }

    @Test
    fun deleteTask_removesTask() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val task = Task(id = "1", title = "To Delete")
        repository.addTask(task)
        advanceUntilIdle()

        viewModel.deleteTask("1")
        advanceUntilIdle()

        val deletedTask = repository.getTaskById("1")
        assertNull(deletedTask)
    }

    @Test
    fun deleteTask_byTaskObject_removesTask() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val task = Task(id = "2", title = "To Delete Object")
        repository.addTask(task)
        advanceUntilIdle()

        viewModel.deleteTask(task)
        advanceUntilIdle()

        val deletedTask = repository.getTaskById("2")
        assertNull(deletedTask)
    }

    @Test
    fun deleteTasksInRange_deletesOnlyTasksInDateRange() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val today = LocalDate.now()
        val t1 = Task(id = "1", title = "Task Day 0", date = today)
        val t2 = Task(id = "2", title = "Task Day 2", date = today.plusDays(2))
        val t3 = Task(id = "3", title = "Task Day 4", date = today.plusDays(4))
        val t4 = Task(id = "4", title = "Task Day 10", date = today.plusDays(10))

        repository.insertTasks(listOf(t1, t2, t3, t4))
        advanceUntilIdle()

        assertEquals(4, viewModel.allTasks.value.size)

        viewModel.deleteTasksInRange(today.plusDays(1), today.plusDays(5))
        advanceUntilIdle()

        val remaining = viewModel.allTasks.value
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.id == "1" })
        assertTrue(remaining.any { it.id == "4" })
        assertFalse(remaining.any { it.id == "2" })
        assertFalse(remaining.any { it.id == "3" })
    }

    @Test
    fun deleteTasksByRepeatGroupId_deletesAllTasksInGroup() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val groupId = "group_123"
        val t1 = Task(id = "1", title = "Rec 1", date = LocalDate.now(), repeatGroupId = groupId)
        val t2 = Task(id = "2", title = "Rec 2", date = LocalDate.now().plusDays(1), repeatGroupId = groupId)
        val t3 = Task(id = "3", title = "Single Task", date = LocalDate.now().plusDays(2), repeatGroupId = null)

        repository.insertTasks(listOf(t1, t2, t3))
        advanceUntilIdle()

        assertEquals(3, viewModel.allTasks.value.size)

        viewModel.deleteTasksByRepeatGroupId(groupId)
        advanceUntilIdle()

        val remaining = viewModel.allTasks.value
        assertEquals(1, remaining.size)
        assertEquals("3", remaining[0].id)
    }

    @Test
    fun deleteRepeatGroupTasks_deletesAllTasksInGroup() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val groupId = "group_456"
        val t1 = Task(id = "1", title = "Rec 1", date = LocalDate.now(), repeatGroupId = groupId)
        val t2 = Task(id = "2", title = "Rec 2", date = LocalDate.now().plusDays(1), repeatGroupId = groupId)
        val t3 = Task(id = "3", title = "Single Task", date = LocalDate.now().plusDays(2), repeatGroupId = null)

        repository.insertTasks(listOf(t1, t2, t3))
        advanceUntilIdle()

        assertEquals(3, viewModel.allTasks.value.size)

        viewModel.deleteRepeatGroupTasks(groupId)
        advanceUntilIdle()

        val remaining = viewModel.allTasks.value
        assertEquals(1, remaining.size)
        assertEquals("3", remaining[0].id)
    }

    @Test
    fun deleteRepeatGroupTasksInRange_deletesOnlyGroupTasksInDateRange() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val groupId = "group_123"
        val today = LocalDate.now()
        val t1 = Task(id = "1", title = "Rec Day 0", date = today, repeatGroupId = groupId)
        val t2 = Task(id = "2", title = "Rec Day 2", date = today.plusDays(2), repeatGroupId = groupId)
        val t3 = Task(id = "3", title = "Rec Day 4", date = today.plusDays(4), repeatGroupId = groupId)
        val t4 = Task(id = "4", title = "Rec Day 10", date = today.plusDays(10), repeatGroupId = groupId)
        val t5 = Task(id = "5", title = "Other Group Day 2", date = today.plusDays(2), repeatGroupId = "other_group")
        val t6 = Task(id = "6", title = "Non-repeating Day 2", date = today.plusDays(2), repeatGroupId = null)

        repository.insertTasks(listOf(t1, t2, t3, t4, t5, t6))
        advanceUntilIdle()

        assertEquals(6, viewModel.allTasks.value.size)

        viewModel.deleteRepeatGroupTasksInRange(groupId, today.plusDays(1), today.plusDays(5))
        advanceUntilIdle()

        val remaining = viewModel.allTasks.value
        assertEquals(4, remaining.size)
        assertTrue(remaining.any { it.id == "1" })
        assertTrue(remaining.any { it.id == "4" })
        assertTrue(remaining.any { it.id == "5" })
        assertTrue(remaining.any { it.id == "6" })
        assertFalse(remaining.any { it.id == "2" })
        assertFalse(remaining.any { it.id == "3" })
    }

    @Test
    fun migration3To4_verifiesVersionNumbers() {
        assertEquals(3, AppDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, AppDatabase.MIGRATION_3_4.endVersion)
    }

    @Test
    fun taskCountsByDate_calculatesCorrectTaskCountsPerDate() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val t1 = Task(id = "1", title = "Task 1", date = today)
        val t2 = Task(id = "2", title = "Task 2", date = today)
        val t3 = Task(id = "3", title = "Task 3", date = tomorrow)

        repository.insertTasks(listOf(t1, t2, t3))
        advanceUntilIdle()

        val counts = viewModel.taskCountsByDate.value
        assertEquals(2, counts[today])
        assertEquals(1, counts[tomorrow])
        assertEquals(2, viewModel.uiState.value.taskCountsByDate[today])
        assertEquals(1, viewModel.uiState.value.taskCountsByDate[tomorrow])
    }

    @Test
    fun totalTaskCount_reflectsTotalNumberOfTasksAcrossAllDates() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val t1 = Task(id = "1", title = "Task 1", date = today)
        val t2 = Task(id = "2", title = "Task 2", date = today)
        val t3 = Task(id = "3", title = "Task 3", date = tomorrow)

        repository.insertTasks(listOf(t1, t2, t3))
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.totalTaskCount)
    }

    @Test
    fun converters_taskStatusConversions() {
        val converters = com.capycdr786.todo.data.local.Converters()
        assertEquals(TaskStatus.PENDING, converters.toTaskStatus("TODO"))
        assertEquals(TaskStatus.PENDING, converters.toTaskStatus("PENDING"))
        assertEquals(TaskStatus.ONGOING, converters.toTaskStatus("ONGOING"))
        assertEquals(TaskStatus.DONE, converters.toTaskStatus("DONE"))
        assertEquals(TaskStatus.CANCELLED, converters.toTaskStatus("CANCELLED"))
        assertEquals(TaskStatus.PENDING, converters.toTaskStatus("UNKNOWN_INVALID"))
        assertEquals(TaskStatus.PENDING, converters.toTaskStatus(null))

        assertEquals("PENDING", converters.fromTaskStatus(TaskStatus.PENDING))
        assertEquals("DONE", converters.fromTaskStatus(TaskStatus.DONE))
        assertNull(converters.fromTaskStatus(null))
    }

    @Test
    fun converters_localDateConversions() {
        val converters = com.capycdr786.todo.data.local.Converters()
        val now = LocalDate.now()
        val stringDate = converters.fromLocalDate(now)
        assertEquals(now.toString(), stringDate)
        assertEquals(now, converters.toLocalDate(stringDate))
        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
    }

    @Test
    fun task_jsonSerializationRoundTrip() {
        val originalTask = Task(
            id = "test-id-123",
            title = "Task with 🚀 unicode",
            description = "Line 1\nLine 2 with special chars: <>&'\"",
            date = LocalDate.of(2026, 12, 31),
            status = TaskStatus.CANCELLED,
            isCompleted = false,
            cancellationReason = "Requirement dropped",
            repeatGroupId = "grp-456",
        )

        val json = Json { prettyPrint = true }
        val jsonString = json.encodeToString(Task.serializer(), originalTask)
        val deserialized = json.decodeFromString(Task.serializer(), jsonString)

        assertEquals(originalTask, deserialized)
        assertEquals("test-id-123", deserialized.id)
        assertEquals("Task with 🚀 unicode", deserialized.title)
        assertEquals(LocalDate.of(2026, 12, 31), deserialized.date)
        assertEquals(TaskStatus.CANCELLED, deserialized.status)
        assertEquals("Requirement dropped", deserialized.cancellationReason)
        assertEquals("grp-456", deserialized.repeatGroupId)
    }

    @Test
    fun addRepeatingTask_invalidDateRange_doesNotInsert() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val start = LocalDate.now().plusDays(5)
        val end = LocalDate.now().plusDays(2)

        viewModel.addRepeatingTask(
            title = "Invalid Range Task",
            startDate = start,
            endDate = end,
            daysOfWeek = setOf(DayOfWeek.MONDAY),
        )
        advanceUntilIdle()

        assertEquals(0, viewModel.allTasks.value.size)
    }

    @Test
    fun addRepeatingTask_emptyDaysOfWeek_doesNotInsert() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val start = LocalDate.now()
        val end = start.plusDays(7)

        viewModel.addRepeatingTask(
            title = "Empty Days Task",
            startDate = start,
            endDate = end,
            daysOfWeek = emptySet(),
        )
        advanceUntilIdle()

        assertEquals(0, viewModel.allTasks.value.size)
    }

    @Test
    fun addRepeatingTask_crossMonthRange_correctlyCalculatesDates() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val start = LocalDate.of(2026, 10, 30)
        val end = LocalDate.of(2026, 11, 3)
        val days = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)

        viewModel.addRepeatingTask(
            title = "Month Transition Task",
            startDate = start,
            endDate = end,
            daysOfWeek = days,
        )
        advanceUntilIdle()

        val tasks = viewModel.allTasks.value
        assertEquals(2, tasks.size)
        assertTrue(tasks.any { it.date == LocalDate.of(2026, 10, 30) })
        assertTrue(tasks.any { it.date == LocalDate.of(2026, 11, 2) })
    }

    @Test
    fun setTaskStatus_nonCancelledStatus_clearsCancellationReason() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val task = Task(
            id = "c1",
            title = "Cancelled Task",
            status = TaskStatus.CANCELLED,
            cancellationReason = "Original reason",
        )
        repository.addTask(task)
        advanceUntilIdle()

        viewModel.setTaskStatus("c1", TaskStatus.ONGOING)
        advanceUntilIdle()

        val ongoingTask = repository.getTaskById("c1")
        assertNotNull(ongoingTask)
        assertEquals(TaskStatus.ONGOING, ongoingTask?.status)
        assertNull(ongoingTask?.cancellationReason)
        assertFalse(ongoingTask!!.isCompleted)

        viewModel.setTaskStatus("c1", TaskStatus.DONE)
        advanceUntilIdle()

        val doneTask = repository.getTaskById("c1")
        assertNotNull(doneTask)
        assertEquals(TaskStatus.DONE, doneTask?.status)
        assertNull(doneTask?.cancellationReason)
        assertTrue(doneTask!!.isCompleted)
    }

    @Test
    fun addTask_withUnicodeAndSpecialCharacters_persistsCorrectly() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val unicodeTitle = "Buy ☕ and 🍎 (URGENT!) <tags>"
        val unicodeDesc = "Notes with €100 cost, 日本語 text, and emojis 🎉"

        viewModel.addTask(title = unicodeTitle, description = unicodeDesc)
        advanceUntilIdle()

        val tasks = viewModel.tasksForSelectedDate.value
        assertEquals(1, tasks.size)
        assertEquals(unicodeTitle, tasks[0].title)
        assertEquals(unicodeDesc, tasks[0].description)
    }

    @Test
    fun migration2To3_verifiesVersionNumbers() {
        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)
    }
}
