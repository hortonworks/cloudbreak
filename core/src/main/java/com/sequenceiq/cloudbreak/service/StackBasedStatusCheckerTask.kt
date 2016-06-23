package com.sequenceiq.cloudbreak.service

import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.StackRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.inject.Inject

abstract class StackBasedStatusCheckerTask<T : StackContext> : SimpleStatusCheckerTask<T>() {

    @Inject
    private val stackRepository: StackRepository? = null

    override fun exitPolling(t: T): Boolean {
        try {
            val stack = stackRepository!!.findByIdLazy(t.stack.id)
            if (stack == null || stack.isDeleteInProgress || stack.isDeleteCompleted) {
                return true
            }
            return false
        } catch (ex: Exception) {
            LOGGER.error("Error occurred when check status checker exit criteria: ", ex)
            return true
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterBasedStatusCheckerTask<StackContext>::class.java)
    }

}
