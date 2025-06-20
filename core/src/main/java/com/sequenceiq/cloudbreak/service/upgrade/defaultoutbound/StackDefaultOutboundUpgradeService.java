package com.sequenceiq.cloudbreak.service.upgrade.defaultoutbound;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackDefaultOutboundUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackOutboundTypeValidationV4Response;
import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class StackDefaultOutboundUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDefaultOutboundUpgradeService.class);

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackService stackService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    public StackDefaultOutboundUpgradeV4Response upgradeDefaultOutbound(NameOrCrn nameOrCrn) {
        Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Default Outbound upgrade has been initiated for stack {}", nameOrCrn.getNameOrCrn());

        // TODO: Call the upgrade logic here
        return null;
    }

    public StackOutboundTypeValidationV4Response getStacksWithOutboundType(Long workspaceId, String envCrn) {
        MDCBuilder.addResourceCrn(envCrn);
        MDCBuilder.addEnvironmentCrn(envCrn);

        Set<StackListItem> stackList = stackService.getByWorkspaceId(workspaceId, envCrn, List.of(StackType.DATALAKE, StackType.WORKLOAD));
        List<Long> stackIds = stackList.stream()
                .map(StackListItem::getId)
                .toList();
        List<Resource> networks = resourceService.findByStackIdsAndType(stackIds, ResourceType.AZURE_NETWORK);

        Map<String, OutboundType> stackOutboundTypeMap = stackList.stream().collect(Collectors.toMap(
                StackListItem::getName,
            stackItem -> getResource(stackItem, networks)
                .flatMap(resource -> resourceAttributeUtil.getTypedAttributes(resource, NetworkAttributes.class))
                .map(NetworkAttributes::getOutboundType)
                .orElse(OutboundType.NOT_DEFINED)
        ));

        LOGGER.debug("Current OutboundType situation for env {}: {}", envCrn, stackOutboundTypeMap);
        return new StackOutboundTypeValidationV4Response(stackOutboundTypeMap);
    }

    private Optional<Resource> getResource(StackListItem stackItem, List<Resource> networks) {
        return networks.stream()
            .filter(resource -> resource.getStack().getId().equals(stackItem.getId()))
            .findFirst();
    }
}

