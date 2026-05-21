package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.Response
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.SearchGogsUsers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchGogsUsersTest {

    @MockK private lateinit var preference: Preference

    private val onProgress = mockk<(Float, String?) -> Unit>(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkConstructor(GogsAPI::class)
        every { onProgress(any(), any()) } just runs
        every { preference.getPref(any(), any(), String::class) }.returns("http://localhost/api")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test search by user query`() = runTest {
        val user = mockk<User>()
        coEvery { anyConstructed<GogsAPI>().searchUsers(any(), any(), any()) } returns listOf(user)
        every { anyConstructed<GogsAPI>().getLastResponse() } returns null

        val users = SearchGogsUsers(preference).execute("test", 1, onProgress)

        assertEquals(1, users.size)
        verify { onProgress(any(), "Searching for users") }
    }

    @Test
    fun `test search users with empty query`() = runTest {
        coEvery { anyConstructed<GogsAPI>().searchUsers(any(), any(), any()) } returns emptyList()
        every { anyConstructed<GogsAPI>().getLastResponse() } returns null

        val users = SearchGogsUsers(preference).execute("", 1, onProgress)

        assertEquals(0, users.size)
        verify { onProgress(any(), "Searching for users") }
    }
}
