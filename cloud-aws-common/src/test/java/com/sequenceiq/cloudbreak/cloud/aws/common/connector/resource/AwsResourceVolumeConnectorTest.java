package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsCommonDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

@ExtendWith(MockitoExtension.class)
public class AwsResourceVolumeConnectorTest {

    @Mock
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @InjectMocks
    private AwsResourceVolumeConnector underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudResource cloudResource;

    @Test
    public void testDetachVolumes() throws Exception {
        underTest.detachVolumes(authenticatedContext, List.of(cloudResource));
        verify(awsCommonDiskUpdateService).detachVolumes(authenticatedContext, List.of(cloudResource));
    }

    @Test
    public void testDetachVolumesException() throws Exception {
        doThrow(AwsServiceException.builder().message("TEST").build()).when(awsCommonDiskUpdateService).detachVolumes(authenticatedContext,
                List.of(cloudResource));
        AwsServiceException exception = assertThrows(AwsServiceException.class, () -> underTest.detachVolumes(authenticatedContext, List.of(cloudResource)));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    public void testDeleteVolumes() throws Exception {
        underTest.deleteVolumes(authenticatedContext, List.of(cloudResource));
        verify(awsCommonDiskUpdateService).deleteVolumes(authenticatedContext, List.of(cloudResource));
    }

    @Test
    public void testDeleteVolumesException() throws Exception {
        doThrow(AwsServiceException.builder().message("TEST").build()).when(awsCommonDiskUpdateService).deleteVolumes(authenticatedContext,
                List.of(cloudResource));
        AwsServiceException exception = assertThrows(AwsServiceException.class, () -> underTest.deleteVolumes(authenticatedContext, List.of(cloudResource)));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    public void testUpdateDiskVolumes() throws Exception {
        underTest.updateDiskVolumes(authenticatedContext, List.of("TEST-VOLUME"), "TEST", 100);
        verify(awsCommonDiskUpdateService).modifyVolumes(authenticatedContext, List.of("TEST-VOLUME"), "TEST", 100);
    }

    @Test
    public void testUpdateDiskVolumesException() throws Exception {
        doThrow(AwsServiceException.builder().message("TEST").build()).when(awsCommonDiskUpdateService).modifyVolumes(authenticatedContext,
                List.of("TEST-VOLUME"), "TEST", 100);
        AwsServiceException exception = assertThrows(AwsServiceException.class, () -> underTest.updateDiskVolumes(authenticatedContext,
                List.of("TEST-VOLUME"), "TEST", 100));
        assertEquals("TEST", exception.getMessage());
    }
}
