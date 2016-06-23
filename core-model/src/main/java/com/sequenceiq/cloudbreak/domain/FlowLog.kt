package com.sequenceiq.cloudbreak.domain

import java.util.Date

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator

@Entity
class FlowLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "flowlog_generator")
    @SequenceGenerator(name = "flowlog_generator", sequenceName = "flowlog_id_seq", allocationSize = 1)
    var id: Long? = null

    var stackId: Long? = null

    var created: Long? = Date().time

    var flowId: String? = null

    var nextEvent: String? = null

    @Column(length = Integer.MAX_VALUE, columnDefinition = "TEXT")
    var payload: String? = null

    var payloadType: Class<*>? = null

    var flowType: Class<*>? = null

    var currentState: String? = null

    var finalized: Boolean? = false

    constructor() {

    }

    constructor(stackId: Long?, flowId: String, currentState: String, finalized: Boolean?) {
        this.stackId = stackId
        this.flowId = flowId
        this.currentState = currentState
        this.finalized = finalized
    }

    constructor(stackId: Long?, flowId: String, nextEvent: String, payload: String, payloadType: Class<*>, flowType: Class<*>, currentState: String) {
        this.stackId = stackId
        this.flowId = flowId
        this.nextEvent = nextEvent
        this.payload = payload
        this.payloadType = payloadType
        this.flowType = flowType
        this.currentState = currentState
    }
}
