package com.sequenceiq.cloudbreak.service.decorator;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

@Service
public class StackDecorator implements Decorator<Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDecorator.class);

    private static final double ONE_HUNDRED = 100.0;

    @Inject
    private CredentialService credentialService;

    @Inject
    private NetworkService networkService;

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private CloudParameterService cloudParameterService;

    private enum DecorationData {
        CREDENTIAL_ID,
        NETWORK_ID
    }

    @Override
    public Stack decorate(Stack subject, Object... data) {
        Object credentialId = data[DecorationData.CREDENTIAL_ID.ordinal()];
        if (credentialId != null) {
            Object networkId = data[DecorationData.NETWORK_ID.ordinal()];
            if (subject.getInstanceGroups() == null || networkId == null) {
                throw new BadRequestException("Instance groups and network must be specified!");
            }
            Credential credential = credentialService.get((Long) credentialId);
            subject.setCloudPlatform(credential.cloudPlatform());
            subject.setCredential(credential);

            subject.setNetwork(networkService.getById((Long) networkId));
            if (subject.getOrchestrator() != null && (subject.getOrchestrator().getApiEndpoint() != null || subject.getOrchestrator().getType() == null)) {
                throw new BadRequestException("Orchestrator cannot be configured for the stack!");
            }
            prepareOrchestratorIfNotExist(subject, credential);
            if (subject.getFailurePolicy() != null) {
                validatFailurePolicy(subject, subject.getFailurePolicy());
            }

            validate(subject);
        } else {
            subject.setCloudPlatform("BYOS");
            if (subject.getOrchestrator() == null) {
                throw new BadRequestException("If credential is not provided, orchestrator details cannot be empty.");
            }
        }
        return subject;
    }

    private void prepareOrchestratorIfNotExist(Stack subject, Credential credential) {
        if (subject.getOrchestrator() == null) {
            PlatformOrchestrators orchestrators = cloudParameterService.getOrchestrators();
            Orchestrator orchestrator = orchestrators.getDefaults().get(Platform.platform(credential.cloudPlatform()));
            com.sequenceiq.cloudbreak.domain.Orchestrator orchestratorObject = new com.sequenceiq.cloudbreak.domain.Orchestrator();
            orchestratorObject.setType(orchestrator.value());
            subject.setOrchestrator(orchestratorObject);
        }
    }

    private void validate(Stack stack) {
        if (stack.getGatewayInstanceGroup() == null) {
            throw new BadRequestException("Gateway instance group not configured");
        }
        int minNodeCount = ConsulUtils.ConsulServers.SINGLE_NODE_COUNT_LOW.getMin();
        int fullNodeCount = stack.getFullNodeCount();
        if (fullNodeCount < minNodeCount) {
            throw new BadRequestException(String.format("At least %s nodes are required to launch the stack", minNodeCount));
        }
    }

    private int getConsulServerCount(Integer userDefinedConsulServers, int fullNodeCount) {
        int consulServers = userDefinedConsulServers == null ? ConsulUtils.getConsulServerCount(fullNodeCount) : userDefinedConsulServers;
        if (consulServers > fullNodeCount || consulServers < 1) {
            throw new BadRequestException("Invalid consul server specification: must be in range 1-" + fullNodeCount);
        }
        return consulServers;
    }

    private void validatFailurePolicy(Stack stack, FailurePolicy failurePolicy) {
        if (failurePolicy.getThreshold() == 0L && !AdjustmentType.BEST_EFFORT.equals(failurePolicy.getAdjustmentType())) {
            throw new BadRequestException("The threshold can not be 0");
        }
        if (AdjustmentType.EXACT.equals(failurePolicy.getAdjustmentType())) {
            validateExactCount(stack, failurePolicy);
        } else if (AdjustmentType.PERCENTAGE.equals(failurePolicy.getAdjustmentType())) {
            validatePercentageCount(failurePolicy);
        }
    }

    private void validatePercentageCount(FailurePolicy failurePolicy) {
        if (failurePolicy.getThreshold() < 0L || failurePolicy.getThreshold() > ONE_HUNDRED) {
            throw new BadRequestException("The percentage of the threshold has to be between 0 an 100.");
        }
    }

    private void validateExactCount(Stack stack, FailurePolicy failurePolicy) {
        if (failurePolicy.getThreshold() > stack.getFullNodeCount()) {
            throw new BadRequestException("Threshold can not be higher than the node count of the stack.");
        }
    }

}
