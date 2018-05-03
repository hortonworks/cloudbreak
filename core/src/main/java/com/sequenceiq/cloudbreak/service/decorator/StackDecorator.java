package com.sequenceiq.cloudbreak.service.decorator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackValidator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.StackParameterService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Service
public class StackDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDecorator.class);

    private static final double ONE_HUNDRED = 100.0;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private StackValidator stackValidator;

    @Inject
    private StackParameterService stackParameterService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private NetworkService networkService;

    @Inject
    private TemplateService templateService;

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private TemplateDecorator templateDecorator;

    @Inject
    private TemplateValidator templateValidator;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private CloudParameterCache cloudParameterCache;

    public Stack decorate(Stack subject, StackRequest request, IdentityUser user) {
        prepareCredential(subject, request, user);
        prepareDomainIfDefined(subject, request, user);
        Long credentialId = request.getCredentialId();
        String credentialName = request.getCredentialName();
        if (credentialId != null || subject.getCredential() != null || credentialName != null) {
            subject.setCloudPlatform(subject.getCredential().cloudPlatform());
            if (subject.getInstanceGroups() == null) {
                throw new BadRequestException("Instance groups must be specified!");
            }
            PlatformParameters pps = cloudParameterCache.getPlatformParameters().get(Platform.platform(subject.cloudPlatform()));
            Boolean mandatoryNetwork = pps.specialParameters().getSpecialParameters().get(PlatformParametersConsts.NETWORK_IS_MANDATORY);
            if (BooleanUtils.isTrue(mandatoryNetwork) && request.getNetworkId() == null && subject.getNetwork() == null) {
                throw new BadRequestException("Network must be specified!");
            }
            prepareNetwork(subject, request.getNetworkId());
            prepareOrchestratorIfNotExist(subject, subject.getCredential());
            if (subject.getFailurePolicy() != null) {
                validatFailurePolicy(subject, subject.getFailurePolicy());
            }
            prepareInstanceGroups(subject, request, subject.getCredential(), user);
            prepareFlexSubscription(subject, request.getFlexId());
            validate(subject);
        }
        return subject;
    }

    private void prepareCredential(Stack subject, StackRequest request, IdentityUser user) {
        if (subject.getCredential() == null) {
            if (request.getCredentialId() != null) {
                Credential credential = credentialService.get(request.getCredentialId());
                subject.setCredential(credential);
            }
            if (request.getCredentialName() != null) {
                Credential credential = credentialService.getPublicCredential(request.getCredentialName(), user);
                subject.setCredential(credential);
            }
        }
        subject.setParameters(getValidParameters(subject, request));
    }

    private Map<String, String> getValidParameters(Stack stack, StackRequest stackRequest) {
        Map<String, String> params = new HashMap<>();
        Map<String, String> userParams = stackRequest.getParameters();
        if (userParams != null) {
            List<StackParamValidation> stackParams = stackParameterService.getStackParams(stackRequest.getName(), stack.getCredential());
            for (StackParamValidation stackParamValidation : stackParams) {
                String paramName = stackParamValidation.getName();
                String value = userParams.get(paramName);
                if (value != null) {
                    params.put(paramName, value);
                }
            }
            stackValidator.validate(params, stackParams);
        }
        return params;
    }

    private void prepareNetwork(Stack subject, Object networkId) {
        if (networkId != null) {
            subject.setNetwork(networkService.getById((Long) networkId));
            if (subject.getOrchestrator() != null && (subject.getOrchestrator().getApiEndpoint() != null || subject.getOrchestrator().getType() == null)) {
                throw new BadRequestException("Orchestrator cannot be configured for the stack!");
            }
        }
    }

    private void prepareInstanceGroups(Stack subject, StackRequest request, Credential credential, IdentityUser user) {
        Map<String, InstanceGroupParameterResponse> instanceGroupParameterResponse = cloudParameterService
                .getInstanceGroupParameters(credential, getInstanceGroupParameterRequests(subject));
        for (InstanceGroup instanceGroup : subject.getInstanceGroups()) {
            updateInstanceGroupParameters(instanceGroupParameterResponse, instanceGroup);
            if (instanceGroup.getTemplate() != null) {
                Template template = instanceGroup.getTemplate();
                if (template.getId() == null) {
                    template.setPublicInAccount(subject.isPublicInAccount());
                    template.setCloudPlatform(getCloudPlatform(subject, request, template.cloudPlatform()));
                    templateValidator.validateTemplateRequest(credential, instanceGroup.getTemplate(), request.getRegion(),
                            request.getAvailabilityZone(), request.getPlatformVariant());
                    template = templateDecorator.decorate(credential, template, request.getRegion(),
                            request.getAvailabilityZone(), request.getPlatformVariant());
                    template = templateService.create(user, template);
                }
                instanceGroup.setTemplate(template);
            }
            if (instanceGroup.getSecurityGroup() != null) {
                SecurityGroup securityGroup = instanceGroup.getSecurityGroup();
                if (securityGroup.getId() == null) {
                    securityGroup.setPublicInAccount(subject.isPublicInAccount());
                    securityGroup.setCloudPlatform(getCloudPlatform(subject, request, securityGroup.getCloudPlatform()));
                    securityGroup = securityGroupService.create(user, securityGroup);
                    instanceGroup.setSecurityGroup(securityGroup);
                }
            }
        }
    }

    private Set<InstanceGroupParameterRequest> getInstanceGroupParameterRequests(Stack subject) {
        Set<InstanceGroupParameterRequest> instanceGroupParameterRequests = new HashSet<>();
        for (InstanceGroup instanceGroup : subject.getInstanceGroups()) {
            InstanceGroupParameterRequest convert = conversionService.convert(instanceGroup, InstanceGroupParameterRequest.class);
            convert.setStackName(subject.getName());
            instanceGroupParameterRequests.add(convert);
        }
        return instanceGroupParameterRequests;
    }

    private void updateInstanceGroupParameters(Map<String, InstanceGroupParameterResponse> instanceGroupParameterResponses, InstanceGroup instanceGroup) {
        InstanceGroupParameterResponse instanceGroupParameterResponse = instanceGroupParameterResponses.get(instanceGroup.getGroupName());
        if (instanceGroupParameterResponse != null) {
            try {
                Json jsonProperties = new Json(instanceGroupParameterResponse.getParameters());
                instanceGroup.setAttributes(jsonProperties);
            } catch (JsonProcessingException e) {
                LOGGER.error("Could not update '{}' instancegroup parameters with defaults.", instanceGroupParameterResponse.getGroupName());
            }
        }
    }

    private void prepareDomainIfDefined(Stack subject, StackRequest request, IdentityUser user) {
        if (subject.getNetwork() != null) {
            Network network = subject.getNetwork();
            if (network.getId() == null) {
                network.setPublicInAccount(subject.isPublicInAccount());
                network.setCloudPlatform(getCloudPlatform(subject, request, network.cloudPlatform()));
                network = networkService.create(user, network);
            }
            subject.setNetwork(network);
        }
        if (subject.getCredential() != null) {
            Credential credentialForStack = subject.getCredential();
            if (credentialForStack.getId() == null) {
                credentialForStack.setPublicInAccount(subject.isPublicInAccount());
                credentialForStack.setCloudPlatform(getCloudPlatform(subject, request, credentialForStack.cloudPlatform()));
                credentialForStack = credentialService.create(user, credentialForStack);
            }
            subject.setCredential(credentialForStack);
        }
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
        long instanceGroups = stack.getInstanceGroups().stream().filter(ig -> InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType())).count();
        if (instanceGroups == 0) {
            throw new BadRequestException("Gateway instance group not configured");
        }
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

    private void prepareFlexSubscription(Stack subject, Long flexId) {
        if (flexId != null) {
            FlexSubscription flexSubscription = flexSubscriptionService.findOneById(flexId);
            subject.setFlexSubscription(flexSubscription);
        }
    }

    private String getCloudPlatform(Stack stack, StackRequest request, String cloudPlatform) {
        if (!Strings.isNullOrEmpty(cloudPlatform)) {
            return cloudPlatform;
        } else if (stack.getCredential() != null && stack.getCredential().getId() != null) {
            return stack.getCredential().cloudPlatform();
        } else if (Strings.isNullOrEmpty(stack.cloudPlatform()))  {
            return stack.cloudPlatform();
        } else {
            return request.getCloudPlatform();
        }
    }



}