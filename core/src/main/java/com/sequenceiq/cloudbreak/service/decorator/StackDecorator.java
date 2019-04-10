package com.sequenceiq.cloudbreak.service.decorator;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.AdjustmentType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.SharedServiceValidator;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Service
public class StackDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDecorator.class);

    private static final double ONE_HUNDRED = 100.0;

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
    private ConverterUtil converterUtil;

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CloudParameterCache cloudParameterCache;

    @Inject
    private SharedServiceValidator sharedServiceValidator;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Measure(StackDecorator.class)
    public Stack decorate(@Nonnull Stack subject, @Nonnull StackV4Request request, User user, Workspace workspace) {
        setEnvironment(subject, request, workspace);

        String stackName = request.getName();
        measure(() -> prepareCredential(subject, request, workspace),
                LOGGER, "Credential was prepared under {} ms for stack {}", stackName);

        measure(() -> prepareDomainIfDefined(subject, request, user, workspace),
                LOGGER, "Domain was prepared under {} ms for stack {}", stackName);

        subject.setCloudPlatform(subject.getCredential().cloudPlatform());
        if (subject.getInstanceGroups() == null) {
            throw new BadRequestException("Instance groups must be specified!");
        }

        measure(() -> {
            PlatformParameters pps = cloudParameterCache.getPlatformParameters().get(Platform.platform(subject.cloudPlatform()));
            Boolean mandatoryNetwork = pps.specialParameters().getSpecialParameters().get(PlatformParametersConsts.NETWORK_IS_MANDATORY);
            if (BooleanUtils.isTrue(mandatoryNetwork) && subject.getNetwork() == null) {
                throw new BadRequestException("Network must be specified!");
            }
        }, LOGGER, "Network was prepared and validated under {} ms for stack {}", stackName);

        measure(() -> prepareOrchestratorIfNotExist(subject, subject.getCredential()),
                LOGGER, "Orchestrator was prepared under {} ms for stack {}", stackName);

        if (subject.getFailurePolicy() != null) {
            measure(() -> validatFailurePolicy(subject, subject.getFailurePolicy()),
                    LOGGER, "Failure policy was validated under {} ms for stack {}", stackName);
        }

        measure(() -> prepareInstanceGroups(subject, request, subject.getCredential(), user),
                LOGGER, "Instance groups were prepared under {} ms for stack {}", stackName);

        measure(() -> validateInstanceGroups(subject),
                LOGGER, "Validation of gateway instance groups has been finished in {} ms for stack {}", stackName);

        measure(() -> {
            ValidationResult validationResult = sharedServiceValidator.checkSharedServiceStackRequirements(request, workspace);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }, LOGGER, "Validation of shared services requirements has been finished in {} ms for stack {}", stackName);

        return subject;
    }

    private void setEnvironment(@Nonnull Stack subject, @Nonnull StackV4Request request, Workspace workspace) {
        if (!StringUtils.isEmpty(request.getEnvironment().getName())) {
            EnvironmentView environment = environmentViewService.getByNameForWorkspace(request.getEnvironment().getName(), workspace);
            subject.setEnvironment(environment);
        }
    }

    private void prepareCredential(Stack subject, StackV4Request request, Workspace workspace) {
        if (subject.getEnvironment() != null) {
            subject.setCredential(subject.getEnvironment().getCredential());
        } else if (subject.getCredential() == null) {
            if (request.getEnvironment().getCredentialName() != null) {
                Credential credential = credentialService.getByNameForWorkspace(request.getEnvironment().getCredentialName(), workspace);
                subject.setCredential(credential);
            }
        }
    }

    private void prepareInstanceGroups(Stack subject, StackV4Request request, Credential credential, User user) {
        Map<String, InstanceGroupParameterResponse> instanceGroupParameterResponse = cloudParameterService
                .getInstanceGroupParameters(credential, getInstanceGroupParameterRequests(subject));
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        subject.getInstanceGroups().parallelStream().forEach(instanceGroup -> {
            restRequestThreadLocalService.setCloudbreakUser(cloudbreakUser);
            updateInstanceGroupParameters(instanceGroupParameterResponse, instanceGroup);
            if (instanceGroup.getTemplate() != null) {
                Template template = instanceGroup.getTemplate();
                if (template.getId() == null) {
                    template.setCloudPlatform(getCloudPlatform(subject, request, template.cloudPlatform()));
                    PlacementSettingsV4Request placement = request.getPlacement();
                    String availabilityZone = placement != null ? placement.getAvailabilityZone() : null;
                    String region = placement != null ? placement.getRegion() : null;

                    templateValidator.validateTemplateRequest(credential, template, region, availabilityZone, subject.getPlatformVariant());
                    template = templateDecorator.decorate(credential, template, region, availabilityZone, subject.getPlatformVariant());
                    template.setWorkspace(subject.getWorkspace());
                    template = templateService.create(user, template);
                    instanceGroup.setTemplate(template);
                }
            }
            if (instanceGroup.getSecurityGroup() != null) {
                SecurityGroup securityGroup = instanceGroup.getSecurityGroup();
                if (securityGroup.getId() == null) {
                    securityGroup.setCloudPlatform(getCloudPlatform(subject, request, securityGroup.getCloudPlatform()));
                    securityGroup.setWorkspace(subject.getWorkspace());
                    securityGroup = securityGroupService.create(user, securityGroup);
                    instanceGroup.setSecurityGroup(securityGroup);
                }
            }
        });
    }

    private Set<InstanceGroupParameterRequest> getInstanceGroupParameterRequests(Stack subject) {
        Set<InstanceGroupParameterRequest> instanceGroupParameterRequests = new HashSet<>();
        for (InstanceGroup instanceGroup : subject.getInstanceGroups()) {
            InstanceGroupParameterRequest convert = converterUtil.convert(instanceGroup, InstanceGroupParameterRequest.class);
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
                LOGGER.info("Could not update '{}' instancegroup parameters with defaults.", instanceGroupParameterResponse.getGroupName());
            }
        }
    }

    private void prepareDomainIfDefined(Stack subject, StackV4Request request, User user, Workspace workspace) {
        if (subject.getNetwork() != null) {
            Network network = subject.getNetwork();
            if (network.getId() == null) {
                network.setCloudPlatform(getCloudPlatform(subject, request, network.cloudPlatform()));
                network = networkService.create(network, subject.getWorkspace());
            }
            subject.setNetwork(network);
        }
        if (subject.getCredential() != null) {
            Credential credentialForStack = subject.getCredential();
            if (credentialForStack.getId() == null) {
                credentialForStack.setCloudPlatform(getCloudPlatform(subject, request, credentialForStack.cloudPlatform()));
                credentialForStack = credentialService.create(credentialForStack, workspace, user);
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

    private void validateInstanceGroups(Stack stack) {
        long instanceGroups = stack.getInstanceGroups().stream().filter(ig -> InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType())).count();
        if (instanceGroups == 0L) {
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

    private String getCloudPlatform(Stack stack, StackV4Request request, String cloudPlatform) {
        if (!Strings.isNullOrEmpty(cloudPlatform)) {
            return cloudPlatform;
        } else if (stack.getCredential() != null && stack.getCredential().getId() != null) {
            return stack.getCredential().cloudPlatform();
        } else if (Strings.isNullOrEmpty(stack.cloudPlatform())) {
            return stack.cloudPlatform();
        }
        return request.getCloudPlatform().name();
    }
}