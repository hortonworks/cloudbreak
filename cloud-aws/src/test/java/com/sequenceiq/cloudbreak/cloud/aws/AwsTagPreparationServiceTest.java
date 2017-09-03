package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.cloudformation.model.Tag;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@RunWith(MockitoJUnitRunner.class)
public class AwsTagPreparationServiceTest {

    @InjectMocks
    private AwsTagPreparationService awsTagPreparationService;

    @Test
    public void testWhenCustomTagsDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "test1");
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", Lists.asList("test2:abc", new String[]{"test3:def"}));
        awsTagPreparationService.init();
        Collection<Tag> tags = awsTagPreparationService.prepareTags(authenticatedContext(), Maps.newHashMap());
        Assert.assertEquals(4, tags.size());
    }

    @Test
    public void testWhenCustomAndUserTagsDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "test1");
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", Lists.asList("test2:abc", new String[]{"test3:def"}));
        awsTagPreparationService.init();
        Map<String, String> userDefined = Maps.newHashMap();
        userDefined.put("userdefinedkey", "userdefinedvalue");
        Collection<Tag> tags = awsTagPreparationService.prepareTags(authenticatedContext(), userDefined);
        Assert.assertEquals(5, tags.size());
    }

    @Test
    public void testWhenCustomTagsNotDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "test1");
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", new ArrayList<>());
        awsTagPreparationService.init();
        Collection<Tag> tags = awsTagPreparationService.prepareTags(authenticatedContext(), Maps.newHashMap());
        Assert.assertEquals(2, tags.size());
    }

    @Test
    public void testWhenDefaultAndCustomTagsNotDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "");
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", new ArrayList<>());
        awsTagPreparationService.init();
        Collection<Tag> tags = awsTagPreparationService.prepareTags(authenticatedContext(), Maps.newHashMap());
        Assert.assertEquals(1, tags.size());
    }

    private AuthenticatedContext authenticatedContext() {
        CloudContext cloudContext = new CloudContext(1L, "testname", "AWS", "owner");
        CloudCredential cloudCredential = new CloudCredential(1L, "credentialname");
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }
}