package com.sequenceiq.periscope.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;

@ExtendWith(MockitoExtension.class)
class CloudbreakVersionServiceTest {

    private static final String TEST_STACK_CRN_1 = "crn:cdp:datahub:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:cluster:94ec71ec-068c-4ccb-b848-5cacaa67572e";

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @InjectMocks
    private CloudbreakVersionService underTest;

    @Test
    void testCloudbreakSaltStateVersion() {
        AutoscaleStackV4Response mockResponse = mock(AutoscaleStackV4Response.class);
        doReturn(mockResponse).when(cloudbreakCommunicator).getAutoscaleClusterByCrn(anyString());

        underTest.getCloudbreakSaltStateVersionByStackCrn(TEST_STACK_CRN_1);

        verify(cloudbreakCommunicator, times(1)).getAutoscaleClusterByCrn(anyString());
        verify(mockResponse, times(1)).getSaltCbVersion();
    }
}