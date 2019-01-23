package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.ConnectorV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.EncryptionKeyConfigV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformEncryptionKeysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateV4RequestValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackRequestValidator implements Validator<StackV4Request> {

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
    private KerberosService kerberosService;

    @Inject
    private ConnectorV4Endpoint connectorV4Endpoint;

    @Override
    public ValidationResult validate(StackV4Request subject) {
        ValidationResultBuilder validationBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(subject.getInstanceGroups())) {
            validationBuilder.error("Stack request must contain instance groups.");
        }
        validateTemplates(subject, validationBuilder);
        validateSharedService(subject, validationBuilder);
        validateEncryptionKey(subject, validationBuilder);
        validateKerberos(subject.getCluster().getKerberosName(), validationBuilder);
        return validationBuilder.build();
    }

    private void validateTemplates(StackV4Request stackRequest, ValidationResultBuilder resultBuilder) {
        stackRequest.getInstanceGroups()
                .stream()
                .map(i -> templateRequestValidator.validate(i.getTemplate()))
                .reduce(ValidationResult::merge)
                .ifPresent(resultBuilder::merge);
    }

    private void validateSharedService(StackV4Request stackRequest, ValidationResultBuilder validationBuilder) {
        checkResourceRequirementsIfBlueprintIsDatalakeReady(stackRequest, validationBuilder);
        SharedServiceV4Request sharedService = stackRequest.getCluster().getSharedService();
        if (sharedService != null) {
            Stack stack = stackService.getByNameInWorkspace(sharedService.getSharedClusterName(), restRequestThreadLocalService.getRequestedWorkspaceId());
            if (stack == null) {
                validationBuilder.error("Unable to attach to datalake because it doesn't exists.");
            } else if (AVAILABLE.equals(stack.getStatus())) {
                Optional<Cluster> cluster = Optional.ofNullable(clusterService.retrieveClusterByStackIdWithoutAuth(stack.getId()));
                if (cluster.isPresent() && !AVAILABLE.equals(cluster.get().getStatus())) {
                    validationBuilder.error("Ambari installation in progress or some of it's components has failed. "
                            + "Please check Ambari before trying to attach cluster to datalake.");
                }
            } else if (!AVAILABLE.equals(stack.getStatus())) {
                validationBuilder.error("Unable to attach to datalake because it's infrastructure is not ready.");
            }
        }
    }

    private void validateEncryptionKey(StackV4Request stackRequest, ValidationResultBuilder validationBuilder) {
        stackRequest.getInstanceGroups().stream()
                .filter(request -> isEncryptionTypeSetUp(request.getTemplate()))
                .filter(request -> {
                    EncryptionType valueForTypeKey = getEncryptionType(request.getTemplate());
                    return EncryptionType.CUSTOM.equals(valueForTypeKey);
                })
                .forEach(request -> checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(request, stackRequest.getEnvironment().getCredentialName(),
                        stackRequest.getEnvironment().getPlacement().getRegion(), validationBuilder));
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
            String region, ValidationResultBuilder validationBuilder) {
        Optional<PlatformEncryptionKeysV4Response> keys = getEncryptionKeysWithExceptionHandling(credentialName, region);
        if (keys.isPresent() && !keys.get().getEncryptionKeyConfigs().isEmpty()) {
            if (getEncryptionKey(instanceGroupRequest.getTemplate()) == null) {
                validationBuilder.error("There is no encryption key provided but CUSTOM type is given for encryption.");
            } else if (keys.get().getEncryptionKeyConfigs().stream().map(EncryptionKeyConfigV4Response::getName)
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

    private Optional<PlatformEncryptionKeysV4Response> getEncryptionKeysWithExceptionHandling(String credentialName, String region) {
        try {
            return Optional.ofNullable(connectorV4Endpoint.getEncryptionKeys(restRequestThreadLocalService.getRequestedWorkspaceId(),
                    credentialName, region, null, null));
        } catch (RuntimeException ignore) {
            return Optional.empty();
        }
    }

    private void checkResourceRequirementsIfBlueprintIsDatalakeReady(StackV4Request stackRequest, ValidationResultBuilder validationBuilder) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(stackRequest.getCluster()
                .getAmbari().getBlueprintName(), restRequestThreadLocalService.getRequestedWorkspaceId());
        boolean sharedServiceReadyBlueprint = blueprintService.isDatalakeBlueprint(blueprint);
        if (sharedServiceReadyBlueprint) {
            Set<String> databaseTypes = getGivenRdsTypes(stackRequest.getCluster());
            String rdsErrorMessageFormat = "For a Datalake cluster (since you have selected a datalake ready blueprint) you should provide at least one %s "
                    + "rds/database configuration to the Cluster request";
            if (!databaseTypes.contains(DatabaseType.HIVE.name())) {
                validationBuilder.error(String.format(rdsErrorMessageFormat, "Hive"));
            }
            if (!databaseTypes.contains(DatabaseType.RANGER.name())) {
                validationBuilder.error(String.format(rdsErrorMessageFormat, "Ranger"));
            }
            if (isLdapNotProvided(stackRequest.getCluster())) {
                validationBuilder.error("For a Datalake cluster (since you have selected a datalake ready blueprint) you should provide an "
                        + "LDAP configuration or its name/id to the Cluster request");
            }
        }
    }

    private boolean isLdapNotProvided(ClusterV4Request clusterRequest) {
        return clusterRequest.getLdapName() == null;
    }

    private Set<String> getGivenRdsTypes(ClusterV4Request clusterRequest) {
        return clusterRequest.getDatabases().stream().map(s ->
                rdsConfigService.getByNameForWorkspaceId(s, restRequestThreadLocalService.getRequestedWorkspaceId()).getType()).collect(Collectors.toSet());
    }

    private void validateKerberos(String kerberosConfigName, ValidationResultBuilder validationBuilder) {
        if (StringUtils.isEmpty(kerberosConfigName)) {
            validationBuilder.error("kerberosConfigNameParameter should not be empty. Should be neither filled or null!");
        } else if (StringUtils.isNotEmpty(kerberosConfigName)) {
            kerberosService.getByNameForWorkspaceId(kerberosConfigName, restRequestThreadLocalService.getRequestedWorkspaceId());
        }
    }

    private boolean isEncryptionTypeSetUp(InstanceTemplateV4Request template) {
        return getEncryptionType(template) != null;
    }
}

