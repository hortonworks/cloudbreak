package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

public class AbstractGcpDatabaseBaseResourceCheckerTest {

    TestAbstractGcpDatabaseBaseResourceChecker underTest = new TestAbstractGcpDatabaseBaseResourceChecker();

    @Test
    public void testCreateOperationAwareCloudResource() {
        CloudResource cloudResource = CloudResource.builder()
                .type(ResourceType.GCP_DATABASE)
                .name("test-image")
                .status(CommonStatus.REQUESTED)
                .params(Map.of())
                .build();
        com.google.api.services.sqladmin.model.Operation operation = new com.google.api.services.sqladmin.model.Operation();
        operation.setName("op1");
        CloudResource operationAwareCloudResource = underTest.createOperationAwareCloudResource(cloudResource, operation);
        Assert.assertEquals(false, operationAwareCloudResource.isPersistent());
        Assert.assertEquals("op1", operationAwareCloudResource.getStringParameter("opid"));
        Assert.assertEquals(ResourceType.GCP_DATABASE, operationAwareCloudResource.getType());
    }

}