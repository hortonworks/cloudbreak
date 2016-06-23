package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId.jobId
import java.util.stream.Collectors

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.common.collect.Multimap
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates

class SaltJobIdTracker(private val saltConnector: SaltConnector, private val saltJobRunner: SaltJobRunner) : OrchestratorBootstrap {

    @Throws(Exception::class)
    override fun call(): Boolean? {
        if (JobState.NOT_STARTED == saltJobRunner.jobState) {
            LOGGER.info("Job has not started in the cluster. Starting for first time.")
            val jobIdObject = jobId(saltJobRunner.submit(saltConnector))
            val jobId = jobIdObject.jobId
            saltJobRunner.jid = jobIdObject
            checkIsFinished(jobId)
        } else if (JobState.IN_PROGRESS == saltJobRunner.jobState) {
            val jobId = saltJobRunner.jid.jobId
            LOGGER.info("Job: {} is running currently checking the current state.", jobId)
            checkIsFinished(jobId)
        } else if (JobState.FAILED == saltJobRunner.jobState) {
            val jobId = saltJobRunner.jid.jobId
            LOGGER.info("Job: {} failed in the previous time. Trigger again with these targets: {}", jobId, saltJobRunner.target)
            saltJobRunner.jid = jobId(saltJobRunner.submit(saltConnector))
            saltJobRunner.jobState = JobState.IN_PROGRESS
            return call()
        }
        if (JobState.IN_PROGRESS == saltJobRunner.jobState) {
            val jobIsRunningMessage = String.format("Job: %s is running currently, waiting for next polling attempt", saltJobRunner.jid)
            throw CloudbreakOrchestratorFailedException(jobIsRunningMessage)
        }
        if (JobState.FAILED == saltJobRunner.jobState) {
            throw CloudbreakOrchestratorFailedException(buildErrorMessage())
        }
        LOGGER.info("Job (jid: {}) was finished. Triggering next salt event.", saltJobRunner.jid.jobId)
        return true
    }

    private fun checkIsFinished(jobId: String) {
        val jobRunning = SaltStates.jobIsRunning(saltConnector, jobId, Compound(saltJobRunner.target))
        if (jobRunning) {
            LOGGER.info("Job: {} is running currently, waiting for next polling attempt.", jobId)
            saltJobRunner.jobState = JobState.IN_PROGRESS
        } else {
            LOGGER.info("Job finished: {}. Collecting missing nodes", jobId)
            checkJobFinishedWithSuccess()
        }
    }

    private fun buildErrorMessage(): String {
        val jobId = saltJobRunner.jid.jobId
        val errorMessageBuilder = StringBuilder()
        errorMessageBuilder.append(String.format("There are missing nodes from job (jid: %s), target: %s", jobId, saltJobRunner.target))
        if (saltJobRunner.jobState.nodesWithError != null) {
            for (host in saltJobRunner.jobState.nodesWithError.keySet()) {
                val errorMessages = saltJobRunner.jobState.nodesWithError.get(host)
                errorMessageBuilder.append("\n").append("Node: ").append(host).append(" Error(s): ").append(errorMessages.joinToString(" | "))
            }
        }
        return errorMessageBuilder.toString()
    }

    private fun checkJobFinishedWithSuccess() {
        val jobId = saltJobRunner.jid.jobId
        val missingNodesWithReason = SaltStates.jidInfo(saltConnector, jobId, Compound(saltJobRunner.target),
                saltJobRunner.stateType())
        if (!missingNodesWithReason.isEmpty) {
            LOGGER.info("There are missing nodes after the job (jid: {}) completion: {}", jobId, missingNodesWithReason.keySet().joinToString(","))
            val jobState = JobState.FAILED
            jobState.nodesWithError = missingNodesWithReason
            saltJobRunner.jobState = JobState.FAILED
            val newTargets = missingNodesWithReason.keySet().stream().map({ node -> SaltStates.resolveHostNameToMinionHostName(saltConnector, node) }).collect(Collectors.toSet<String>())
            saltJobRunner.target = newTargets
        } else {
            LOGGER.info("The job (jid: {}) completed successfully on every node.", jobId)
            saltJobRunner.jobState = JobState.FINISHED
        }
    }

    override fun toString(): String {
        return "SaltJobIdTracker{"
        +"saltJobRunner=" + saltJobRunner
        +'}'
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(SaltJobIdTracker::class.java)
    }
}
