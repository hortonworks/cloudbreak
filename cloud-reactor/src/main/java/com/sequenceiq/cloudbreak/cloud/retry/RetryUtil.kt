package com.sequenceiq.cloudbreak.cloud.retry

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RetryUtil private constructor(private var retryCount: Int) : Runnable {
    private var task: RetryTask? = null
    private var errorHandler: ErrorTask? = null
    private var exceptionCheck: ExceptionCheckTask? = null
    private var check: CheckTask? = null

    fun retry(task: RetryTask): RetryUtil {
        this.task = task
        return this
    }

    fun retryIfFalse(check: CheckTask): RetryUtil {
        this.check = check
        return this
    }

    fun checkIfRecoverable(check: ExceptionCheckTask): RetryUtil {
        this.exceptionCheck = check
        return this
    }

    fun ifNotRecoverable(errorHandler: ErrorTask): RetryUtil {
        this.errorHandler = errorHandler
        return this
    }

    override fun run() {
        try {
            retryCount--
            task!!.run()
            runChecker()
        } catch (e: Exception) {
            runExceptionChecker(e)
        }

    }

    private fun runRetry() {
        if (retryCount > 0) {
            run()
        } else {
            runErrorHandler(RetryException("too many retries"))
        }
    }

    @Throws(Exception::class)
    private fun runChecker() {
        if (check != null && !check!!.check()) {
            runRetry()
        }
    }

    private fun runExceptionChecker(e: Exception) {
        if (exceptionCheck == null || exceptionCheck != null && exceptionCheck!!.check(e)) {
            runRetry()
        } else {
            runErrorHandler(e)
        }
    }

    private fun runErrorHandler(e: Exception) {
        try {
            if (errorHandler != null) {
                errorHandler!!.run(e)
            }
        } catch (ex: Exception) {
            LOGGER.warn("ErrorHandler failed during retries.", ex)
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RetryUtil::class.java)
        private val DEFAULT_NUMBER_OF_RETRIES = 3

        fun withDefaultRetries(): RetryUtil {
            return RetryUtil(DEFAULT_NUMBER_OF_RETRIES)
        }

        fun withRetries(count: Int): RetryUtil {
            return RetryUtil(count)
        }
    }

}
