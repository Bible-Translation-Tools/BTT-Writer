package org.bibletranslationtools.writer.unit.ui

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DecomposeSettings
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.bibletranslationtools.writer.core.ComponentScope
import org.junit.After
import org.junit.Before
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.test.KoinTest
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalDecomposeApi::class)
abstract class BaseComponentTest : KoinTest {

    private val trackedLifecycles = mutableListOf<LifecycleRegistry>()
    private val trackedScopes = mutableListOf<CoroutineScope>()

    @Before
    fun baseSetUp() {
        Dispatchers.setMain(SafeUnconfinedDispatcher)
        DecomposeSettings.update { it.copy(mainThreadCheckEnabled = false) }
    }

    @After
    fun baseTearDown() {
        runBlocking {
            trackedScopes.forEach {
                runCatching { it.coroutineContext[Job]?.cancelAndJoin() }
            }
        }
        trackedScopes.clear()
        trackedLifecycles.forEach {
            runCatching { it.stop() }
            runCatching { it.destroy() }
        }
        trackedLifecycles.clear()
        unmockkAll()
        Dispatchers.resetMain()
        DecomposeSettings.update { it.copy(mainThreadCheckEnabled = true) }
        stopKoin()
    }

    fun <T : Any> createComponent(factory: (ComponentContext) -> T): T {
        val lifecycle = LifecycleRegistry()
        trackedLifecycles.add(lifecycle)
        val component = factory(DefaultComponentContext(lifecycle = lifecycle))
        lifecycle.resume()
        if (component is ComponentScope) {
            trackedScopes.add(component.coroutineScope)
        }
        return component
    }
}

internal object SafeUnconfinedDispatcher : MainCoroutineDispatcher() {
    override val immediate: MainCoroutineDispatcher get() = this
    override fun isDispatchNeeded(context: CoroutineContext): Boolean = false
    override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
}
