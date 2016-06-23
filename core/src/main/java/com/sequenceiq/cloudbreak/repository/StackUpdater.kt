package com.sequenceiq.cloudbreak.repository

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter
import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.domain.Stack
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import javax.inject.Inject

@Component
class StackUpdater {

    @Inject
    private val stackRepository: StackRepository? = null
    @Inject
    private val resourceRepository: ResourceRepository? = null
    @Inject
    private val statusToPollGroupConverter: StatusToPollGroupConverter? = null

    fun updateStackStatus(stackId: Long?, status: Status): Stack {
        return doUpdateStackStatus(stackId, status, "")
    }

    fun updateStackStatus(stackId: Long?, status: Status, statusReason: String): Stack {
        return doUpdateStackStatus(stackId, status, statusReason)
    }

    fun addStackResources(stackId: Long?, resources: List<Resource>): Stack {
        val stack = stackRepository!!.findById(stackId)
        for (resource in resources) {
            resource.stack = stack
        }
        resourceRepository!!.save(resources)
        stack.resources.addAll(resources)
        return stackRepository.save(stack)
    }

    fun removeStackResources(resources: List<Resource>) {
        resourceRepository!!.delete(resources)
    }

    private fun doUpdateStackStatus(stackId: Long?, status: Status?, statusReason: String?): Stack {
        var stack = stackRepository!!.findById(stackId)
        if (!stack.isDeleteCompleted) {
            if (status != null) {
                stack.status = status
            }
            if (statusReason != null) {
                stack.statusReason = statusReason
            }
            InMemoryStateStore.putStack(stackId, statusToPollGroupConverter!!.convert(status))
            if (Status.DELETE_COMPLETED == status) {
                InMemoryStateStore.deleteStack(stackId)
            }
            stack = stackRepository.save(stack)
        }
        return stack
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(StackUpdater::class.java)
    }

}
