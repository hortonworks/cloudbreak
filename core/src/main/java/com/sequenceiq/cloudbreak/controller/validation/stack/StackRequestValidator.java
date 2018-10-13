package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.EncryptionKeyConfigJson;
import com.sequenceiq.cloudbreak.api.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupBase;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupBase;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.controller.PlatformParameterV1Controller;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateRequestValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class StackRequestValidator implements Validator<StackRequest> {

    private static final String TYPE = "type";

    private static final String KEY = "key";

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private PlatformParameterV1Controller parameterV1Controller;

    @Inject
    private CredentialService credentialService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Inject
    private TemplateRequestValidator templateRequestValidator;

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
        Optional<PlatformEncryptionKeysResponse> keys = getEncryptionKeysWithExceptionHandling(cred.getId(), region);
        if (keys.isPresent() && !keys.get().getEncryptionKeyConfigs().isEmpty()) {
            if (!instanceGroupRequest.getTemplate().getParameters().containsKey(KEY)) {
                validationBuilder.error("There is no encryption key provided but CUSTOM type is given for encryption.");
            } else if (keys.get().getEncryptionKeyConfigs().stream().map(EncryptionKeyConfigJson::getName)
                    .noneMatch(s -> Objects.equals(s, instanceGroupRequest.getTemplate().getParameters().get(KEY)))) {
                validationBuilder.error("The provided encryption key does not exists in the given region's encryption key list for this credential.");
            }
        }
    }

    private Optional<PlatformEncryptionKeysResponse> getEncryptionKeysWithExceptionHandling(Long id, String region) {
        try {
            return Optional.ofNullable(parameterV1Controller.getEncryptionKeys(getRequestForEncryptionKeys(id, region)));
        } catch (RuntimeException ignore) {
            return Optional.empty();
        }
    }

    private PlatformResourceRequestJson getRequestForEncryptionKeys(Long credentialId, String region) {
        PlatformResourceRequestJson request = new PlatformResourceRequestJson();
        request.setCredentialId(credentialId);
        request.setRegion(region);
        return request;
    }

}

