package org.bibletranslationtools.writer.unit.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

suspend fun <T> StateFlow<T>.awaitState(timeoutMs: Long = 1000, condition: (T) -> Boolean): T {
    return withTimeout(timeoutMs) {
        this@awaitState.first { condition(it) }
    }
}

suspend fun <T> Flow<T>.awaitEvent(timeoutMs: Long = 1000, condition: (T) -> Boolean = { true }): T {
    return withTimeout(timeoutMs) {
        this@awaitEvent.first { condition(it) }
    }
}
