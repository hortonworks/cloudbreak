package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.converter.util.ExceptionMessageFormatterUtil.formatAccessDeniedMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateValidator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.EnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class StackValidationV4RequestToStackValidationConverter extends AbstractConversionServiceAwareConverter<StackValidationV4Request, StackValidation> {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private NetworkService networkService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private CloudParameterCache cloudParameterCache;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private InstanceTemplateValidator instanceTemplateValidator;

    @Override
    public StackValidation convert(StackValidationV4Request stackValidationRequest) {
        StackValidation stackValidation = new StackValidation();
        Set<InstanceGroup> instanceGroups = converterUtil.convertAllAsSet(stackValidationRequest.getInstanceGroups(), InstanceGroup.class);
        stackValidation.setInstanceGroups(instanceGroups);
        stackValidation.setEnvironmentCrn(stackValidationRequest.getEnvironmentCrn());
        stackValidation.setHostGroups(convertHostGroupsFromJson(instanceGroups, stackValidationRequest.getHostGroups()));

        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        formatAccessDeniedMessage(
                () -> validateBlueprint(stackValidationRequest, stackValidation, workspace),
                "blueprint", stackValidationRequest.getBlueprintName()
        );
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stackValidation.getEnvironmentCrn());
        CredentialResponse credentialResponse = environment.getCredential();
        formatAccessDeniedMessage(
                () -> validateCredential(stackValidation, credentialResponse),
                "credential", Optional.ofNullable(credentialResponse).map(CredentialResponse::getName).orElse("NULL")
        );
        formatAccessDeniedMessage(
                () -> validateNetwork(stackValidationRequest.getNetworkId(), stackValidationRequest.getNetwork(), stackValidation, environment),
                "network",
                stackValidationRequest.getNetworkId()
        );
        validateInstanceTemplates(instanceGroups);
        return stackValidation;
    }

    private void validateBlueprint(StackValidationV4Request stackValidationRequest, StackValidation stackValidation, Workspace workspace) {
        Set<Blueprint> allAvailableInWorkspace = blueprintService.getAllAvailableInWorkspace(workspace);
        if (stackValidationRequest.getBlueprintName() == null) {
            throw new BadRequestException("Cluster definition is not configured for the validation request!");
        }
        if (stackValidationRequest.getBlueprintName() != null) {
            selectBlueprint(allAvailableInWorkspace, stackValidation, cd -> cd.getName().equals(stackValidationRequest.getBlueprintName()));
        }
    }

    private void validateCredential(StackValidation stackValidation, CredentialResponse credentialResponse) {
        if (credentialResponse != null) {
            Credential credential = credentialConverter.convert(credentialResponse);
            stackValidation.setCredential(credential);
        } else if (stackValidation.getCredential() == null) {
            throw new BadRequestException("Credential is not configured for the validation request!");
        }
    }

    private void validateNetwork(Long networkId, NetworkV4Request networkRequest, StackValidation stackValidation,
            DetailedEnvironmentResponse environment) {
        SpecialParameters specialParameters =
                cloudParameterCache.getPlatformParameters().get(Platform.platform(stackValidation.getCredential().cloudPlatform())).specialParameters();
        if (networkId != null) {
            Network network = networkService.get(networkId);
            stackValidation.setNetwork(network);
        } else {
            if (environment != null && environment.getNetwork() != null) {
                CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
                EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(cloudPlatform);
                if (environmentNetworkConverter != null) {
                    // we don't use subnets in the validation, so we set the first availability zone
                    Network network = environmentNetworkConverter.convertToLegacyNetwork(environment.getNetwork(),
                            getAvailabilityZoneIfAvailableOtherwiseThrowException(environment));
                    stackValidation.setNetwork(network);
                }
            } else if (networkRequest != null) {
                Network network = converterUtil.convert(networkRequest, Network.class);
                stackValidation.setNetwork(network);
            } else if (specialParameters.getSpecialParameters().get(PlatformParametersConsts.NETWORK_IS_MANDATORY)) {
                throw new BadRequestException("Network is not configured for the validation request!");
            }
        }
    }

    private String getAvailabilityZoneIfAvailableOtherwiseThrowException(DetailedEnvironmentResponse environment) {
        Optional<CloudSubnet> cloudSubnet = environment.getNetwork().getSubnetMetas().values().stream().findFirst();
        if (cloudSubnet.isPresent()) {
            return cloudSubnet.get().getAvailabilityZone();
        }
        throw new IllegalStateException("No cloud subnet found for environment: " + environment.getName());
    }

    private Set<HostGroup> convertHostGroupsFromJson(Collection<InstanceGroup> instanceGroups, Iterable<HostGroupV4Request> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupV4Request json : hostGroupsJsons) {
            HostGroup hostGroup = new HostGroup();
            hostGroup.setName(json.getName());
            String instanceGroupName = json.getInstanceGroupName();
            if (instanceGroupName != null) {
                Optional<InstanceGroup> instanceGroup =
                        instanceGroups.stream().filter(instanceGroup1 -> instanceGroup1.getGroupName().equals(instanceGroupName)).findFirst();
                if (instanceGroup.isEmpty()) {
                    throw new BadRequestException(String.format("Cannot find instance group named '%s' in instance group list", instanceGroupName));
                }
            }
            hostGroups.add(hostGroup);
        }
        return hostGroups;
    }

    private void selectBlueprint(Set<Blueprint> blueprints, StackValidation stackValidation, Predicate<Blueprint> predicate) {
        blueprints.stream()
                .filter(predicate)
                .findFirst().ifPresent(stackValidation::setBlueprint);
    }

    private void validateInstanceTemplates(Set<InstanceGroup> instanceGroups) {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        instanceGroups.forEach(instanceGroup -> resultBuilder.merge(instanceTemplateValidator.validate(instanceGroup.getTemplate())));
        ValidationResult validationResult = resultBuilder.build();
        if (validationResult.hasError()) {
            String message = String.format("There is invalid instance template in the request, error: %s", validationResult.getFormattedErrors());
            throw new BadRequestException(message);
        }
    }
}
