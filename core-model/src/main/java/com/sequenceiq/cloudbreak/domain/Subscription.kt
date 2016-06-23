package com.sequenceiq.cloudbreak.domain

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator

@Entity
class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "subscription_generator")
    @SequenceGenerator(name = "subscription_generator", sequenceName = "subscription_id_seq", allocationSize = 1)
    var id: Long? = null

    var clientId: String? = null

    var endpoint: String? = null

    constructor() {
    }

    constructor(clientId: String, endpoint: String) {
        this.clientId = clientId
        this.endpoint = endpoint
    }
}
