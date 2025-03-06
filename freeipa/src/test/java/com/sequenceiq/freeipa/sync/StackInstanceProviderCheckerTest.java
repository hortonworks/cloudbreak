package com.sequenceiq.freeipa.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.handler.InstanceStateQuery;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;

@ExtendWith(MockitoExtension.class)
class StackInstanceProviderCheckerTest {

    @InjectMocks
    private StackInstanceProviderChecker underTest;

    @Mock
    private InstanceStateQuery instanceStateQuery;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Test
    void checkStatusForInstancesWhenCheckingProviderFailedShouldReturnUnknownStatuses() {
        when(instanceStateQuery.getCloudVmInstanceStatusesWithoutRetry(any(), any(), anyList())).thenThrow(new RuntimeException("error"));
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = underTest.checkStatus(new Stack(), Set.of(new InstanceMetaData()));
        assertThat(cloudVmInstanceStatuses).hasSize(1);
        assertThat(cloudVmInstanceStatuses).extracting(CloudVmInstanceStatus::getStatus).containsOnly(InstanceStatus.UNKNOWN);
    }

}