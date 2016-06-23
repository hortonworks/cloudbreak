package com.sequenceiq.cloudbreak.domain

import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
class TopologyRecord {
    @Column(columnDefinition = "TEXT")
    var hypervisor: String? = null
    @Column(columnDefinition = "TEXT")
    var rack: String? = null

    private constructor() {
    }

    constructor(hypervisor: String, rack: String) {
        this.hypervisor = hypervisor
        this.rack = rack
    }
}
