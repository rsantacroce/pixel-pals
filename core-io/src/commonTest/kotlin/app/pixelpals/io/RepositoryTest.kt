package app.pixelpals.io

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RepositoryTest {
    private var now = 1_000_000L
    private val fs = MemoryFileStore()
    private val repo = ProjectRepository(fs) { now }

    @Test
    fun saveListLoad() =
        runTest {
            repo.save(sampleProject("a"))
            repo.save(sampleProject("b").copy(updatedAt = 5000))
            val list = repo.list()
            assertEquals(listOf("b", "a"), list.projects.map { it.id })
            assertEquals(sampleProject("a"), repo.load("a"))
        }

    @Test
    fun corruptFilesAreMovedAsideNotCrashing() =
        runTest {
            repo.save(sampleProject("ok"))
            fs.writeAtomic("projects/bad.pixelpals", "garbage".encodeToByteArray())
            val list = repo.list()
            assertEquals(listOf("ok"), list.projects.map { it.id })
            assertEquals(listOf("bad.pixelpals"), list.movedAside)
            assertEquals(1, fs.list("corrupt").size)

            fs.writeAtomic("projects/bad2.pixelpals", "{}".encodeToByteArray())
            assertFailsWith<CorruptProjectException> { repo.load("bad2") }
            assertEquals(2, fs.list("corrupt").size)
        }

    @Test
    fun duplicateAndRename() =
        runTest {
            repo.save(sampleProject("a"))
            val copy = repo.duplicate("a", "Copy")
            repo.rename("a", "Renamed")
            assertEquals(setOf("Renamed", "Copy"), repo.list().projects.map { it.name }.toSet())
            assertEquals(sampleProject().drawing, repo.load(copy.id).drawing)
        }

    @Test
    fun trashRestoreAndExpiry() =
        runTest {
            repo.save(sampleProject("a"))
            repo.save(sampleProject("b"))
            repo.moveToTrash("a")
            assertEquals(listOf("b"), repo.list().projects.map { it.id })
            assertEquals(listOf("a"), repo.listTrash().map { it.summary.id })
            repo.restore("a")
            assertEquals(2, repo.list().projects.size)

            repo.moveToTrash("a")
            now += 29L * 24 * 3600 * 1000
            repo.purgeExpiredTrash()
            assertEquals(1, repo.listTrash().size)
            now += 2L * 24 * 3600 * 1000
            repo.purgeExpiredTrash()
            assertEquals(0, repo.listTrash().size)
            assertTrue(fs.list("trash").isEmpty())
        }

    @Test
    fun emptyTrash() =
        runTest {
            repo.save(sampleProject("a"))
            repo.moveToTrash("a")
            repo.emptyTrash()
            assertTrue(repo.listTrash().isEmpty())
        }

    @Test
    fun settingsRoundTripAndDefaults() {
        val s = SettingsRepository(fs)
        assertEquals(AppSettings(), s.load())
        s.save(AppSettings(soundOn = false, customColors = listOf(1, 2)))
        assertEquals(listOf(1, 2), s.load().customColors)
        fs.writeAtomic("settings.json", "nonsense".encodeToByteArray())
        assertEquals(AppSettings(), s.load())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun autosaverDebouncesAndFlushes() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        val saved = mutableListOf<String>()
        val saver = Autosaver(scope, 1000, dispatcher) { saved += it.name }
        saver.schedule(sampleProject().copy(name = "1"))
        scope.advanceTimeBy(500)
        saver.schedule(sampleProject().copy(name = "2"))
        scope.advanceTimeBy(900)
        scope.runCurrent()
        assertEquals(emptyList(), saved)
        scope.advanceTimeBy(200)
        scope.runCurrent()
        assertEquals(listOf("2"), saved)
        saver.schedule(sampleProject().copy(name = "3"))
        scope.runTest { saver.flush() }
        assertEquals(listOf("2", "3"), saved)
    }
}
