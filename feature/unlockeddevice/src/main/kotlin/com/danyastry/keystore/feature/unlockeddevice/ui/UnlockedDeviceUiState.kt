package com.danyastry.keystore.feature.unlockeddevice.ui

import com.danyastry.keystore.feature.unlockeddevice.ProbeKey
import com.danyastry.keystore.feature.unlockeddevice.ProbeResult

data class ProbeRound(val number: Int, val wasDeviceLocked: Boolean, val results: Map<ProbeKey, ProbeResult>)

data class UnlockedDeviceUiState(
    val hasScreenLock: Boolean? = null,
    val hasKeys: Boolean = false,
    val secondsUntilTest: Int = 0,
    val isTesting: Boolean = false,
    val rounds: List<ProbeRound> = emptyList(),
    val isKeyCreationFailed: Boolean = false,
) {
    val isCountingDown: Boolean
        get() = secondsUntilTest > 0

    val canCreateKeys: Boolean
        get() = !hasKeys

    val canTest: Boolean
        get() = hasKeys && !isCountingDown && !isTesting
}
