package com.sequenceiq.cloudbreak.cloud.gcp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.OperationInfo;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.OperationType;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

class GcpOperationUtilTest {
    @Test
    void testGetOperationTypeWhenNullReturnsUnknown() {
        assertEquals(OperationType.UNKNOWN, GcpOperationUtil.getOperationType(null));
    }

    @Test
    void testGetOperationTypeWhenZoneSetReturnsZonal() {
        Operation operation = new Operation();
        operation.setZone("zones/us-east1-b");
        assertEquals(OperationType.ZONAL, GcpOperationUtil.getOperationType(operation));
    }

    @Test
    void testGetOperationTypeWhenRegionSetButNoZoneReturnsRegional() {
        Operation operation = new Operation();
        operation.setRegion("regions/us-east1");
        assertEquals(OperationType.REGIONAL, GcpOperationUtil.getOperationType(operation));
    }

    @Test
    void testGetOperationTypeWhenNeitherZoneNorRegionReturnsGlobal() {
        Operation operation = new Operation();
        assertEquals(OperationType.GLOBAL, GcpOperationUtil.getOperationType(operation));
    }

    @Test
    void testGetOperationInfoUsesLegacyOperationIdWhenOperationInfoAbsent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(GcpOperationUtil.OPERATION_ID, "legacy-op-789");
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE)
                .withName("instance-1")
                .withStatus(CommonStatus.CREATED)
                .withParameters(params)
                .build();

        OperationInfo operationInfo = GcpOperationUtil.getOperationInfo(resource);

        assertEquals(OperationType.UNKNOWN, operationInfo.operationType());
        assertEquals("legacy-op-789", operationInfo.operationId());
    }

    @Test
    void testGetOperationInfoaUsesLegacyOperationIdWhenOperationInfoAbsent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(GcpOperationUtil.OPERATION_ID, List.of("legacy-op-789", "legacy-op-2"));
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE)
                .withName("instance-1")
                .withStatus(CommonStatus.CREATED)
                .withParameters(params)
                .build();

        List<OperationInfo> operationInfos = GcpOperationUtil.getOperationInfos(resource);

        assertEquals(OperationType.UNKNOWN, operationInfos.get(0).operationType());
        assertEquals("legacy-op-789", operationInfos.get(0).operationId());
        assertEquals(OperationType.UNKNOWN, operationInfos.get(1).operationType());
        assertEquals("legacy-op-2", operationInfos.get(1).operationId());
    }
}
