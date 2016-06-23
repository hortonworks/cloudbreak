package com.sequenceiq.cloudbreak.cloud.retry

import org.mockito.Matchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import org.junit.Test

class RetryUtilTest {

    private val task = mock<RetryTask>(RetryTask::class.java)
    private val error = mock<ErrorTask>(ErrorTask::class.java)
    private val check = mock<CheckTask>(CheckTask::class.java)
    private val exceptionCheck = mock<ExceptionCheckTask>(ExceptionCheckTask::class.java)

    @Test
    @Throws(Exception::class)
    fun testRunWithoutException() {
        runRetryTask()
        verify(task, times(1)).run()
        verify(error, times(0)).run(Exception())
    }

    @Test
    @Throws(Exception::class)
    fun testRunWithoutExceptionCheckOk() {
        `when`(check.check()).thenReturn(true)
        runRetryTaskWithCheck()
        verify(task, times(1)).run()
        verify(check, times(1)).check()
        verify(error, times(0)).run(Exception())
    }

    @Test
    @Throws(Exception::class)
    fun testRunWithoutExceptionCheckNok() {
        `when`(check.check()).thenReturn(false)
        runRetryTaskWithCheck()
        verify(task, times(3)).run()
        verify(check, times(3)).check()
        verify(error, times(1)).run(any<Any>() as Exception)
    }

    @Test
    @Throws(Exception::class)
    fun testRunWithException() {
        doThrow(Exception()).`when`(task).run()
        runRetryTask()
        verify(task, times(3)).run()
        verify(error, times(1)).run(any<Any>() as Exception)
    }

    @Test
    @Throws(Exception::class)
    fun testRunWithOneException() {
        doThrow(Exception()).doNothing().`when`(task).run()
        runRetryTask()
        verify(task, times(2)).run()
        verify(error, times(0)).run(any<Any>() as Exception)
    }

    @Test
    @Throws(Exception::class)
    fun testRunWithRecoverableException() {
        `when`(exceptionCheck.check(any<Any>() as IllegalArgumentException)).thenReturn(true)
        doThrow(IllegalArgumentException()).`when`(task).run()
        runRetryTaskWithExceptionCheck()
        verify(task, times(3)).run()
        verify(exceptionCheck, times(3)).check(any<Any>() as Exception)
        verify(error, times(1)).run(any<Any>() as Exception)
    }

    @Test
    @Throws(Exception::class)
    fun testRunWithNotRecoverableException() {
        `when`(exceptionCheck.check(any<Any>() as NullPointerException)).thenReturn(false)
        doThrow(NullPointerException()).`when`(task).run()
        runRetryTaskWithExceptionCheck()
        verify(task, times(1)).run()
        verify(exceptionCheck, times(1)).check(any<Any>() as Exception)
        verify(error, times(1)).run(any<Any>() as Exception)
    }

    private fun runRetryTask() {
        RetryUtil.withRetries(3).retry(task).ifNotRecoverable(error).run()
    }

    private fun runRetryTaskWithCheck() {
        RetryUtil.withRetries(3).retry(task).retryIfFalse(check).ifNotRecoverable(error).run()
    }

    private fun runRetryTaskWithExceptionCheck() {
        RetryUtil.withRetries(3).retry(task).checkIfRecoverable(exceptionCheck).ifNotRecoverable(error).run()
    }
}