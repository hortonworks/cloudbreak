package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.cloudformation.model.Tag;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@RunWith(MockitoJUnitRunner.class)
public class AwsTagPreparationServiceTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    @InjectMocks
    private AwsTagPreparationService awsTagPreparationService;

    @Test
    public void testWhenCustomTagsDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "test1");
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", Lists.asList("test2:abc", new String[]{"test3:def"}));
        awsTagPreparationService.init();
        Collection<Tag> tags = awsTagPreparationService.prepareCloudformationTags(authenticatedContext(), Maps.newHashMap());
        Assert.assertEquals(4L, tags.size());
    }

    @Test
    public void testWhenCustomAndUserTagsDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "test1");
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", Lists.asList("test2:abc", new String[]{"test3:def"}));
        awsTagPreparationService.init();
        Map<String, String> userDefined = Maps.newHashMap();
        userDefined.put("userdefinedkey", "userdefinedvalue");
        Collection<Tag> tags = awsTagPreparationService.prepareCloudformationTags(authenticatedContext(), userDefined);
        Assert.assertEquals(5L, tags.size());
    }

    @Test
    public void testWhenCustomTagsNotDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "test1");
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", new ArrayList<>());
        awsTagPreparationService.init();
        Collection<Tag> tags = awsTagPreparationService.prepareCloudformationTags(authenticatedContext(), Maps.newHashMap());
        Assert.assertEquals(2L, tags.size());
    }

    @Test
    public void testWhenDefaultAndCustomTagsNotDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "");
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", new ArrayList<>());
        awsTagPreparationService.init();
        Collection<Tag> tags = awsTagPreparationService.prepareCloudformationTags(authenticatedContext(), Maps.newHashMap());
        Assert.assertEquals(1L, tags.size());
    }

    private AuthenticatedContext authenticatedContext() {
        CloudContext cloudContext = new CloudContext(1L, "testname", "AWS", USER_ID, WORKSPACE_ID);
        CloudCredential cloudCredential = new CloudCredential(1L, "credentialname");
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }
}