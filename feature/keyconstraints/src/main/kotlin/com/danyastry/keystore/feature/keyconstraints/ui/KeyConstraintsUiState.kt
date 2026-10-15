package com.danyastry.keystore.feature.keyconstraints.ui

import com.danyastry.keystore.feature.keyconstraints.UseResult

enum class WindowPhase { NOT_YET_VALID, VALID, EXPIRED }

data class WindowAttempt(val secondsAfterCreation: Long, val result: UseResult)

data class LimitAttempt(val number: Int, val result: UseResult)

data class WindowState(
    val hasKey: Boolean = false,
    val secondsSinceCreation: Long? = null,
    val attempts: List<WindowAttempt> = emptyList(),
    val isCreationFailed: Boolean = false,
) {
    val phase: WindowPhase?
        get() = secondsSinceCreation?.let {
            when {
                it < KeyConstraintsViewModel.VALID_FROM_SECONDS -> WindowPhase.NOT_YET_VALID
                it < KeyConstraintsViewModel.VALID_UNTIL_SECONDS -> WindowPhase.VALID
                else -> WindowPhase.EXPIRED
            }
        }

    val secondsToNextPhase: Long
        get() = when (phase) {
            WindowPhase.NOT_YET_VALID -> KeyConstraintsViewModel.VALID_FROM_SECONDS - (secondsSinceCreation ?: 0)
            WindowPhase.VALID -> KeyConstraintsViewModel.VALID_UNTIL_SECONDS - (secondsSinceCreation ?: 0)
            WindowPhase.EXPIRED, null -> 0
        }
}

data class LimitState(
    val isSupported: Boolean = false,
    val hasKey: Boolean = false,
    val usesMade: Int = 0,
    val attempts: List<LimitAttempt> = emptyList(),
    val isCreationFailed: Boolean = false,
) {
    val usesLeft: Int
        get() = (KeyConstraintsViewModel.MAX_USES - usesMade).coerceAtLeast(0)
}

data class KeyConstraintsUiState(
    val window: WindowState = WindowState(),
    val limit: LimitState = LimitState(),
)
