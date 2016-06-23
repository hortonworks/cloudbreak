package com.sequenceiq.cloudbreak.orchestrator.model

import java.util.HashMap

class SaltPillarConfig {

    var servicePillarConfig: Map<String, SaltPillarProperties>? = null

    constructor() {
        servicePillarConfig = HashMap<String, SaltPillarProperties>()
    }

    constructor(servicePillarConfig: Map<String, SaltPillarProperties>) {
        this.servicePillarConfig = servicePillarConfig
    }
}
