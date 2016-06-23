package com.sequenceiq.cloudbreak.cloud.task

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED

abstract class AbstractPollTask<T> @JvmOverloads constructor(override val authenticatedContext: AuthenticatedContext, private val cancellable: Boolean = true) : PollTask<T> {

    override fun cancelled(): Boolean {
        if (!cancellable) {
            return false
        }
        val pollGroup = InMemoryStateStore.getStack(authenticatedContext.cloudContext.id)
        return pollGroup != null && CANCELLED == pollGroup
    }
}
