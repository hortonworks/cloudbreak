package com.sequenceiq.cloudbreak.service.decorator

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.FailurePolicy
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.credential.CredentialService
import com.sequenceiq.cloudbreak.service.network.NetworkService
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils

@Service
class StackDecorator : Decorator<Stack> {

    @Inject
    private val credentialService: CredentialService? = null

    @Inject
    private val networkService: NetworkService? = null

    @Inject
    private val securityGroupService: SecurityGroupService? = null

    @Inject
    private val cloudParameterService: CloudParameterService? = null

    private enum class DecorationData {
        CREDENTIAL_ID,
        NETWORK_ID,
        SECURITY_GROUP_ID
    }

    override fun decorate(subject: Stack, vararg data: Any): Stack {
        val credentialId = data[DecorationData.CREDENTIAL_ID.ordinal]
        if (credentialId != null) {
            val securityGroupId = data[DecorationData.SECURITY_GROUP_ID.ordinal]
            val networkId = data[DecorationData.NETWORK_ID.ordinal]
            if (subject.instanceGroups == null || networkId == null) {
                throw BadRequestException("Instance groups and network must be specified!")
            }
            val credential = credentialService!!.get(credentialId as Long?)
            subject.setCloudPlatform(credential.cloudPlatform())
            subject.credential = credential
            if (securityGroupId != null) {
                subject.securityGroup = securityGroupService!!.get(securityGroupId as Long?)
            }

            subject.network = networkService!!.getById(networkId as Long?)
            if (subject.orchestrator != null && (subject.orchestrator.apiEndpoint != null || subject.orchestrator.type == null)) {
                throw BadRequestException("Orchestrator cannot be configured for the stack!")
            }
            prepareOrchestratorIfNotExist(subject, credential)
            if (subject.failurePolicy != null) {
                validatFailurePolicy(subject, subject.failurePolicy)
            }
            validate(subject)
        } else {
            subject.setCloudPlatform("BYOS")
            if (subject.orchestrator == null) {
                throw BadRequestException("If credential is not provided, orchestrator details cannot be empty.")
            }
        }
        return subject
    }

    private fun prepareOrchestratorIfNotExist(subject: Stack, credential: Credential) {
        if (subject.orchestrator == null) {
            val orchestrators = cloudParameterService!!.orchestrators
            val orchestrator = orchestrators.defaults.get(credential.cloudPlatform())
            val orchestratorObject = com.sequenceiq.cloudbreak.domain.Orchestrator()
            orchestratorObject.type = orchestrator.value()
            subject.orchestrator = orchestratorObject
        }
    }


    private fun validate(stack: Stack) {
        if (stack.gatewayInstanceGroup == null) {
            throw BadRequestException("Gateway instance group not configured")
        }
        val minNodeCount = ConsulUtils.ConsulServers.SINGLE_NODE_COUNT_LOW.min
        val fullNodeCount = stack.fullNodeCount!!
        if (fullNodeCount < minNodeCount) {
            throw BadRequestException(String.format("At least %s nodes are required to launch the stack", minNodeCount))
        }
    }

    private fun getConsulServerCount(userDefinedConsulServers: Int?, fullNodeCount: Int): Int {
        val consulServers = userDefinedConsulServers ?: ConsulUtils.getConsulServerCount(fullNodeCount)
        if (consulServers > fullNodeCount || consulServers < 1) {
            throw BadRequestException("Invalid consul server specification: must be in range 1-" + fullNodeCount)
        }
        return consulServers
    }

    private fun validatFailurePolicy(stack: Stack, failurePolicy: FailurePolicy) {
        if (failurePolicy.threshold === 0L && AdjustmentType.BEST_EFFORT != failurePolicy.adjustmentType) {
            throw BadRequestException("The threshold can not be 0")
        }
        if (AdjustmentType.EXACT == failurePolicy.adjustmentType) {
            validateExactCount(stack, failurePolicy)
        } else if (AdjustmentType.PERCENTAGE == failurePolicy.adjustmentType) {
            validatePercentageCount(failurePolicy)
        }
    }

    private fun validatePercentageCount(failurePolicy: FailurePolicy) {
        if (failurePolicy.threshold < 0L || failurePolicy.threshold > ONE_HUNDRED) {
            throw BadRequestException("The percentage of the threshold has to be between 0 an 100.")
        }
    }

    private fun validateExactCount(stack: Stack, failurePolicy: FailurePolicy) {
        if (failurePolicy.threshold > stack.fullNodeCount) {
            throw BadRequestException("Threshold can not be higher than the node count of the stack.")
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(StackDecorator::class.java)

        private val ONE_HUNDRED = 100.0
    }

}
