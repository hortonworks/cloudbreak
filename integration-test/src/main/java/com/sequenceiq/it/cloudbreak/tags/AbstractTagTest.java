package com.sequenceiq.it.cloudbreak.tags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;

public class AbstractTagTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTagTest.class);

    private Map<String, String> cloudProviderParams = new HashMap<>();

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "tags" })
    public void testTagTest(String tags) throws Exception {
        //GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        StackEndpoint stackEndpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackEndpoint();
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));

        cloudProviderParams.put("stackName", stackResponse.getName());

        Map<String, String> userDefinedTagsStack = TagsUtil.checkTagsStack(stackResponse);
        Map<String, String> tagsToCheckMap = TagsUtil.getTagsToCheck(tags);
        //WHEN: The cluster was created with user-defined tags
        //THEN
        TagsUtil.checkTags(tagsToCheckMap, userDefinedTagsStack);
        List<String> instanceIdList = TagsUtil.getInstancesList(stackResponse);
        TagsUtil.checkTagsWithProvider(cloudProviderParams, applicationContext, instanceIdList, tagsToCheckMap);
    }

    protected Map<String, String> getCloudProviderParams() {
        return cloudProviderParams;
    }
}
