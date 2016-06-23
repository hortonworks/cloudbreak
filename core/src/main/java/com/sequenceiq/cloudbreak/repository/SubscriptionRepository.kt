package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository

import com.sequenceiq.cloudbreak.domain.Subscription

@EntityType(entityClass = Subscription::class)
interface SubscriptionRepository : CrudRepository<Subscription, Long> {

    fun findByClientId(clientId: String): List<Subscription>
}
