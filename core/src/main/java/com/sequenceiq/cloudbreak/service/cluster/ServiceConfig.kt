package com.sequenceiq.cloudbreak.service.cluster

class ServiceConfig(val serviceName: String, val globalConfig: Map<String, List<ConfigProperty>>, val hostGroupConfig: Map<String, List<ConfigProperty>>)
