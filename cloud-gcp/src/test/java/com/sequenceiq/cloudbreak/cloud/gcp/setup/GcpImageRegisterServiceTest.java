package com.sequenceiq.cloudbreak.cloud.gcp.setup;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ENVIRONMENT_RESOURCE_ENCRYPTION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.GuestOsFeature;
import com.google.api.services.compute.model.Image;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@ExtendWith(MockitoExtension.class)
public class GcpImageRegisterServiceTest {

    @InjectMocks
    public GcpImageRegisterService underTest;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Captor
    private ArgumentCaptor<Image> imageArgumentCaptor;

    @Test
    public void testRegisterImageWhenEverythingWorksFine() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack stack = mock(CloudStack.class);
        Compute compute = mock(Compute.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Insert insert = mock(Compute.Images.Insert.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(gcpStackUtil.getTarName("image-name")).thenReturn("image.tar.gz");
        when(gcpStackUtil.getImageName("image-name")).thenReturn("super-image");
        when(stack.getParameters()).thenReturn(Map.of(ENVIRONMENT_RESOURCE_ENCRYPTION_KEY, "encryptionKey"));
        when(compute.images()).thenReturn(images);
        when(images.insert(eq("project-id"), any(Image.class))).thenReturn(insert);

        underTest.register(authenticatedContext, "bucket-name", "image-name", stack);

        verify(images, times(1)).insert(eq("project-id"), imageArgumentCaptor.capture());
        Image captureImage = imageArgumentCaptor.getValue();
        assertEquals("super-image", captureImage.getName());
        assertEquals("http://storage.googleapis.com/bucket-name/image.tar.gz", captureImage.getRawDisk().getSource());
        assertEquals(2, captureImage.getGuestOsFeatures().size());
        assertThat(captureImage.getGuestOsFeatures()).extracting(GuestOsFeature::getType).containsExactlyInAnyOrder("UEFI_COMPATIBLE", "MULTI_IP_SUBNET");
        assertEquals("encryptionKey", captureImage.getImageEncryptionKey().getKmsKeyName());
        verify(insert, times(1)).execute();
    }

    @Test
    public void testRegisterImageWhenConflictHappensThenEverythingWorksFine() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack stack = mock(CloudStack.class);
        Compute compute = mock(Compute.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Insert insert = mock(Compute.Images.Insert.class);
        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(gcpStackUtil.getTarName("image-name")).thenReturn("image.tar.gz");
        when(gcpStackUtil.getImageName("image-name")).thenReturn("super-image");
        when(stack.getParameters()).thenReturn(Map.of(ENVIRONMENT_RESOURCE_ENCRYPTION_KEY, "encryptionKey"));
        when(compute.images()).thenReturn(images);
        when(images.insert(eq("project-id"), any(Image.class))).thenReturn(insert);
        when(insert.execute()).thenThrow(googleJsonResponseException);
        when(googleJsonResponseException.getStatusCode()).thenReturn(HttpStatus.SC_CONFLICT);

        underTest.register(authenticatedContext, "bucket-name", "image-name", stack);

        verify(images, times(1)).insert(eq("project-id"), imageArgumentCaptor.capture());
        Image captureImage = imageArgumentCaptor.getValue();
        assertEquals("super-image", captureImage.getName());
        assertEquals("http://storage.googleapis.com/bucket-name/image.tar.gz", captureImage.getRawDisk().getSource());
        assertEquals(2, captureImage.getGuestOsFeatures().size());
        assertThat(captureImage.getGuestOsFeatures()).extracting(GuestOsFeature::getType).containsExactlyInAnyOrder("UEFI_COMPATIBLE", "MULTI_IP_SUBNET");
        assertEquals("encryptionKey", captureImage.getImageEncryptionKey().getKmsKeyName());
        verify(insert, times(1)).execute();
    }

    @Test
    public void testRegisterImageWhenNOTConflictHappensThenRegisterShouldFail() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack stack = mock(CloudStack.class);
        Compute compute = mock(Compute.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Insert insert = mock(Compute.Images.Insert.class);
        GoogleJsonError details = new GoogleJsonError();
        details.setMessage("error");
        GoogleJsonResponseException googleJsonResponseException = new GoogleJsonResponseException(
                new HttpResponseException.Builder(404, "", new HttpHeaders()), details);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(gcpStackUtil.getTarName("image-name")).thenReturn("image.tar.gz");
        when(gcpStackUtil.getImageName("image-name")).thenReturn("super-image");
        when(stack.getParameters()).thenReturn(Map.of(ENVIRONMENT_RESOURCE_ENCRYPTION_KEY, "encryptionKey"));
        when(compute.images()).thenReturn(images);
        when(images.insert(eq("project-id"), any(Image.class))).thenReturn(insert);
        when(insert.execute()).thenThrow(googleJsonResponseException);

        GoogleJsonResponseException exception = assertThrows(GoogleJsonResponseException.class,
                () -> underTest.register(authenticatedContext, "bucket-name", "image-name", stack));

        verify(images, times(1)).insert(eq("project-id"), imageArgumentCaptor.capture());
        Image captureImage = imageArgumentCaptor.getValue();
        assertEquals("super-image", captureImage.getName());
        assertEquals("http://storage.googleapis.com/bucket-name/image.tar.gz", captureImage.getRawDisk().getSource());
        assertEquals(2, captureImage.getGuestOsFeatures().size());
        assertThat(captureImage.getGuestOsFeatures()).extracting(GuestOsFeature::getType).containsExactlyInAnyOrder("UEFI_COMPATIBLE", "MULTI_IP_SUBNET");
        assertEquals("encryptionKey", captureImage.getImageEncryptionKey().getKmsKeyName());
        verify(insert, times(1)).execute();
        assertEquals("error", exception.getDetails().getMessage());
    }
}