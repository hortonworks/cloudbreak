package com.sequenceiq.cloudbreak.service.cluster.flow

import com.ecwid.consul.v1.ConsulClient
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.StackContext

class ConsulKVCheckerContext(stack: Stack, val consulClient: ConsulClient, val keys: List<String>, val expectedValue: String, val failValue: String) : StackContext(stack)
