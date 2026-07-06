package org.bibletranslationtools.writer.core

import kotlinx.coroutines.CoroutineScope

interface ComponentScope {
    val coroutineScope: CoroutineScope
}