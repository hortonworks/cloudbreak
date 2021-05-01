package com.sequenceiq.cloudbreak.cloud.gcp.setup;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
public class GcpImageRegisterServiceTest {

    @InjectMocks
    public GcpImageRegisterService underTest;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Test
    public void testRegisterImageWhenEverythingWorksFine() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        Compute compute = mock(Compute.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Insert insert = mock(Compute.Images.Insert.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(gcpStackUtil.getTarName(anyString())).thenReturn("image.tar.gz");
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(compute.images()).thenReturn(images);
        when(images.insert(anyString(), any(Image.class))).thenReturn(insert);
        when(insert.execute()).thenReturn(mock(Operation.class));

        underTest.register(authenticatedContext, "bucket-name", "image-name");

        verify(insert, times(1)).execute();
    }

    @Test
    public void testRegisterImageWhenConflictHappensThenEverythingWorksFine() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        Compute compute = mock(Compute.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Insert insert = mock(Compute.Images.Insert.class);
        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(gcpStackUtil.getTarName(anyString())).thenReturn("image.tar.gz");
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(compute.images()).thenReturn(images);
        when(images.insert(anyString(), any(Image.class))).thenReturn(insert);
        when(insert.execute()).thenThrow(googleJsonResponseException);
        when(googleJsonResponseException.getStatusCode()).thenReturn(HttpStatus.SC_CONFLICT);

        underTest.register(authenticatedContext, "bucket-name", "image-name");

        verify(insert, times(1)).execute();
    }

    @Test
    public void testRegisterImageWhenNOTConflictHappensThenRegisterShouldFail() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        Compute compute = mock(Compute.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Insert insert = mock(Compute.Images.Insert.class);
        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);
        GoogleJsonError googleJsonError = mock(GoogleJsonError.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(gcpStackUtil.getTarName(anyString())).thenReturn("image.tar.gz");
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(compute.images()).thenReturn(images);
        when(images.insert(anyString(), any(Image.class))).thenReturn(insert);
        when(insert.execute()).thenThrow(googleJsonResponseException);
        when(googleJsonResponseException.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(googleJsonResponseException.getDetails()).thenReturn(googleJsonError);
        when(googleJsonError.getMessage()).thenReturn("error");

        GoogleJsonResponseException exception = assertThrows(GoogleJsonResponseException.class,
                () -> underTest.register(authenticatedContext, "bucket-name", "image-name"));

        verify(insert, times(1)).execute();
        Assert.assertTrue(exception.getDetails().getMessage().equals("error"));
    }
}