package org.bibletranslationtools.writer.integration.core

import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.TestDirectoryProvider
import org.bibletranslationtools.writer.core.TranslationFormat
import org.bibletranslationtools.writer.di.platformModule
import org.bibletranslationtools.writer.di.sharedModule
import org.bibletranslationtools.writer.rendering.RenderingGroup
import org.bibletranslationtools.writer.rendering.RenderingProvider
import org.bibletranslationtools.writer.ui.textadapters.ComposeTextAdapter
import org.bibletranslationtools.writer.TestUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.io.IOException

// End-to-end integration tests that verify the full rendering pipeline:
// USX input -> USXRenderer.render() -> ComposeTextAdapter.convert() -> AnnotatedString.text
// Expected output is stored in resources/usx/ _processed.data files.
class UsxBrokenRenderTest : KoinTest {

    private val renderingProvider: RenderingProvider by inject()

    private var expectedText: String? = null

    @Before
    fun setUp() {
        Logger.flush()
        startKoin {
            modules(
                sharedModule,
                platformModule,
                module { single<DirectoryProvider> { TestDirectoryProvider() } }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    @Throws(Exception::class)
    fun test01ProcessMk_1_1() {
        val out = doRender(testId = "usx/mk_1_1")
        verifyProcessedText(expectedText!!, out)
    }

    @Test
    @Throws(Exception::class)
    fun test02ProcessMk_7_6() {
        val out = doRender(testId = "usx/mk_7_6")
        verifyProcessedText(expectedText!!, out)
    }

    @Test
    @Throws(Exception::class)
    fun test03ProcessMk_7_14() {
        val out = doRender(testId = "usx/mk_7_14")
        verifyProcessedText(expectedText!!, out)
    }

    @Test
    @Throws(Exception::class)
    fun test04ProcessMk_11_24() {
        val out = doRender(testId = "usx/mk_11_24")
        verifyProcessedText(expectedText!!, out)
    }

    @Test
    @Throws(Exception::class)
    fun test05ProcessMk_16_19() {
        val out = doRender(testId = "usx/mk_16_19")
        verifyProcessedText(expectedText!!, out)
    }

    @Test
    @Throws(Exception::class)
    fun test06ProcessMk_1_1Search() {
        val out = doRender(testId = "usx/mk_1_1")
        verifyProcessedText(expectedText!!, out)
    }

    @Test
    @Throws(Exception::class)
    fun test07ProcessMk_7_6Search() {
        val out = doRender(testId = "usx/mk_7_6")
        verifyProcessedText(expectedText!!, out)
    }

    @Test
    @Throws(Exception::class)
    fun test08ProcessMk_7_14Search() {
        val out = doRender(testId = "usx/mk_7_14")
        verifyProcessedText(expectedText!!, out)
    }

    @Test
    @Throws(Exception::class)
    fun test09ProcessMk_11_24Search() {
        val out = doRender(testId = "usx/mk_11_24")
        verifyProcessedText(expectedText!!, out)
    }

    @Test
    @Throws(Exception::class)
    fun test10ProcessMk_16_19Search() {
        val out = doRender(testId = "usx/mk_16_19")
        verifyProcessedText(expectedText!!, out)
    }

    @Throws(IOException::class)
    private fun doRender(testId: String): String {
        val testTextFile = testId + "_raw.data"
        val expectTextFile = testId + "_processed.data"
        val testText = TestUtils.getResource(testTextFile)
        Assert.assertNotNull(testText)
        Assert.assertFalse(testText.isEmpty())
        expectedText = TestUtils.getResource(expectTextFile)
        Assert.assertNotNull(expectedText)
        Assert.assertFalse(expectedText!!.isEmpty())

        val renderingGroup = RenderingGroup()
        val format = TranslationFormat.USX

        renderingProvider.setupRenderingGroup(format, renderingGroup)

        renderingGroup.init(testText)
        val nodes = renderingGroup.start()

        val annotatedString = ComposeTextAdapter.convert(nodes)
        return annotatedString.text
    }

    private fun verifyProcessedText(expectedText: String, out: String?) {
        Assert.assertNotNull(out)
        Assert.assertFalse(out!!.isEmpty())
        if (out != expectedText) {
            if (out.length != expectedText.length) {
                Logger.e(
                    TAG,
                    "expected length " + expectedText.length + " but got length " + out.length
                )
            }

            var ptr = 0
            while (true) {
                if (ptr >= out.length) {
                    Logger.e(
                        TAG,
                        "expected extra text at position $ptr: '" + expectedText.substring(ptr) + "'"
                    )
                    if (ptr < expectedText.length) {
                        Logger.e(
                            TAG,
                            "character: '" + expectedText[ptr] + "', " + Character.codePointAt(
                                expectedText,
                                ptr
                            )
                        )
                    }
                    break
                }
                if (ptr >= expectedText.length) {
                    Logger.e(
                        TAG,
                        "not expected extra text at position " + ptr + ": '" + out.substring(ptr) + "'"
                    )
                    Logger.e(
                        TAG,
                        "character: '" + out[ptr] + "', " + Character.codePointAt(out, ptr)
                    )
                    break
                }

                val cOut = out[ptr]
                val cExpect = expectedText[ptr]
                if (cOut != cExpect) {
                    Logger.e(TAG, "expected different at position $ptr")
                    Logger.e(TAG, "expected: '" + expectedText.substring(ptr) + "'")
                    Logger.e(TAG, "but got: '" + out.substring(ptr) + "'")
                    Logger.e(
                        TAG,
                        "expected character: '" + expectedText[ptr] + "', " + Character.codePointAt(
                            expectedText,
                            ptr
                        )
                    )
                    Logger.e(
                        TAG,
                        "but got character: '" + out[ptr] + "', " + Character.codePointAt(out, ptr)
                    )
                    break
                }
                ptr++
            }
        }
        Assert.assertEquals(out, expectedText)
    }

    companion object {
        val TAG: String = UsxBrokenRenderTest::class.java.simpleName
    }
}
