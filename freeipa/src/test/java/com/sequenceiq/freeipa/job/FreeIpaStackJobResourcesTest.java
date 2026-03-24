package com.sequenceiq.freeipa.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.freeipa.entity.Stack;

class FreeIpaStackJobResourcesTest {

    @Test
    void fromStackMatchesGetJobResourceShape() {
        Stack stack = new Stack();
        stack.setId(7L);
        stack.setResourceCrn("crn:cdp:freeipa:us-west-1:acct:freeipa:name");
        stack.setName("ipa-1");
        stack.setCloudPlatform("AWS");

        JobResource jobResource = FreeIpaStackJobResources.fromStack(stack);

        assertThat(jobResource.getLocalId()).isEqualTo("7");
        assertThat(jobResource.getRemoteResourceId()).isEqualTo("crn:cdp:freeipa:us-west-1:acct:freeipa:name");
        assertThat(jobResource.getName()).isEqualTo("ipa-1");
        assertThat(jobResource.getProvider()).contains("AWS");
    }

    @Test
    void fromStackAllowsNullCloudPlatform() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setResourceCrn("crn:test-ipa");
        stack.setName("n");
        stack.setCloudPlatform(null);

        JobResource jobResource = FreeIpaStackJobResources.fromStack(stack);

        assertThat(jobResource.getProvider()).isEmpty();
    }
}
