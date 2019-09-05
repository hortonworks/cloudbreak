package com.sequenceiq.it.cloudbreak.mock;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.users.WorkspaceRequest;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV3Constants;

public class MockWorkspaceCreationTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.mockworkspace.name}")
    private String defaultName;

    @Test
    @Parameters("workspaceName")
    public void testMockCredentialCreation(@Optional("") String workspaceName) {
        // GIVEN
        workspaceName = StringUtils.hasLength(workspaceName) ? workspaceName : defaultName;
        workspaceName += UUID.randomUUID();
        WorkspaceRequest workspaceRequest = new WorkspaceRequest();
        workspaceRequest.setName(workspaceName);
        workspaceRequest.setDescription("Mock workspace for integrationtest");
        // WHEN
        Long id = getCloudbreakClient().workspaceV3Endpoint().create(workspaceRequest).getId();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.WORKSPACE_ID, id);
        getItContext().putContextParam(CloudbreakV3Constants.WORKSPACE_NAME, workspaceName, true);
    }
}
