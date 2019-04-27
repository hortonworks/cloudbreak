package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateV4RequestValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackV4RequestValidator implements Validator<StackV4Request> {

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private InstanceTemplateV4RequestValidator templateRequestValidator;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private EnvironmentService environmentService;

    @Override
    public ValidationResult validate(StackV4Request subject) {
        ValidationResultBuilder validationBuilder = ValidationResult.builder();
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        if (CollectionUtils.isEmpty(subject.getInstanceGroups())) {
            validationBuilder.error("Stack request must contain instance groups.");
        }
        validateTemplates(subject, validationBuilder);
        validateSharedService(subject, validationBuilder, workspaceId);
        validateEncryptionKey(subject, validationBuilder, workspaceId);
        validateKerberos(subject.getCluster().getKerberosName(), validationBuilder, workspaceId);
        validatePlacementByCredentialOrEnvironmentName(subject.getPlacement(), validationBuilder,
                Optional.ofNullable(subject.getEnvironment().getCredentialName()), Optional.ofNullable(subject.getEnvironment().getName()), workspaceId);
        return validationBuilder.build();
    }

    private void validateTemplates(StackV4Request stackRequest, ValidationResultBuilder resultBuilder) {
        stackRequest.getInstanceGroups()
                .stream()
                .map(i -> templateRequestValidator.validate(i.getTemplate()))
                .reduce(ValidationResult::merge)
                .ifPresent(resultBuilder::merge);
    }

    private void validateSharedService(StackV4Request stackRequest, ValidationResultBuilder validationBuilder, Long workspaceId) {
        ClusterV4Request clusterReq = stackRequest.getCluster();
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(clusterReq.getBlueprintName(), workspaceId);
        checkResourceRequirementsIfBlueprintIsDatalakeReady(stackRequest, blueprint, validationBuilder, workspaceId);
        SharedServiceV4Request sharedService = stackRequest.getSharedService();
        if (isDatalakeNameSpecified(sharedService) && blueprintService.isAmbariBlueprint(blueprint)) {
            DatalakeResources datalakeResources = datalakeResourcesService.getByNameForWorkspaceId(sharedService.getDatalakeName(), workspaceId);
            Optional<Stack> stack = datalakeResources.getDatalakeStackId() == null ? Optional.empty()
                    : stackService.findById(datalakeResources.getDatalakeStackId());
            if (stack.isPresent() && AVAILABLE.equals(stack.get().getStatus())) {
                validateAvailableDatalakeStack(validationBuilder, stack);
            } else if (stack.isPresent() && !AVAILABLE.equals(stack.get().getStatus())) {
                validationBuilder.error("Unable to attach to datalake because it's infrastructure is not ready.");
            } else if (!stack.isPresent() && datalakeResources.getDatalakeStackId() != null) {
                validationBuilder.error("Unable to attach to datalake because it doesn't exists.");
            }
        }
    }

    private boolean isDatalakeNameSpecified(SharedServiceV4Request sharedService) {
        return sharedService != null && isNotBlank(sharedService.getDatalakeName());
    }

    private void validateAvailableDatalakeStack(ValidationResultBuilder validationBuilder, Optional<Stack> stack) {
        Optional<Cluster> cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stack.get().getId());
        if (cluster.isPresent() && !AVAILABLE.equals(cluster.get().getStatus())) {
            validationBuilder.error("Ambari installation in progress or some of it's components has failed. "
                    + "Please check Ambari before trying to attach cluster to datalake.");
        }
    }

    private void validateEncryptionKey(StackV4Request stackRequest, ValidationResultBuilder validationBuilder, Long workspaceId) {
        stackRequest.getInstanceGroups().stream()
                .filter(request -> isEncryptionTypeSetUp(request.getTemplate()))
                .filter(request -> {
                    EncryptionType valueForTypeKey = getEncryptionType(request.getTemplate());
                    return EncryptionType.CUSTOM.equals(valueForTypeKey);
                })
                .forEach(request -> checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(request, stackRequest.getEnvironment().getCredentialName(),
                        stackRequest.getPlacement().getRegion(), validationBuilder, workspaceId));
    }

    private EncryptionType getEncryptionType(InstanceTemplateV4Request template) {
        if (template.getAws() != null && template.getAws().getEncryption() != null && template.getAws().getEncryption().getType() != null) {
            return template.getAws().getEncryption().getType();
        }
        if (template.getGcp() != null && template.getGcp().getEncryption() != null && template.getGcp().getEncryption().getType() != null) {
            return template.getGcp().getEncryption().getType();
        }
        return null;
    }

    private void checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(InstanceGroupV4Request instanceGroupRequest, String credentialName,
            String region, ValidationResultBuilder validationBuilder, Long workspaceId) {
        Optional<CloudEncryptionKeys> keys = getEncryptionKeysWithExceptionHandling(credentialName, region, workspaceId);
        if (keys.isPresent() && !keys.get().getCloudEncryptionKeys().isEmpty()) {
            if (getEncryptionKey(instanceGroupRequest.getTemplate()) == null) {
                validationBuilder.error("There is no encryption key provided but CUSTOM type is given for encryption.");
            } else if (keys.get().getCloudEncryptionKeys().stream().map(CloudEncryptionKey::getName)
                    .noneMatch(s -> s.equals(getEncryptionKey(instanceGroupRequest.getTemplate())))) {
                validationBuilder.error("The provided encryption key does not exists in the given region's encryption key list for this credential.");
            }
        }
    }

    private String getEncryptionKey(InstanceTemplateV4Request template) {
        if (template.getAws() != null && template.getAws().getEncryption() != null && template.getAws().getEncryption().getKey() != null) {
            return template.getAws().getEncryption().getKey();
        }
        if (template.getGcp() != null && template.getGcp().getEncryption() != null && template.getGcp().getEncryption().getKey() != null) {
            return template.getGcp().getEncryption().getKey();
        }
        return null;
    }

    private Optional<CloudEncryptionKeys> getEncryptionKeysWithExceptionHandling(String credentialName, String region, Long workspaceId) {
        try {
            PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, null, null);
            return Optional.ofNullable(platformParameterService.getEncryptionKeys(request));
        } catch (RuntimeException ignore) {
            return Optional.empty();
        }
    }

    private void checkResourceRequirementsIfBlueprintIsDatalakeReady(StackV4Request stackRequest, Blueprint blueprint,
            ValidationResultBuilder validationBuilder, Long wsId) {
        if (blueprintService.isDatalakeBlueprint(blueprint)) {
            ClusterV4Request clusterRequest = stackRequest.getCluster();
            Set<String> databaseTypes = getGivenRdsTypes(clusterRequest, wsId);
            if (blueprintService.isAmbariBlueprint(blueprint)) {
                String rdsErrorMessageFormat = "For a Datalake cluster (since you have selected a datalake ready blueprint) you should provide at least "
                        + "one %s rds/database configuration to the Cluster request";
                if (!databaseTypes.contains(DatabaseType.HIVE.name())) {
                    validationBuilder.error(String.format(rdsErrorMessageFormat, "Hive"));
                }
                if (!databaseTypes.contains(DatabaseType.RANGER.name())) {
                    validationBuilder.error(String.format(rdsErrorMessageFormat, "Ranger"));
                }
                if (isLdapNotProvided(clusterRequest)) {
                    validationBuilder.error("For a Datalake cluster (since you have selected a datalake ready blueprint) you should provide an "
                            + "LDAP configuration or its name/id to the Cluster request");
                }
            }
        }
    }

    private boolean isLdapNotProvided(ClusterV4Request clusterRequest) {
        return clusterRequest.getLdapName() == null;
    }

    private Set<String> getGivenRdsTypes(ClusterV4Request clusterRequest, Long workspaceId) {
        return Optional.ofNullable(clusterRequest.getDatabases()).orElse(new HashSet<>()).stream().map(s ->
                rdsConfigService.getByNameForWorkspaceId(s, workspaceId).getType()).collect(Collectors.toSet());
    }

    private void validateKerberos(String kerberosName, ValidationResultBuilder validationBuilder, Long workspaceId) {
        if (kerberosName != null && kerberosName.isEmpty()) {
            validationBuilder.error("kerberosName should not be empty. Should be either filled or null!");
        } else if (isNotEmpty(kerberosName)) {
            kerberosConfigService.getByNameForWorkspaceId(kerberosName, workspaceId);
        }
    }

    private boolean isEncryptionTypeSetUp(InstanceTemplateV4Request template) {
        return getEncryptionType(template) != null;
    }

    private void validatePlacementByCredentialOrEnvironmentName(PlacementSettingsV4Request placementSettingsV4Request,
            ValidationResultBuilder validationBuilder, Optional<String> credentialName, Optional<String> environmentName, Long workspaceId) {
        Credential credential;
        if (credentialName.isPresent()) {
            credential = credentialService.getByNameForWorkspaceId(credentialName.get(), workspaceId);
        } else if (environmentName.isPresent()) {
            String credentialNameFromEnvironment = environmentService.get(environmentName.get(), workspaceId).getCredentialName();
            credential = credentialService.getByNameForWorkspaceId(credentialNameFromEnvironment, workspaceId);
        } else {
            validationBuilder.error("Environment must contains at least environment name or the name of the credential!");
            return;
        }
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCloudPlatform(credential.cloudPlatform());
        platformResourceRequest.setCredential(credential);
        platformResourceRequest.setPlatformVariant(credential.cloudPlatform());
        platformResourceRequest.setFilters(Collections.emptyMap());
        Optional<CloudRegions> cloudRegions = Optional.ofNullable(platformParameterService.getRegionsByCredential(platformResourceRequest));
        if (cloudRegions.isPresent() && cloudRegions.get().areRegionsSupported() && placementSettingsV4Request == null) {
            validationBuilder.error("The given cloud platform must contain a valid placement request!");
        }
    }

}

