package org.bibletranslationtools.writer.uitest

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.writer.AppTheme
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.TestDirectoryProvider
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.di.platformModule
import org.bibletranslationtools.writer.di.sharedModule
import org.bibletranslationtools.writer.ui.navigation.DefaultRootComponent
import org.bibletranslationtools.writer.ui.navigation.RootComponent
import org.bibletranslationtools.writer.ui.navigation.RootContent
import org.bibletranslationtools.writer.usecases.UpdateApp
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
fun runWriterUiTest(
    initialConfiguration: RootComponent.Config = RootComponent.Config.Splash,
    test: androidx.compose.ui.test.ComposeUiTest.() -> Unit,
) {
    val testDirectoryProvider = TestDirectoryProvider()

    startKoin {
        modules(
            sharedModule,
            platformModule,
            module { single<DirectoryProvider> { testDirectoryProvider } },
            module {
                single<UpdateApp> {
                    mockk<UpdateApp>().also { updateApp ->
                        coEvery { updateApp.execute(any()) } coAnswers {
                            val onProgress = firstArg<(Float, String?) -> Unit>()
                            onProgress(1f, null)
                        }
                    }
                }
            },
        )
    }

    runBlocking {
        org.koin.core.context.GlobalContext.get().get<Typography>().init()
    }

    try {
        runComposeUiTest {
            setContent {
                val lifecycle = remember { LifecycleRegistry() }
                LaunchedEffect(lifecycle) {
                    lifecycle.resume()
                }
                AppTheme {
                    val root = remember {
                        DefaultRootComponent(
                            componentContext = DefaultComponentContext(lifecycle = lifecycle),
                            onExitApp = {},
                            initialConfiguration = initialConfiguration,
                        )
                    }
                    RootContent(component = root)
                }
            }
            test()
        }
    } finally {
        stopKoin()
        testDirectoryProvider.cleanup()
    }
}
