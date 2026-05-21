package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.Repository
import org.bibletranslationtools.gogsclient.Response
import org.bibletranslationtools.gogsclient.Token
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.CreateRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateRepositoryTest {

    @MockK private lateinit var preference: Preference
    @MockK private lateinit var profile: Profile
    @MockK private lateinit var targetTranslation: TargetTranslation

    private val onProgress = mockk<(Float, String?) -> Unit>(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkConstructor(GogsAPI::class)
        every { onProgress(any(), any()) }.just(runs)
        every { preference.getPref(any(), any(), String::class) }.returns("http://localhost/api")
        every { targetTranslation.id }.returns("aa_gen_text_reg")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test create repository successful`() = runTest {
        val user: User = mockk { every { token } returns Token("token", "abcd") }
        every { profile.gogsUser }.returns(user)

        val repoSlot = slot<Repository>()
        coEvery { anyConstructed<GogsAPI>().createRepo(capture(repoSlot), any()) } returns Repository("aa_gen_text_reg")
        every { anyConstructed<GogsAPI>().getLastResponse() } returns null

        val success = CreateRepository(preference, profile).execute(targetTranslation, onProgress)

        assertTrue(success)
        assertEquals("aa_gen_text_reg", repoSlot.captured.name)
        verify { onProgress(any(), any()) }
        verify { preference.getPref(any(), any(), String::class) }
        verify { targetTranslation.id }
        verify { profile.gogsUser }
    }

    @Test
    fun `test create repository succeeds because remote exists`() = runTest {
        val user: User = mockk { every { token }.returns(Token("token", "abcd")) }
        every { profile.gogsUser }.returns(user)

        coEvery { anyConstructed<GogsAPI>().createRepo(any(), any()) } returns null
        every { anyConstructed<GogsAPI>().getLastResponse() } returns Response(409)

        val success = CreateRepository(preference, profile).execute(targetTranslation, onProgress)

        assertTrue(success)
        verify { onProgress(any(), any()) }
        verify { preference.getPref(any(), any(), String::class) }
        verify { targetTranslation.id }
        verify { profile.gogsUser }
    }

    @Test
    fun `test create repository fails because no user`() = runTest {
        every { profile.gogsUser }.returns(null)

        val success = CreateRepository(preference, profile).execute(targetTranslation, onProgress)

        assertFalse(success)
        verify { onProgress(any(), any()) }
        verify { preference.getPref(any(), any(), String::class) }
        verify(exactly = 0) { targetTranslation.id }
        verify { profile.gogsUser }
    }
}
