package com.sequenceiq.cloudbreak.service.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.handler.InstanceStateQuery;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;

@ExtendWith(MockitoExtension.class)
class StackInstanceStatusCheckerTest {

    @InjectMocks
    private StackInstanceStatusChecker underTest;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Mock
    private InstanceStateQuery instanceStateQuery;

    @Mock
    private EnvironmentInternalCrnClient environmentInternalCrnClient;

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private CredentialToCloudCredentialConverter cloudCredentialConverter;

    @Mock
    private EnvironmentServiceCrnEndpoints environmentServiceCrnEndpoints;

    @Mock
    private CredentialEndpoint credentialEndpoint;

    private Stack stack;

    private Collection<InstanceMetaData> instanceMetaData;

    @BeforeEach
    void setUp() {
        stack = TestUtil.stack();
        instanceMetaData = new HashSet<>();
    }

    private void setUpCredentials() {
        when(environmentInternalCrnClient.withInternalCrn()).thenReturn(environmentServiceCrnEndpoints);
        when(environmentServiceCrnEndpoints.credentialV1Endpoint()).thenReturn(credentialEndpoint);
    }

    @Test
    void shouldNotQueryWithEmptyInstances() {
        underTest.queryInstanceStatuses(stack, instanceMetaData);

        verify(instanceStateQuery, never()).getCloudVmInstanceStatuses(any(), any(), any());
    }

    @Test
    void shouldQueryWhenInstancesArePresent() {
        setUpCredentials();
        instanceMetaData.add(new InstanceMetaData());

        underTest.queryInstanceStatuses(stack, instanceMetaData);

        verify(instanceStateQuery).getCloudVmInstanceStatuses(any(), any(), any());
    }

    @Test
    void shouldReportUnknownStatusWhenStatusQueryFails() {
        setUpCredentials();
        instanceMetaData.add(new InstanceMetaData());
        when(instanceStateQuery.getCloudVmInstanceStatuses(any(), any(), any())).thenThrow(RuntimeException.class);

        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = underTest.queryInstanceStatuses(stack, instanceMetaData);

        Assertions.assertThat(cloudVmInstanceStatuses)
                .allMatch(cloudVmInstanceStatus -> cloudVmInstanceStatus.getStatus().equals(InstanceStatus.UNKNOWN));
    }

}
