package com.sequenceiq.cloudbreak.cloud.aws

import java.util.ArrayList

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.test.util.ReflectionTestUtils

import com.amazonaws.services.cloudformation.model.Tag
import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

@RunWith(MockitoJUnitRunner::class)
class AwsTagPreparationServiceTest {

    @InjectMocks
    private val awsTagPreparationService: AwsTagPreparationService? = null

    @Test
    fun testWhenCustomTagsDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "test1")
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", Lists.asList("test2:abc", arrayOf("test3:def")))
        awsTagPreparationService!!.init()
        val tags = awsTagPreparationService.prepareTags(authenticatedContext())
        Assert.assertEquals(4, tags.size.toLong())
    }

    @Test
    fun testWhenCustomTagsNotDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "test1")
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", ArrayList<Any>())
        awsTagPreparationService!!.init()
        val tags = awsTagPreparationService.prepareTags(authenticatedContext())
        Assert.assertEquals(2, tags.size.toLong())
    }

    @Test
    fun testWhenDefaultAndCustomTagsNotDefined() {
        ReflectionTestUtils.setField(awsTagPreparationService, "defaultCloudformationTag", "")
        ReflectionTestUtils.setField(awsTagPreparationService, "customCloudformationTags", ArrayList<Any>())
        awsTagPreparationService!!.init()
        val tags = awsTagPreparationService.prepareTags(authenticatedContext())
        Assert.assertEquals(1, tags.size.toLong())
    }

    private fun authenticatedContext(): AuthenticatedContext {
        val cloudContext = CloudContext(1L, "testname", "AWS", "owner")
        val cloudCredential = CloudCredential(1L, "credentialname", "sshkey", "loginuser")
        return AuthenticatedContext(cloudContext, cloudCredential)
    }
}