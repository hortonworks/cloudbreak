package com.sequenceiq.cloudbreak.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.view.StackView;

class StackViewJobResourcesTest {

    @Test
    void fromStackViewMatchesStackRepositoryJobResourceShape() {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(42L);
        when(stack.getResourceCrn()).thenReturn("crn:cdp:environments:us-west-1:account:stack:name");
        when(stack.getName()).thenReturn("my-stack");
        when(stack.getCloudPlatform()).thenReturn("AWS");

        JobResource jobResource = StackViewJobResources.fromStackView(stack);

        assertThat(jobResource.getLocalId()).isEqualTo("42");
        assertThat(jobResource.getRemoteResourceId()).isEqualTo("crn:cdp:environments:us-west-1:account:stack:name");
        assertThat(jobResource.getName()).isEqualTo("my-stack");
        assertThat(jobResource.getProvider()).contains("AWS");
    }

    @Test
    void fromStackViewAllowsNullCloudPlatform() {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getResourceCrn()).thenReturn("crn:test");
        when(stack.getName()).thenReturn("n");
        when(stack.getCloudPlatform()).thenReturn(null);

        JobResource jobResource = StackViewJobResources.fromStackView(stack);

        assertThat(jobResource.getProvider()).isEmpty();
    }
}
