package com.capycdr786.todo.ui.theme

import androidx.compose.ui.graphics.Color
import com.capycdr786.todo.data.model.TaskStatus

val LightPrimary = Color(0xFF2563EB)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDBEAFE)
val LightOnPrimaryContainer = Color(0xFF1E40AF)

val LightSecondary = Color(0xFF475569)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFF1F5F9)
val LightOnSecondaryContainer = Color(0xFF1E293B)

val LightTertiary = Color(0xFF7C3AED)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFEDE9FE)
val LightOnTertiaryContainer = Color(0xFF5B21B6)

val LightBackground = Color(0xFFF8FAFC)
val LightOnBackground = Color(0xFF0F172A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)

val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurfaceVariant = Color(0xFF64748B)
val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)

val DarkPrimary = Color(0xFF60A5FA)
val DarkOnPrimary = Color(0xFF1E3A8A)
val DarkPrimaryContainer = Color(0xFF1E40AF)
val DarkOnPrimaryContainer = Color(0xFFBFDBFE)

val DarkSecondary = Color(0xFF94A3B8)
val DarkOnSecondary = Color(0xFF0F172A)
val DarkSecondaryContainer = Color(0xFF334155)
val DarkOnSecondaryContainer = Color(0xFFE2E8F0)

val DarkTertiary = Color(0xFFA78BFA)
val DarkOnTertiary = Color(0xFF4C1D95)
val DarkTertiaryContainer = Color(0xFF5B21B6)
val DarkOnTertiaryContainer = Color(0xFFDDD6FE)

val DarkBackground = Color(0xFF0F172A)
val DarkOnBackground = Color(0xFFF8FAFC)
val DarkSurface = Color(0xFF1E293B)
val DarkOnSurface = Color(0xFFF8FAFC)

val DarkSurfaceVariant = Color(0xFF334155)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF475569)
val DarkOutlineVariant = Color(0xFF334155)

val PendingBadgeContainerLight = Color(0xFFF1F5F9)
val PendingBadgeContentLight = Color(0xFF475569)
val PendingBadgeContainerDark = Color(0xFF334155)
val PendingBadgeContentDark = Color(0xFFF8FAFC)

val OngoingBadgeContainerLight = Color(0xFFFEF3C7)
val OngoingBadgeContentLight = Color(0xFFB45309)
val OngoingBadgeContainerDark = Color(0xFF78350F)
val OngoingBadgeContentDark = Color(0xFFFDE68A)

val DoneBadgeContainerLight = Color(0xFFDCFCE7)
val DoneBadgeContentLight = Color(0xFF15803D)
val DoneBadgeContainerDark = Color(0xFF14532D)
val DoneBadgeContentDark = Color(0xFF86EFAC)

val CancelledBadgeContainerLight = Color(0xFFFEE2E2)
val CancelledBadgeContentLight = Color(0xFFB91C1C)
val CancelledBadgeContainerDark = Color(0xFF7F1D1D)
val CancelledBadgeContentDark = Color(0xFFFCA5A5)

data class StatusBadgeColors(
    val containerColor: Color,
    val contentColor: Color,
)

fun getStatusBadgeColors(status: TaskStatus, isDarkTheme: Boolean): StatusBadgeColors {
    return when (status) {
        TaskStatus.PENDING -> if (isDarkTheme) {
            StatusBadgeColors(PendingBadgeContainerDark, PendingBadgeContentDark)
        } else {
            StatusBadgeColors(PendingBadgeContainerLight, PendingBadgeContentLight)
        }

        TaskStatus.ONGOING -> if (isDarkTheme) {
            StatusBadgeColors(OngoingBadgeContainerDark, OngoingBadgeContentDark)
        } else {
            StatusBadgeColors(OngoingBadgeContainerLight, OngoingBadgeContentLight)
        }

        TaskStatus.DONE -> if (isDarkTheme) {
            StatusBadgeColors(DoneBadgeContainerDark, DoneBadgeContentDark)
        } else {
            StatusBadgeColors(DoneBadgeContainerLight, DoneBadgeContentLight)
        }

        TaskStatus.CANCELLED -> if (isDarkTheme) {
            StatusBadgeColors(CancelledBadgeContainerDark, CancelledBadgeContentDark)
        } else {
            StatusBadgeColors(CancelledBadgeContainerLight, CancelledBadgeContentLight)
        }
    }
}
