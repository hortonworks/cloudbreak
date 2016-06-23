package com.sequenceiq.cloudbreak.cloud.store

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import java.util.concurrent.ConcurrentHashMap

object InMemoryStateStore {

    private val STACK_STATE_STORE = ConcurrentHashMap<Long, PollGroup>()
    private val CLUSTER_STATE_STORE = ConcurrentHashMap<Long, PollGroup>()

    fun getStack(key: Long?): PollGroup {
        return STACK_STATE_STORE.get(key)
    }

    fun putStack(key: Long?, value: PollGroup) {
        STACK_STATE_STORE.put(key, value)
    }

    fun deleteStack(key: Long?) {
        STACK_STATE_STORE.remove(key)
    }

    fun getCluster(key: Long?): PollGroup {
        return CLUSTER_STATE_STORE.get(key)
    }

    fun putCluster(key: Long?, value: PollGroup) {
        CLUSTER_STATE_STORE.put(key, value)
    }

    fun deleteCluster(key: Long?) {
        CLUSTER_STATE_STORE.remove(key)
    }
}
