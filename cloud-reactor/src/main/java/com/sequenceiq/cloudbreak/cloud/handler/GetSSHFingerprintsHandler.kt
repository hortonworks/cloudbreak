package com.sequenceiq.cloudbreak.cloud.handler

import java.util.HashSet
import java.util.regex.Matcher
import java.util.regex.Pattern

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult
import com.sequenceiq.cloudbreak.cloud.event.instance.InstanceConsoleOutputResult
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class GetSSHFingerprintsHandler : CloudPlatformEventHandler<GetSSHFingerprintsRequest<Any>> {

    @Inject
    private val statusCheckFactory: PollTaskFactory? = null
    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<InstanceConsoleOutputResult>? = null
    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<GetSSHFingerprintsRequest<Any>> {
        return GetSSHFingerprintsRequest<Any>::class.java
    }

    override fun accept(getSSHFingerprintsRequestEvent: Event<GetSSHFingerprintsRequest<Any>>) {
        LOGGER.info("Received event: {}", getSSHFingerprintsRequestEvent)
        val fingerprintsRequest = getSSHFingerprintsRequestEvent.data
        try {
            val cloudContext = fingerprintsRequest.cloudContext
            val cloudInstance = fingerprintsRequest.cloudInstance
            val connector = cloudPlatformConnectors!!.get(cloudContext.platformVariant)
            val ac = connector.authentication().authenticate(cloudContext, fingerprintsRequest.cloudCredential)
            val fingerprintsResult: GetSSHFingerprintsResult
            try {
                val initialConsoleOutput = connector.instances().getConsoleOutput(ac, cloudInstance)
                var consoleOutputResult = InstanceConsoleOutputResult(cloudContext, cloudInstance, initialConsoleOutput)
                val outputPollerTask = statusCheckFactory!!.newPollConsoleOutputTask(connector.instances(), ac, cloudInstance)
                if (!outputPollerTask.completed(consoleOutputResult)) {
                    consoleOutputResult = syncPollingScheduler!!.schedule(outputPollerTask)
                }
                val sshFingerprints = FingerprintParserUtil.parseFingerprints(consoleOutputResult.consoleOutput)
                if (sshFingerprints.isEmpty()) {
                    throw RuntimeException("Failed to get SSH fingerprints from the specified VM instance.")
                } else {
                    fingerprintsResult = GetSSHFingerprintsResult(fingerprintsRequest, sshFingerprints)
                }
            } catch (e: CloudOperationNotSupportedException) {
                fingerprintsResult = GetSSHFingerprintsResult(fingerprintsRequest, HashSet<String>())
            }

            fingerprintsRequest.result.onNext(fingerprintsResult)
            eventBus!!.notify(fingerprintsResult.selector(), Event(getSSHFingerprintsRequestEvent.headers, fingerprintsResult))
            LOGGER.info("GetSSHFingerprintsHandler finished")
        } catch (e: Exception) {
            val failure = GetSSHFingerprintsResult("Failed to get ssh fingerprints!", e, fingerprintsRequest)
            fingerprintsRequest.result.onNext(failure)
            eventBus!!.notify(failure.selector(), Event(getSSHFingerprintsRequestEvent.headers, failure))
        }

    }

    //TODO remove it from here and core and move it to a common module
    object FingerprintParserUtil {

        private val LOGGER = LoggerFactory.getLogger(FingerprintParserUtil::class.java)

        private val FINGERPRINT_PATTERNS = arrayOf(Pattern.compile("(?<fingerprint>([a-f0-9]{2}:){15,}[a-f0-9]{2}).*ECDSA"), Pattern.compile("(?<fingerprint>([a-f0-9]{2}:){15,}[a-f0-9]{2}).*RSA"))

        fun parseFingerprints(consoleLog: String): Set<String> {
            LOGGER.debug("Received console log: {}", consoleLog)
            val matchedFingerprints = HashSet<String>()
            val lines = consoleLog.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            for (line in lines) {
                for (pattern in FINGERPRINT_PATTERNS) {
                    val m = pattern.matcher(line)
                    if (m.find()) {
                        matchedFingerprints.add(m.group("fingerprint"))
                    }
                }
            }
            return matchedFingerprints
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GetSSHFingerprintsHandler::class.java)
    }
}
