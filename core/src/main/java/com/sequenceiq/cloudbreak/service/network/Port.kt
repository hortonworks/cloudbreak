package com.sequenceiq.cloudbreak.service.network

import java.util.ArrayList

import com.sequenceiq.cloudbreak.cloud.model.EndpointRule

class Port(val exposedService: ExposedService, val port: String, val localPort: String, val protocol: String, val aclRules: List<EndpointRule>) {

    val name: String

    constructor(exposedService: ExposedService, port: String, protocol: String) : this(exposedService, port, port, protocol, ArrayList<EndpointRule>()) {
    }

    init {
        this.name = exposedService.portName
    }
}
