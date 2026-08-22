package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.gogsclient.Repository
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.git.Repo
import org.bibletranslationtools.writer.git.TransportCallback
import org.bibletranslationtools.writer.usecases.GetRepository
import org.bibletranslationtools.writer.usecases.PushTargetTranslation
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.DeleteBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.errors.NoRemoteRepositoryException
import org.eclipse.jgit.transport.PushResult
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.URIish
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class PushTargetTranslationTest {

    @MockK private lateinit var profile: Profile
    @MockK private lateinit var getRepository: GetRepository
    @MockK private lateinit var transportCallback: TransportCallback
    @MockK private lateinit var targetTranslation: TargetTranslation
    @MockK private lateinit var git: Git
    @MockK private lateinit var repo: Repo
    @MockK private lateinit var repository: Repository
    @MockK private lateinit var deleteCommand: DeleteBranchCommand
    @MockK private lateinit var createCommand: CreateBranchCommand
    @MockK private lateinit var pushCommand: PushCommand
    @MockK(relaxed = true) private lateinit var preference: Preference

    private val onProgress = mockk<(Float, String?) -> Unit>(relaxed = true)
    private lateinit var pushTargetTranslation: PushTargetTranslation

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        pushTargetTranslation = PushTargetTranslation(
            profile,
            getRepository,
            transportCallback,
            preference
        )

        every { onProgress(any(), any()) }.just(runs)
        every { repository.sshUrl }.returns("ssh://repo.git")
        coEvery { getRepository.execute(targetTranslation, onProgress) }.returns(repository)

        every { targetTranslation.id }.returns("test")
        every { targetTranslation.commitSync() }.returns(true)
        every { targetTranslation.repo }.returns(repo)
        every { repo.git }.returns(git)
        every { git.branchDelete() }.returns(deleteCommand)
        every { git.branchCreate() }.returns(createCommand)
        every { deleteCommand.setBranchNames(any<String>()) }.returns(deleteCommand)
        every { deleteCommand.setForce(any()) }.returns(deleteCommand)
        every { deleteCommand.call() }.returns(listOf())

        every { createCommand.setName(any()) }.returns(createCommand)
        every { createCommand.setForce(any()) }.returns(createCommand)
        every { createCommand.call() }.returns(mockk())

        every { pushCommand.setTransportConfigCallback(any()) }.returns(pushCommand)
        every { pushCommand.setRemote(any()) }.returns(pushCommand)
        every { pushCommand.setPushTags() }.returns(pushCommand)
        every { pushCommand.setForce(any()) }.returns(pushCommand)
        every { pushCommand.setRefSpecs(any<RefSpec>()) }.returns(pushCommand)

        every { repo.deleteRemote(any()) }.just(runs)
        every { repo.setRemote(any(), any()) }.just(runs)
        every { git.push() }.returns(pushCommand)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test push target translation authorized`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.OK)
            every { remoteName }.returns("test_repo")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { pushCommand.call() }.returns(listOf(pushResult))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        val expectedMessage = """
            [${refUpdate.remoteName}] Success push to remote ref.
            Server: ${repository.sshUrl}
            
        """.trimIndent()

        assertEquals(PushTargetTranslation.Status.OK, result.status)
        assertFalse(result.status.isRejected)
        assertEquals(expectedMessage, result.message)

        verifySuccessCalls(refUpdate, pushResult)
    }

    @Test
    fun `test push target translation not authorized`() = runTest {
        every { profile.gogsUser }.returns(null)

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.AUTH_FAILURE, result.status)
        assertFalse(result.status.isRejected)
        assertNull(result.message)

        verify { profile.gogsUser }
        verify(exactly = 0) { pushCommand.call() }
        verify(exactly = 0) { onProgress(any(), any()) }
        verify(exactly = 0) { repository.sshUrl }
        coVerify(exactly = 0) { getRepository.execute(targetTranslation, onProgress) }
    }

    @Test
    fun `test push target translation, remote repo not found and not created`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        coEvery { getRepository.execute(any(), any()) }.returns(null)

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.UNKNOWN, result.status)
        assertFalse(result.status.isRejected)
        assertNull(result.message)

        verify { profile.gogsUser }
        verify(exactly = 0) { pushCommand.call() }
        verify(exactly = 0) { repository.sshUrl }
        coVerify { getRepository.execute(targetTranslation, onProgress) }
    }

    @Test
    fun `test push target translation, translation commit failed`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        coEvery { getRepository.execute(any(), any()) }.returns(null)
        every { targetTranslation.commitSync() }.throws(Exception("Error committing translation"))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.UNKNOWN, result.status)
        assertFalse(result.status.isRejected)
        assertNull(result.message)

        verify { profile.gogsUser }
        verify(exactly = 0) { pushCommand.call() }
        verify(exactly = 0) { repository.sshUrl }
        coVerify(exactly = 0) { getRepository.execute(targetTranslation, onProgress) }
    }

    @Test
    fun `test push target translation, delete origin failed`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        every { repo.deleteRemote(any()) }.throws(IOException("Error deleting remote"))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.UNKNOWN, result.status)
        assertFalse(result.status.isRejected)
        assertEquals("Error deleting remote", result.message)

        verify { profile.gogsUser }
        verify(exactly = 0) { pushCommand.call() }
        verify { repo.deleteRemote(any()) }
        verify(exactly = 0) { repo.setRemote(any(), any()) }
        verify { onProgress(any(), any()) }
        verify { repository.sshUrl }
        coVerify { getRepository.execute(targetTranslation, onProgress) }
    }

    @Test
    fun `test push target translation, rejected non-fast-forward`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD)
            every { remoteName }.returns("test_repo")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { pushCommand.call() }.returns(listOf(pushResult))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        val expectedMessage = """
            [${refUpdate.remoteName}] Remote ref update was rejected, as it would cause non fast-forward update.
            Server: ${repository.sshUrl}
            
        """.trimIndent()

        assertEquals(PushTargetTranslation.Status.REJECTED_NON_FAST_FORWARD, result.status)
        assertTrue(result.status.isRejected)
        assertEquals(expectedMessage, result.message)

        verifySuccessCalls(refUpdate, pushResult)
    }

    @Test
    fun `test push target translation, rejected non-delete`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.REJECTED_NODELETE)
            every { remoteName }.returns("test_repo")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { pushCommand.call() }.returns(listOf(pushResult))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        val expectedMessage = """
            [${refUpdate.remoteName}] Remote ref update was rejected, because remote side doesn't support/allow deleting refs.
            Server: ${repository.sshUrl}
            
        """.trimIndent()

        assertEquals(PushTargetTranslation.Status.REJECTED_NODELETE, result.status)
        assertTrue(result.status.isRejected)
        assertEquals(expectedMessage, result.message)

        verifySuccessCalls(refUpdate, pushResult)
    }

    @Test
    fun `test push target translation, rejected remote changed`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED)
            every { remoteName }.returns("test_repo")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { pushCommand.call() }.returns(listOf(pushResult))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        val expectedMessage = """
            [${refUpdate.remoteName}] Remote ref update was rejected,  because old object id on remote repository wasn't the same as defined expected old object.
            Server: ${repository.sshUrl}
            
        """.trimIndent()

        assertEquals(PushTargetTranslation.Status.REJECTED_REMOTE_CHANGED, result.status)
        assertTrue(result.status.isRejected)
        assertEquals(expectedMessage, result.message)

        verifySuccessCalls(refUpdate, pushResult)
    }

    @Test
    fun `test push target translation, rejected other reason`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.REJECTED_OTHER_REASON)
            every { remoteName }.returns("test_repo")
            every { message }.returns("test reason")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { pushCommand.call() }.returns(listOf(pushResult))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        val expectedMessage = """
            [${refUpdate.remoteName}] Remote ref update was rejected, because test reason.
            Server: ${repository.sshUrl}
            
        """.trimIndent()

        assertEquals(PushTargetTranslation.Status.REJECTED_OTHER_REASON, result.status)
        assertTrue(result.status.isRejected)
        assertEquals(expectedMessage, result.message)

        verifySuccessCalls(refUpdate, pushResult)
    }

    @Test
    fun `test push target translation, auth failed`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        val exception = TransportException(
            "An error occurred.",
            Exception(
                Exception("Auth fail")
            )
        )
        every { pushCommand.call() }.throws(exception)

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.AUTH_FAILURE, result.status)
        assertFalse(result.status.isRejected)
        assertNull(result.message)

        verifyCommonCalls()
    }

    @Test
    fun `test push target translation, remote repo not found`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        val exception = TransportException(
            "An error occurred.",
            NoRemoteRepositoryException(URIish(), "No remote repository")
        )
        every { pushCommand.call() }.throws(exception)

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.NO_REMOTE_REPO, result.status)
        assertFalse(result.status.isRejected)
        assertNull(result.message)

        verifyCommonCalls()
    }

    @Test
    fun `test push target translation, push to private repo fails`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        val exception = TransportException(
            "An error occurred.",
            Exception("Push to private repo is not permitted")
        )
        every { pushCommand.call() }.throws(exception)

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.AUTH_FAILURE, result.status)
        assertFalse(result.status.isRejected)
        assertNull(result.message)

        verifyCommonCalls()
    }

    @Test
    fun `test push target translation, unknown transport exception`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        every { pushCommand.call() }.throws(TransportException("An error occurred."))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.UNKNOWN, result.status)
        assertFalse(result.status.isRejected)
        assertNull(result.message)

        verifyCommonCalls()
    }

    @Test
    fun `test push target translation, out of memory error`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        every { pushCommand.call() }.throws(OutOfMemoryError("Out of memory"))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.OUT_OF_MEMORY, result.status)
        assertFalse(result.status.isRejected)
        assertNull(result.message)

        verifyCommonCalls()
    }

    @Test
    fun `test push target translation, generic exception`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        every { pushCommand.call() }.throws(Exception("An error occurred."))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.UNKNOWN, result.status)
        assertFalse(result.status.isRejected)
        assertNull(result.message)

        verifyCommonCalls()
    }

    @Test
    fun `test push target translation, base exception`() = runTest {
        every { profile.gogsUser }.returns(mockk())

        every { pushCommand.call() }.throws(Throwable("An error occurred."))

        val result = pushTargetTranslation.execute(targetTranslation, onProgress)

        assertEquals(PushTargetTranslation.Status.UNKNOWN, result.status)
        assertFalse(result.status.isRejected)
        assertNull(result.message)

        verifyCommonCalls()
    }

    private fun verifySuccessCalls(refUpdate: RemoteRefUpdate, pushResult: PushResult) {
        verifyCommonCalls()

        verify { refUpdate.status }
        verify { refUpdate.remoteName }
        verify { pushResult.remoteUpdates }
    }

    private fun verifyCommonCalls() {
        verify { profile.gogsUser }
        verify { pushCommand.call() }
        verify { repo.deleteRemote(any()) }
        verify { repo.setRemote(any(), any()) }
        verify { onProgress(any(), any()) }
        verify { repository.sshUrl }
        coVerify { getRepository.execute(targetTranslation, onProgress) }
    }
}