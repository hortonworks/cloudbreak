package com.sequenceiq.cloudbreak.cloud.model

class Port(val name: String, val port: String, val localPort: String, val protocol: String, val aclRules: List<EndpointRule>)
