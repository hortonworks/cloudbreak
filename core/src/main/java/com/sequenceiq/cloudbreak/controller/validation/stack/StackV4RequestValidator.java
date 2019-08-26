package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateV4RequestValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.PlatformResourceClientService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

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
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private PlatformResourceClientService platformResourceClientService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Override
    public ValidationResult validate(StackV4Request subject) {
        ValidationResultBuilder validationBuilder = ValidationResult.builder();
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        if (CollectionUtils.isEmpty(subject.getInstanceGroups())) {
            validationBuilder.error("Stack request must contain instance groups.");
        }
        validateTemplates(subject, validationBuilder);
        validateSharedService(subject, validationBuilder, workspaceId);
        validateEncryptionKey(subject, validationBuilder);
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

    private void validateEncryptionKey(StackV4Request stackRequest, ValidationResultBuilder validationBuilder) {
        if (StringUtils.isEmpty(stackRequest.getEnvironmentCrn())) {
            validationBuilder.error("Environment CRN cannot be null or empty.");
            return;
        }
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stackRequest.getEnvironmentCrn());
        stackRequest.getInstanceGroups().stream()
                .filter(request -> isEncryptionTypeSetUp(request.getTemplate()))
                .filter(request -> {
                    EncryptionType valueForTypeKey = getEncryptionType(request.getTemplate());
                    return EncryptionType.CUSTOM.equals(valueForTypeKey);
                })
                .forEach(request -> {
                    checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(request, environment.getCredential().getName(),
                            stackRequest.getPlacement().getRegion(), validationBuilder);
                });
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

    private void checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(InstanceGroupV4Request instanceGroupRequest,
        String credentialName, String region, ValidationResultBuilder validationBuilder) {
        Optional<CloudEncryptionKeys> keys = getEncryptionKeysWithExceptionHandling(credentialName, region);
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

    private Optional<CloudEncryptionKeys> getEncryptionKeysWithExceptionHandling(String credentialName, String region) {
        try {
            CloudEncryptionKeys cloudEncryptionKeys = platformResourceClientService.getEncryptionKeys(credentialName, region);
            return Optional.ofNullable(cloudEncryptionKeys);
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
                if (isLdapNotProvided(stackRequest.getEnvironmentCrn(), stackRequest.getName())) {
                    validationBuilder.error("For a Datalake cluster (since you have selected a datalake ready blueprint) you should provide an "
                            + "LDAP configuration or its name/id to the Cluster request");
                }
            }
        }
    }

    private boolean isLdapNotProvided(String environmentCrn, String clusterName) {
        return !ldapConfigService.isLdapConfigExistsForEnvironment(environmentCrn, clusterName);
    }

    private Set<String> getGivenRdsTypes(ClusterV4Request clusterRequest, Long workspaceId) {
        return Optional.ofNullable(clusterRequest.getDatabases()).orElse(new HashSet<>()).stream().map(s ->
                rdsConfigService.getByNameForWorkspaceId(s, workspaceId).getType()).collect(Collectors.toSet());
    }

    private boolean isEncryptionTypeSetUp(InstanceTemplateV4Request template) {
        return getEncryptionType(template) != null;
    }
}
