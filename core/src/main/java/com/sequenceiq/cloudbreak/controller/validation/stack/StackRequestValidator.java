package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.EncryptionKeyConfigJson;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformEncryptionKeysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.requests.PlatformResourceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupBase;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupBase;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateRequestValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class StackRequestValidator implements Validator<StackRequest> {

    private static final String TYPE = "type";

    private static final String KEY = "key";

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private TemplateRequestValidator templateRequestValidator;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private KerberosService kerberosService;

    @Override
    public ValidationResult validate(StackRequest subject) {
        ValidationResultBuilder validationBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(subject.getInstanceGroups())) {
            validationBuilder.error("Stack request must contain instance groups.");
        }
        validateHostgroupInstanceGroupMapping(subject, validationBuilder);
        validateTemplates(subject, validationBuilder);
        validateSharedService(subject, validationBuilder);
        validateEncryptionKey(subject, validationBuilder);
        validateKerberos(subject.getClusterRequest().getKerberosConfigName(), validationBuilder);
        return validationBuilder.build();
    }

    private void validateHostgroupInstanceGroupMapping(StackRequest stackRequest, ValidationResultBuilder validationBuilder) {
        Set<String> instanceGroupSet = stackRequest.getInstanceGroups()
                .stream()
                .map(InstanceGroupBase::getGroup)
                .collect(Collectors.toSet());

        if (stackRequest.getClusterRequest() != null) {
            Set<String> hostGroupSet = stackRequest.getClusterRequest().getHostGroups()
                    .stream()
                    .map(HostGroupBase::getName)
                    .collect(Collectors.toSet());

            if (!instanceGroupSet.containsAll(hostGroupSet)) {
                Set<String> newHostGroupSet = Sets.newHashSet(hostGroupSet);
                newHostGroupSet.removeAll(instanceGroupSet);
                validationBuilder.error("There are host groups in the request that do not have a corresponding instance group: "
                        + String.join(", ", newHostGroupSet));
            }

            if (!hostGroupSet.containsAll(instanceGroupSet)) {
                instanceGroupSet.removeAll(hostGroupSet);
                validationBuilder.error("There are instance groups in the request that do not have a corresponding host group: "
                        + String.join(", ", instanceGroupSet));
            }
        }
    }

    private void validateTemplates(StackRequest stackRequest, ValidationResultBuilder resultBuilder) {
        stackRequest.getInstanceGroups()
                .stream()
                .map(i -> templateRequestValidator.validate(i.getTemplate()))
                .reduce(ValidationResult::merge)
                .ifPresent(resultBuilder::merge);
    }

    private void validateSharedService(StackRequest stackRequest, ValidationResultBuilder validationBuilder) {
        checkResourceRequirementsIfBlueprintIsDatalakeReady(stackRequest, validationBuilder);
        if (stackRequest.getClusterToAttach() != null) {
            Optional<Stack> stack = stackRepository.findById(stackRequest.getClusterToAttach());
            if (stack.isPresent() && AVAILABLE.equals(stack.get().getStatus())) {
                Optional<Cluster> cluster = Optional.ofNullable(clusterService.retrieveClusterByStackIdWithoutAuth(stackRequest.getClusterToAttach()));
                if (cluster.isPresent() && !AVAILABLE.equals(cluster.get().getStatus())) {
                    validationBuilder.error("Ambari installation in progress or some of it's components has failed. "
                            + "Please check Ambari before trying to attach cluster to datalake.");
                }
            } else if (stack.isPresent() && !AVAILABLE.equals(stack.get().getStatus())) {
                validationBuilder.error("Unable to attach to datalake because it's infrastructure is not ready.");
            } else if (!stack.isPresent()) {
                validationBuilder.error("Unable to attach to datalake because it doesn't exists.");
            }
        }
    }

    private void validateEncryptionKey(StackRequest stackRequest, ValidationResultBuilder validationBuilder) {
        stackRequest.getInstanceGroups().stream()
                .filter(request -> request.getTemplate().getParameters().containsKey(TYPE))
                .filter(request -> {
                    EncryptionType valueForTypeKey = getEncryptionIfTypeMatches(request.getTemplate().getParameters().get(TYPE));
                    return valueForTypeKey != null && EncryptionType.CUSTOM.equals(getEncryptionIfTypeMatches(valueForTypeKey));
                })
                .forEach(request -> checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(request, stackRequest.getCredentialName(),
                        stackRequest.getRegion(), validationBuilder));
    }

    private EncryptionType getEncryptionIfTypeMatches(Object o) {
        return o instanceof EncryptionType ? (EncryptionType) o : null;
    }

    private void checkEncryptionKeyValidityForInstanceGroupWhenKeysAreListable(InstanceGroupRequest instanceGroupRequest, String credentialName,
            String region, ValidationResultBuilder validationBuilder) {
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        Credential cred = credentialService.getByNameForWorkspaceId(credentialName, workspaceId);
        Optional<PlatformEncryptionKeysV4Response> keys = Optional.empty();
        if (keys.isPresent() && !keys.get().getEncryptionKeyConfigs().isEmpty()) {
            if (!instanceGroupRequest.getTemplate().getParameters().containsKey(KEY)) {
                validationBuilder.error("There is no encryption key provided but CUSTOM type is given for encryption.");
            } else if (keys.get().getEncryptionKeyConfigs().stream().map(EncryptionKeyConfigJson::getName)
                    .noneMatch(s -> Objects.equals(s, instanceGroupRequest.getTemplate().getParameters().get(KEY)))) {
                validationBuilder.error("The provided encryption key does not exists in the given region's encryption key list for this credential.");
            }
        }
    }

    private PlatformResourceV4Request getRequestForEncryptionKeys(Long credentialId, String region) {
        PlatformResourceV4Request request = new PlatformResourceV4Request();
        request.setCredentialId(credentialId);
        request.setRegion(region);
        return request;
    }

    private void checkResourceRequirementsIfBlueprintIsDatalakeReady(StackRequest stackRequest, ValidationResultBuilder validationBuilder) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(stackRequest.getClusterRequest()
                .getBlueprintName(), restRequestThreadLocalService.getRequestedWorkspaceId());
        boolean sharedServiceReadyBlueprint = blueprintService.isDatalakeBlueprint(blueprint);
        if (sharedServiceReadyBlueprint) {
            Set<String> databaseTypes = getGivenRdsTypes(stackRequest.getClusterRequest());
            String rdsErrorMessageFormat = "For a Datalake cluster (since you have selected a datalake ready blueprint) you should provide at least one %s "
                    + "rds/database configuration to the Cluster request";
            if (!databaseTypes.contains(DatabaseType.HIVE.name())) {
                validationBuilder.error(String.format(rdsErrorMessageFormat, "Hive"));
            }
            if (!databaseTypes.contains(DatabaseType.RANGER.name())) {
                validationBuilder.error(String.format(rdsErrorMessageFormat, "Ranger"));
            }
            if (isLdapNotProvided(stackRequest.getClusterRequest())) {
                validationBuilder.error("For a Datalake cluster (since you have selected a datalake ready blueprint) you should provide an "
                        + "LDAP configuration or its name/id to the Cluster request");
            }
        }
    }

    private boolean isLdapNotProvided(ClusterRequest clusterRequest) {
        return clusterRequest.getLdapConfig() == null && clusterRequest.getLdapConfigName() == null && clusterRequest.getLdapConfigId() == null;
    }

    private Set<String> getGivenRdsTypes(ClusterRequest clusterRequest) {
        Set<String> types = clusterRequest.getRdsConfigIds().stream().map(id -> rdsConfigService.get(id).getType())
                .collect(Collectors.toSet());
        types.addAll(clusterRequest.getRdsConfigJsons().stream().map(rdsConfigRequest -> rdsConfigRequest.getType())
                .collect(Collectors.toSet()));
        types.addAll(clusterRequest.getRdsConfigNames().stream().map(s ->
                rdsConfigService.getByNameForWorkspaceId(s, restRequestThreadLocalService.getRequestedWorkspaceId()).getType()).collect(Collectors.toSet()));
        return types;
    }

    private void validateKerberos(String kerberosConfigName, ValidationResultBuilder validationBuilder) {
        if (kerberosConfigName != null && kerberosConfigName.length() == 0) {
            validationBuilder.error("kerberosConfigNameParameter should not be empty. Should be neither filled or null!");
        } else if (StringUtils.isNotEmpty(kerberosConfigName)) {
            kerberosService.getByNameForWorkspaceId(kerberosConfigName, restRequestThreadLocalService.getRequestedWorkspaceId());
        }
    }

}

