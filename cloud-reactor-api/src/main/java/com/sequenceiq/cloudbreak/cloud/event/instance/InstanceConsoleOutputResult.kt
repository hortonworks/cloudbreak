package com.sequenceiq.cloudbreak.cloud.event.instance

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance

class InstanceConsoleOutputResult(val cloudContext: CloudContext, val cloudInstance: CloudInstance, val consoleOutput: String)
