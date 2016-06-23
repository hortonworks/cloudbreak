package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel

class Network : DynamicModel {

    val subnet: Subnet

    constructor(subnet: Subnet) {
        this.subnet = subnet
    }

    constructor(subnet: Subnet, parameters: MutableMap<String, Any>) : super(parameters) {
        this.subnet = subnet
    }


}
