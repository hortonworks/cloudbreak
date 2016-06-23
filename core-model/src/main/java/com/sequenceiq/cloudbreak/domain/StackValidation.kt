package com.sequenceiq.cloudbreak.domain

class StackValidation : ProvisionEntity {
    var hostGroups: Set<HostGroup>? = null
    var instanceGroups: Set<InstanceGroup>? = null
    var blueprint: Blueprint? = null
    var network: Network? = null
}
