package com.sequenceiq.distrox.v1.distrox.audit.rest;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.rest.LegacyRestCommonService;

@ExtendWith(MockitoExtension.class)
public class DatahubRestResourceAuditEventConverterTest {

    @InjectMocks
    private DatahubRestResourceAuditEventConverter underTest;

    @Mock
    private LegacyRestCommonService legacyRestCommonService;

    @Mock
    private StackService stackService;

    @Mock
    private HostGroupService hostGroupService;

    @Test
    public void testRequestParametersWhenStackNotFound() {
        StructuredRestCallEvent event = new StructuredRestCallEvent();
        OperationDetails operation = new OperationDetails();
        operation.setResourceName("name");
        operation.setWorkspaceId(123L);
        event.setOperation(operation);
        RestCallDetails restCall = new RestCallDetails();
        event.setRestCall(restCall);
        RestRequestDetails restRequest = new RestRequestDetails();
        restCall.setRestRequest(restRequest);

        Map<String, Object> params = new HashMap<>();
        when(legacyRestCommonService.addClusterCrnAndNameIfPresent(event)).thenReturn(params);
        when(stackService.findStackByNameAndWorkspaceId(operation.getResourceName(), operation.getWorkspaceId())).thenReturn(Optional.empty());
        Map<String, Object> actual = underTest.requestParameters(event);
        assertEquals(0, actual.size());
    }

    @Test
    public void testRequestParametersWhenStackFoundButNotScaling() {
        StructuredRestCallEvent event = new StructuredRestCallEvent();
        OperationDetails operation = new OperationDetails();
        operation.setResourceName("name");
        operation.setWorkspaceId(123L);
        event.setOperation(operation);
        RestCallDetails restCall = new RestCallDetails();
        event.setRestCall(restCall);
        RestRequestDetails restRequest = new RestRequestDetails();
        restRequest.setMethod("POST");
        restCall.setRestRequest(restRequest);
        Stack stack = new Stack();

        Map<String, Object> params = new HashMap<>();
        when(legacyRestCommonService.addClusterCrnAndNameIfPresent(event)).thenReturn(params);
        when(stackService.findStackByNameAndWorkspaceId(operation.getResourceName(), operation.getWorkspaceId())).thenReturn(Optional.of(stack));
        Map<String, Object> actual = underTest.requestParameters(event);
        assertEquals(0, actual.size());
    }

    @Test
    public void testRequestParametersWhenStackFoundAndScaling() {
        StructuredRestCallEvent event = new StructuredRestCallEvent();
        OperationDetails operation = new OperationDetails();
        operation.setResourceName("name");
        operation.setWorkspaceId(123L);
        operation.setResourceEvent("scaling");
        event.setOperation(operation);
        RestCallDetails restCall = new RestCallDetails();
        event.setRestCall(restCall);
        RestRequestDetails restRequest = new RestRequestDetails();
        restRequest.setMethod("PUT");
        restRequest.setBody(new Json(Map.of("group", "gr", "desiredCount", 2)).getValue());
        restCall.setRestRequest(restRequest);
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(345L);
        stack.setCluster(cluster);
        HostGroup hostGroup = new HostGroup();
        InstanceGroup instanceGroup = new InstanceGroup();
        hostGroup.setInstanceGroup(instanceGroup);

        Map<String, Object> params = new HashMap<>();
        when(legacyRestCommonService.addClusterCrnAndNameIfPresent(event)).thenReturn(params);
        when(stackService.findStackByNameAndWorkspaceId(operation.getResourceName(), operation.getWorkspaceId())).thenReturn(Optional.of(stack));
        when(hostGroupService.getByClusterIdAndNameWithRecipes(345L, "gr")).thenReturn(hostGroup);
        Map<String, Object> actual = underTest.requestParameters(event);
        assertEquals(3, actual.size());
        assertEquals(2, actual.get("desiredCount"));
        assertEquals(0, actual.get("originalCount"));
        assertEquals("gr", actual.get("hostGroup"));
    }
}
