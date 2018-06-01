package com.sequenceiq.cloudbreak.controller.validation.stack;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupBase;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupBase;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;

@Component
public class StackRequestValidator implements Validator<StackRequest> {

    private final Validator<TemplateRequest> templateRequestValidator;

    public StackRequestValidator(Validator<TemplateRequest> templateRequestValidator) {
        this.templateRequestValidator = templateRequestValidator;
    }

    @Override
    public ValidationResult validate(StackRequest subject) {
        ValidationResultBuilder validationBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(subject.getInstanceGroups())) {
            validationBuilder.error("Stack request must contain instance groups.");
        }
        validateHostgroupInstanceGroupMapping(subject, validationBuilder);
        validateTemplates(subject, validationBuilder);
        validateHdfStackHaveOnlyKerberosOrLdap(subject.getClusterRequest(), validationBuilder);
        return validationBuilder.build();
    }

    private void validateHdfStackHaveOnlyKerberosOrLdap(ClusterRequest clusterRequest, ValidationResultBuilder resultBuilder) {
        if (clusterRequest != null && clusterRequest.getAmbariStackDetails() != null
                && "HDF".equalsIgnoreCase(clusterRequest.getAmbariStackDetails().getStack())
                && containsLdapConfig(clusterRequest)
                && clusterRequest.getKerberos() != null) {
            resultBuilder.error("HDF stack request must not contain both LDAP and Kerberos configuration");
        }
    }

    private boolean containsLdapConfig(ClusterRequest clusterRequest) {
        return clusterRequest.getLdapConfig() != null || clusterRequest.getLdapConfigId() != null || StringUtils.isNotBlank(clusterRequest.getLdapConfigName());
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
                        + newHostGroupSet.stream().collect(Collectors.joining(", ")));
            }

            if (!hostGroupSet.containsAll(instanceGroupSet)) {
                instanceGroupSet.removeAll(hostGroupSet);
                validationBuilder.error("There are instance groups in the request that do not have a corresponding host group: "
                        + instanceGroupSet.stream().collect(Collectors.joining(", ")));
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
}

