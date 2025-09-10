package com.sequenceiq.distrox.v1.distrox.audit.rest;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.auditeventname.rest.RestResourceAuditEventConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.rest.LegacyRestCommonService;

@Component
public class DatahubRestResourceAuditEventConverter implements RestResourceAuditEventConverter {

    @Inject
    private LegacyRestCommonService legacyRestCommonService;

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Override
    public AuditEventName auditEventName(StructuredRestCallEvent structuredEvent) {
        String method = structuredEvent.getRestCall().getRestRequest().getMethod();
        AuditEventName eventName = null;
        String resourceEvent = structuredEvent.getOperation().getResourceEvent();
        if ("POST".equals(method) || "PUT".equals(method)) {
            if (resourceEvent == null) {
                eventName = AuditEventName.CREATE_DATAHUB_CLUSTER;
            } else {
                eventName = updateRest(resourceEvent);
            }
        } else if ("DELETE".equals(method)) {
            eventName = deletionRest(resourceEvent, structuredEvent.getOperation());
        }
        return eventName;
    }

    private AuditEventName deletionRest(String resourceEvent, OperationDetails operationDetails) {
        if (StringUtils.isEmpty(resourceEvent)
                || resourceEvent.equals(operationDetails.getResourceName())
                || resourceEvent.equals(operationDetails.getResourceCrn())) {
            return AuditEventName.DELETE_DATAHUB_CLUSTER;
        } else if ("instance".equals(resourceEvent) || "instances".equals(resourceEvent)) {
            return AuditEventName.INSTANCE_DELETE_DATAHUB_CLUSTER;
        }
        return null;
    }

    private AuditEventName updateRest(String resourceEvent) {
        if ("retry".equals(resourceEvent)) {
            return AuditEventName.RETRY_DATAHUB_CLUSTER;
        } else if ("stop".equals(resourceEvent)) {
            return AuditEventName.STOP_DATAHUB_CLUSTER;
        } else if ("start".equals(resourceEvent)) {
            return AuditEventName.START_DATAHUB_CLUSTER;
        } else if ("scaling".equals(resourceEvent)) {
            return AuditEventName.RESIZE_DATAHUB_CLUSTER;
        } else if ("maintenance".equals(resourceEvent)) {
            return AuditEventName.MAINTAIN_DATAHUB_CLUSTER;
        } else if ("manual_repair".equals(resourceEvent)) {
            return AuditEventName.MANUAL_REPAIR_DATAHUB_CLUSTER;
        } else if ("rotate_autotls_certificates".equals(resourceEvent)) {
            return AuditEventName.ROTATE_DATAHUB_CLUSTER_CERTIFICATES;
        } else if ("cluster_upgrade".equals(resourceEvent)) {
            return AuditEventName.UPGRADE_DATAHUB_CLUSTER;
        }
        return null;
    }

    @Override
    public boolean shouldAudit(StructuredRestCallEvent structuredRestCallEvent) {
        return true;
    }

    @Override
    public Crn.Service eventSource(StructuredRestCallEvent structuredEvent) {
        return Crn.Service.DATAHUB;
    }

    @Override
    public Map<String, Object> requestParameters(StructuredRestCallEvent structuredEvent) {
        Map<String, Object> params = legacyRestCommonService.addClusterCrnAndNameIfPresent(structuredEvent);
        OperationDetails operation = structuredEvent.getOperation();
        Optional<Stack> stack = stackService.findStackByNameAndWorkspaceId(operation.getResourceName(), operation.getWorkspaceId());
        AuditEventName auditEventName = auditEventName(structuredEvent);
        if (stack.isPresent() && auditEventName == AuditEventName.RESIZE_DATAHUB_CLUSTER) {
            Json json = new Json(structuredEvent.getRestCall().getRestRequest().getBody());
            String group = json.getString("group");
            HostGroup hostGroup = hostGroupService.getByClusterIdAndNameWithRecipes(stack.get().getCluster().getId(), group);
            Integer desiredCount = json.getInt("desiredCount");
            Integer originalNodeCount = hostGroup.getInstanceGroup().getNodeCount();
            params.put("desiredCount", desiredCount);
            params.put("originalCount", originalNodeCount);
            params.put("hostGroup", group);
        }
        return params;
    }
}
