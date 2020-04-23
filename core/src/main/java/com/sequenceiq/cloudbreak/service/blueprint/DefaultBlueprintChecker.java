package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE;
import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CrnsByCategory;
import com.sequenceiq.authorization.service.DefaultResourceChecker;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.projection.BlueprintStatusView;

@Component
public class DefaultBlueprintChecker implements DefaultResourceChecker {

    @Inject
    private BlueprintService blueprintService;

    @Override
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.CLUSTER_TEMPLATE;
    }

    @Override
    public boolean isDefault(String resourceCrn) {
        BlueprintStatusView b = blueprintService.getStatusViewByResourceCrn(resourceCrn);
        return b != null && ResourceStatus.DEFAULT == b.getStatus();
    }

    @Override
    public boolean isAllowedAction(AuthorizationResourceAction action) {
        return DESCRIBE_CLUSTER_TEMPLATE.equals(action);
    }

    @Override
    public CrnsByCategory getDefaultResourceCrns(Collection<String> resourceCrns) {
        Map<Boolean, List<String>> byDefault = resourceCrns.stream().collect(Collectors.partitioningBy(this::isDefault));
        return CrnsByCategory.newBuilder()
                .defaultResourceCrns(byDefault.getOrDefault(Boolean.TRUE, emptyList()))
                .notDefaultResourceCrns(byDefault.getOrDefault(Boolean.FALSE, emptyList()))
                .build();
    }

}
