package com.sequenceiq.cloudbreak.service.stack.flow

import com.ecwid.consul.v1.ConsulClient
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.StackContext

class ConsulContext(stack: Stack, val consulClient: ConsulClient, val targets: List<String>) : StackContext(stack)
