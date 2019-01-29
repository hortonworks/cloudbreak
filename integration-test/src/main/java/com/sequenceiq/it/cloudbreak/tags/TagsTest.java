package com.sequenceiq.it.cloudbreak.tags;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;

public class TagsTest extends AbstractCloudbreakIntegrationTest {

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class),
                "Cloudprovider parameters are mandatory.");
    }

    @Test
    @Parameters("tags")
    public void testTagTest(String tags) throws Exception {
        //GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        Map<String, String> cloudProviderParams = itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class);
        var stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackV4Endpoint();
        var stackResponse = stackV1Endpoint.get(workspaceId, stackName, new HashSet<>());

        Map<String, String> userDefinedTagsStack = TagsUtil.checkTagsStack(stackResponse);
        Map<String, String> tagsToCheckMap = TagsUtil.getTagsToCheck(tags);
        //WHEN: The cluster was created with user-defined tags
        //THEN
        TagsUtil.checkTags(tagsToCheckMap, userDefinedTagsStack);
        List<String> instanceIdList = TagsUtil.getInstancesList(stackResponse);
        TagsUtil.checkTagsWithProvider(stackResponse.getName(), cloudProviderParams, applicationContext, instanceIdList, tagsToCheckMap);
    }
}
