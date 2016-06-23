package com.sequenceiq.cloudbreak.cloud.task

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.event.instance.InstanceConsoleOutputResult
import com.sequenceiq.cloudbreak.cloud.handler.GetSSHFingerprintsHandler
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance

@Component(PollInstanceConsoleOutputTask.NAME)
@Scope(value = "prototype")
class PollInstanceConsoleOutputTask(private val instanceConnector: InstanceConnector, authenticatedContext: AuthenticatedContext, private val instance: CloudInstance) : AbstractPollTask<InstanceConsoleOutputResult>(authenticatedContext) {

    @Throws(Exception::class)
    override fun call(): InstanceConsoleOutputResult {
        LOGGER.info("Get console output of instance: {}, for stack: {}.", instance.instanceId, authenticatedContext.cloudContext.name)
        val consoleOutput = instanceConnector.getConsoleOutput(authenticatedContext, instance)
        return InstanceConsoleOutputResult(authenticatedContext.cloudContext, instance, consoleOutput)
    }

    override fun completed(instanceConsoleOutputResult: InstanceConsoleOutputResult): Boolean {
        val output = instanceConsoleOutputResult.consoleOutput
        val contains = output.contains(CB_FINGERPRINT_END)
        if (contains) {
            return true
        }
        val fingerprints = GetSSHFingerprintsHandler.FingerprintParserUtil.parseFingerprints(output)
        return !fingerprints.isEmpty()
    }

    companion object {
        val NAME = "pollInstanceConsoleOutputTask"

        private val LOGGER = LoggerFactory.getLogger(PollInstanceConsoleOutputTask::class.java)
        private val CB_FINGERPRINT_END = "-----END SSH HOST KEY FINGERPRINTS-----"
    }
}
