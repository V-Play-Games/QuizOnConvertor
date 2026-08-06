package net.vplaygames.quizonconvertor.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

enum class JobStatus { PENDING, RUNNING, DONE, ERROR }

data class ProgressEvent(
    val step: String,
    val detail: String = "",
    val percent: Int,        // 0-100
    val status: JobStatus = JobStatus.RUNNING
)

class ConversionJob(val id: String = UUID.randomUUID().toString()) {
    @Volatile var status: JobStatus = JobStatus.PENDING
    @Volatile var resultZip: ByteArray? = null
    @Volatile var errorMessage: String? = null

    /** Unbounded queue; SSE handler drains it live. */
    val events: LinkedBlockingQueue<ProgressEvent> = LinkedBlockingQueue()

    fun emit(step: String, detail: String = "", percent: Int) {
        events.offer(ProgressEvent(step, detail, percent))
    }

    fun finish(zip: ByteArray) {
        resultZip = zip
        status = JobStatus.DONE
        events.offer(ProgressEvent("Done", "Conversion complete. Preparing download…", 100, JobStatus.DONE))
    }

    fun fail(message: String) {
        errorMessage = message
        status = JobStatus.ERROR
        events.offer(ProgressEvent("Error", message, -1, JobStatus.ERROR))
    }
}

object JobRegistry {
    private val jobs = ConcurrentHashMap<String, ConversionJob>()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun create(): ConversionJob {
        val job = ConversionJob()
        jobs[job.id] = job
        // Auto-evict after 10 min using Coroutines
        scope.launch {
            delay(10 * 60 * 1000L)
            jobs.remove(job.id)
        }
        return job
    }

    fun get(id: String): ConversionJob? = jobs[id]
}
