package app.pixelpals.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Debounced autosave: [schedule] after every change; the latest project is written [delayMs] after
 * changes stop. [flush] writes immediately (app pause/close). Writes never run out of order.
 */
public class Autosaver(
    private val scope: CoroutineScope,
    private val delayMs: Long = DEFAULT_DELAY_MS,
    private val writeContext: CoroutineContext = ioDispatcher,
    private val write: (Project) -> Unit,
) {
    private val mutex = Mutex()
    private var pending: Project? = null
    private var job: Job? = null

    /** Called when the last write failed, e.g. disk full. */
    public var onError: (Throwable) -> Unit = {}

    /** True while a change has not been written yet. */
    public val hasPending: Boolean get() = pending != null

    /** Queues [project] for saving. Must be called from a single (UI) thread. */
    public fun schedule(project: Project) {
        pending = project
        job?.cancel()
        job =
            scope.launch {
                delay(delayMs)
                flush()
            }
    }

    /** Writes any pending change now. */
    public suspend fun flush() {
        mutex.withLock {
            val p = pending ?: return
            pending = null
            try {
                withContext(writeContext) { write(p) }
            } catch (e: Exception) {
                if (pending == null) pending = p
                onError(e)
            }
        }
    }

    public companion object {
        /** Default debounce, ~1 s per spec. */
        public const val DEFAULT_DELAY_MS: Long = 1000
    }
}
