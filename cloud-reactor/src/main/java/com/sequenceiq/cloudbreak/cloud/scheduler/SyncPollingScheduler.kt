package com.sequenceiq.cloudbreak.cloud.scheduler

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.google.common.util.concurrent.ListenableScheduledFuture
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.sequenceiq.cloudbreak.cloud.task.FetchTask
import com.sequenceiq.cloudbreak.cloud.task.PollTask

@Component
class SyncPollingScheduler<T> {

    @Inject
    private val scheduler: ListeningScheduledExecutorService? = null

    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    @JvmOverloads fun schedule(task: PollTask<T>, interval: Int = POLLING_INTERVAL, maxAttempt: Int = MAX_POLLING_ATTEMPT, maxFailureTolerant: Int = FAILURE_TOLERANT_ATTEMPT): T {
        var result: T? = null
        var actualFailureTolerant = 0
        for (i in 0..maxAttempt - 1) {
            if (task.cancelled()) {
                throw CancellationException("Task was cancelled.")
            }
            try {
                val ft = schedule(task, interval)
                result = ft.get()
                if (task.completed(result)) {
                    return result
                }
            } catch (ex: Exception) {
                actualFailureTolerant++
                if (actualFailureTolerant >= maxFailureTolerant) {
                    throw ex
                }
            }

        }
        throw TimeoutException(String.format("Task did not finished within %d seconds", interval * maxAttempt))
    }

    fun schedule(task: FetchTask<T>, interval: Int): ListenableScheduledFuture<T> {
        return scheduler!!.schedule(task, interval.toLong(), TimeUnit.SECONDS)
    }

    companion object {

        private val POLLING_INTERVAL = 5
        private val MAX_POLLING_ATTEMPT = 1000
        private val FAILURE_TOLERANT_ATTEMPT = 3
    }

}
