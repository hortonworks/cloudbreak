package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

public class AbstractGcpDatabaseBaseResourceCheckerTest {

    TestAbstractGcpDatabaseBaseResourceChecker underTest = new TestAbstractGcpDatabaseBaseResourceChecker();

    @Test
    public void testCreateOperationAwareCloudResource() {
        CloudResource cloudResource = CloudResource.builder()
                .withType(ResourceType.GCP_DATABASE)
                .withName("test-image")
                .withStatus(CommonStatus.REQUESTED)
                .withParameters(Map.of())
                .build();
        com.google.api.services.sqladmin.model.Operation operation = new com.google.api.services.sqladmin.model.Operation();
        operation.setName("op1");
        CloudResource operationAwareCloudResource = underTest.createOperationAwareCloudResource(cloudResource, operation);
        assertEquals(false, operationAwareCloudResource.isPersistent());
        assertEquals("op1", operationAwareCloudResource.getStringParameter("opid"));
        assertEquals(ResourceType.GCP_DATABASE, operationAwareCloudResource.getType());
    }

}
