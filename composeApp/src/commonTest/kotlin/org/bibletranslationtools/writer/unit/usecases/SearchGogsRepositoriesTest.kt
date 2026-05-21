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
import org.bibletranslationtools.gogsclient.Repository
import org.bibletranslationtools.gogsclient.Response
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.SearchGogsRepositories
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchGogsRepositoriesTest {

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
    fun `test search with default user`() = runTest {
        val repo = Repository(name = "fr_gen_text_reg", fullName = "user/fr_gen_text_reg")
        coEvery { anyConstructed<GogsAPI>().searchRepos(any(), any(), any()) } returns listOf(repo)
        coEvery { anyConstructed<GogsAPI>().getRepo(any(), any()) } returns repo
        every { anyConstructed<GogsAPI>().getLastResponse() } returns null

        val repositories = SearchGogsRepositories(preference).execute(0, "_gen_", 1, onProgress)

        assertEquals(1, repositories.size)
        verify { onProgress(any(), "Searching for repositories") }
    }

    @Test
    fun `test search with auth user`() = runTest {
        val user = mockk<User>()
        every { user.id }.returns(1)

        val repo = Repository(name = "fr_gen_text_reg", fullName = "user/fr_gen_text_reg")
        coEvery { anyConstructed<GogsAPI>().searchRepos(any(), any(), any()) } returns listOf(repo)
        coEvery { anyConstructed<GogsAPI>().getRepo(any(), any()) } returns repo
        every { anyConstructed<GogsAPI>().getLastResponse() } returns null

        val repositories = SearchGogsRepositories(preference).execute(user.id, "_gen_", 1, onProgress)

        assertEquals(1, repositories.size)
        verify { user.id }
        verify { onProgress(any(), "Searching for repositories") }
    }

    @Test
    fun `test search with empty query`() = runTest {
        val repo = Repository(name = "fr_gen_text_reg", fullName = "user/fr_gen_text_reg")
        coEvery { anyConstructed<GogsAPI>().searchRepos(any(), any(), any()) } returns listOf(repo)
        coEvery { anyConstructed<GogsAPI>().getRepo(any(), any()) } returns repo
        every { anyConstructed<GogsAPI>().getLastResponse() } returns null

        val repositories = SearchGogsRepositories(preference).execute(0, "", 1, onProgress)

        assertEquals(1, repositories.size)
        verify { onProgress(any(), "Searching for repositories") }
    }
}
