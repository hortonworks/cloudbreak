package com.sequenceiq.cloudbreak.cloud.gcp.setup;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType.EXECUTED_DURING_PROVISIONING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Image;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.RewriteResponse;
import com.google.api.services.storage.model.StorageObject;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpStorageFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ImageStatus;
import com.sequenceiq.common.api.type.ImageStatusResult;

@ExtendWith(MockitoExtension.class)
public class GcpProvisionSetupTest {

    @InjectMocks
    public GcpProvisionSetup underTest;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private GcpStorageFactory gcpStorageFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Mock
    private GcpBucketRegisterService gcpBucketRegisterService;

    @Mock
    private GcpImageRegisterService gcpImageRegisterService;

    @Mock
    private GcpImageAttemptMakerFactory gcpImageAttemptMakerFactory;

    @Test
    public void testPrerequisites() {
        underTest.prerequisites(
                mock(AuthenticatedContext.class),
                mock(CloudStack.class),
                mock(PersistenceNotifier.class)
        );
    }

    @Test
    public void testValidateFileSystem() {
        underTest.validateFileSystem(
                mock(CloudCredential.class),
                mock(SpiFileSystem.class)
        );
    }

    @Test
    public void testValidateParameters() {
        underTest.validateParameters(
                mock(AuthenticatedContext.class),
                mock(Map.class)
        );
    }

    @Test
    public void testScalingPrerequisites() {
        underTest.scalingPrerequisites(
                mock(AuthenticatedContext.class),
                mock(CloudStack.class),
                true
        );
    }

    @Test
    public void testCheckImageStatusWhenImageIsReady() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        Compute compute = mock(Compute.class);
        Compute.Images images = mock(Compute.Images.class);
        Image imageGoogle = mock(Image.class);
        Compute.Images.Get imagesGet = mock(Compute.Images.Get.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        com.sequenceiq.cloudbreak.cloud.model.Image image = createImage();

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(compute.images()).thenReturn(images);
        when(images.get(anyString(), anyString())).thenReturn(imagesGet);
        when(imagesGet.execute()).thenReturn(imageGoogle);
        when(imageGoogle.getStatus()).thenReturn("READY");
        ImageStatusResult imageStatusResult = underTest.checkImageStatus(
                authenticatedContext,
                cloudStack,
                image
        );
        assertEquals(ImageStatus.CREATE_FINISHED, imageStatusResult.getImageStatus());
        assertEquals(Integer.valueOf(100), imageStatusResult.getStatusProgressValue());
    }

    @Test
    public void testCheckImageStatusWhenImageIsStillNotReady() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        Compute compute = mock(Compute.class);
        Compute.Images images = mock(Compute.Images.class);
        Image imageGoogle = mock(Image.class);
        Compute.Images.Get imagesGet = mock(Compute.Images.Get.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        com.sequenceiq.cloudbreak.cloud.model.Image image = createImage();

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(compute.images()).thenReturn(images);
        when(images.get(anyString(), anyString())).thenReturn(imagesGet);
        when(imagesGet.execute()).thenReturn(imageGoogle);
        when(imageGoogle.getStatus()).thenReturn("IN_PROGRESS");
        ImageStatusResult imageStatusResult = underTest.checkImageStatus(
                authenticatedContext,
                cloudStack,
                image
        );
        assertEquals(ImageStatus.IN_PROGRESS, imageStatusResult.getImageStatus());
        assertEquals(Integer.valueOf(50), imageStatusResult.getStatusProgressValue());
    }

    @Test
    public void testCheckImageStatusWhenImageIsDropTokenExceptionShouldDropGcpResourceException() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        Compute compute = mock(Compute.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Get imagesGet = mock(Compute.Images.Get.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        com.sequenceiq.cloudbreak.cloud.model.Image image = createImage();

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(compute.images()).thenReturn(images);
        when(images.get(anyString(), anyString())).thenReturn(imagesGet);
        when(imagesGet.execute()).thenThrow(TokenResponseException.class);
        when(gcpStackUtil.getMissingServiceAccountKeyError(any(TokenResponseException.class), anyString()))
                .thenThrow(new GcpResourceException("error"));

        assertThrows(GcpResourceException.class, () -> underTest.checkImageStatus(
                authenticatedContext,
                cloudStack,
                image
        ));
    }

    @Test
    public void testCheckImageStatusWhenImageIsDropIOExceptionShouldCreateFailed() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        Compute compute = mock(Compute.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Get imagesGet = mock(Compute.Images.Get.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        com.sequenceiq.cloudbreak.cloud.model.Image image = createImage();

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(compute.images()).thenReturn(images);
        when(images.get(anyString(), anyString())).thenReturn(imagesGet);
        when(imagesGet.execute()).thenThrow(IOException.class);

        ImageStatusResult imageStatusResult = underTest.checkImageStatus(
                authenticatedContext,
                cloudStack,
                image
        );
        assertEquals(ImageStatus.CREATE_FAILED, imageStatusResult.getImageStatus());
        assertEquals(Integer.valueOf(0), imageStatusResult.getStatusProgressValue());
    }

    @Test
    public void testPrepareImagewhenGoogleRespondWithBadRequet() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        Compute compute = mock(Compute.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Get imagesGet = mock(Compute.Images.Get.class);
        com.sequenceiq.cloudbreak.cloud.model.Image image = createImage();

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(compute.images()).thenReturn(images);
        when(images.get(anyString(), anyString())).thenReturn(imagesGet);
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        GoogleJsonResponseException googleError = mock(GoogleJsonResponseException.class);
        when(googleError.getMessage()).thenReturn("Google error");
        when(googleError.getSuppressed()).thenReturn(new Throwable[0]);
        when(imagesGet.execute()).thenThrow(googleError);

        CloudConnectorException actual = assertThrows(CloudConnectorException.class,
                () -> underTest.prepareImage(authenticatedContext, cloudStack, image, EXECUTED_DURING_PROVISIONING, null));

        assertTrue(actual.getMessage().contains("Google error"));

        verify(gcpStorageFactory, never()).buildStorage(any(), any());
    }

    @Test
    public void testPrepareImageWhenImageExistOnGoogle() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        Compute compute = mock(Compute.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Get imagesGet = mock(Compute.Images.Get.class);
        Image imageGoogle = mock(Image.class);
        com.sequenceiq.cloudbreak.cloud.model.Image image = createImage();

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(compute.images()).thenReturn(images);
        when(images.get(anyString(), anyString())).thenReturn(imagesGet);
        when(imagesGet.execute()).thenReturn(imageGoogle);
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");

        underTest.prepareImage(authenticatedContext, cloudStack, image, EXECUTED_DURING_PROVISIONING, null);
    }

    @Test
    public void testPrepareImageDoesNotExistAndImageCopyHappensSuccessfully() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        CloudContext context = mock(CloudContext.class);
        Compute compute = mock(Compute.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Get imagesGet = mock(Compute.Images.Get.class);
        GcpImageAttemptMaker gcpImageAttemptMaker = mock(GcpImageAttemptMaker.class);
        Storage storage = mock(Storage.class);
        Storage.Objects storageObjects = mock(Storage.Objects.class);
        Storage.Objects.Rewrite storageObjectsRewrite = mock(Storage.Objects.Rewrite.class);
        RewriteResponse rewriteResponse = mock(RewriteResponse.class);
        com.sequenceiq.cloudbreak.cloud.model.Image image = createImage();
        GoogleJsonResponseException notFoundImageException = mock(GoogleJsonResponseException.class);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(context);
        when(context.getName()).thenReturn("context");
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(compute.images()).thenReturn(images);
        when(images.get(anyString(), anyString())).thenReturn(imagesGet);
        when(imagesGet.execute()).thenThrow(notFoundImageException);
        when(notFoundImageException.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(gcpStackUtil.getTarName(anyString())).thenReturn("tarname");
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpStackUtil.getBucket(anyString())).thenReturn("bucketName");
        when(gcpBucketRegisterService.register(any(AuthenticatedContext.class))).thenReturn("bucket");
        doNothing().when(gcpImageRegisterService).register(any(AuthenticatedContext.class), anyString(), anyString(), any(CloudStack.class));
        when(storage.objects()).thenReturn(storageObjects);
        when(rewriteResponse.getRewriteToken()).thenReturn("token");
        when(storageObjects.rewrite(anyString(), anyString(), anyString(), anyString(), any(StorageObject.class))).thenReturn(storageObjectsRewrite);
        when(storageObjectsRewrite.execute()).thenReturn(rewriteResponse);
        when(gcpImageAttemptMakerFactory.create(anyString(), anyString(), anyString(), anyString(), anyString(), any(Storage.class)))
                .thenReturn(gcpImageAttemptMaker);
        when(gcpImageAttemptMaker.process()).thenReturn(AttemptResults.justFinish());

        underTest.prepareImage(authenticatedContext, cloudStack, image, EXECUTED_DURING_PROVISIONING, null);

        verify(gcpImageAttemptMakerFactory, times(1))
                .create(anyString(), anyString(), anyString(), anyString(), anyString(), any(Storage.class));
        verify(gcpBucketRegisterService, times(1)).register(any(AuthenticatedContext.class));
        verify(gcpImageRegisterService, times(1))
                .register(any(AuthenticatedContext.class), anyString(), anyString(), any(CloudStack.class));
    }

    @Test
    public void testPrepareImageDoesNotExistAndAndPollerStoppedExceptionHappensAndCloudbreakServiceExceptionThrows() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        CloudContext context = mock(CloudContext.class);
        Compute compute = mock(Compute.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Get imagesGet = mock(Compute.Images.Get.class);
        GcpImageAttemptMaker gcpImageAttemptMaker = mock(GcpImageAttemptMaker.class);
        Storage storage = mock(Storage.class);
        Storage.Objects storageObjects = mock(Storage.Objects.class);
        Storage.Objects.Rewrite storageObjectsRewrite = mock(Storage.Objects.Rewrite.class);
        RewriteResponse rewriteResponse = mock(RewriteResponse.class);
        com.sequenceiq.cloudbreak.cloud.model.Image image = createImage();

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(context);
        when(context.getName()).thenReturn("context");
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(compute.images()).thenReturn(images);
        when(images.get(anyString(), anyString())).thenReturn(imagesGet);
        when(imagesGet.execute()).thenReturn(null);
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(gcpStackUtil.getTarName(anyString())).thenReturn("tarname");
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpStackUtil.getBucket(anyString())).thenReturn("bucketName");
        when(gcpBucketRegisterService.register(any(AuthenticatedContext.class))).thenReturn("bucket");
        when(storage.objects()).thenReturn(storageObjects);
        when(rewriteResponse.getRewriteToken()).thenReturn("token");
        when(storageObjects.rewrite(anyString(), anyString(), anyString(), anyString(), any(StorageObject.class))).thenReturn(storageObjectsRewrite);
        when(storageObjectsRewrite.execute()).thenReturn(rewriteResponse);
        when(gcpImageAttemptMakerFactory.create(anyString(), anyString(), anyString(), anyString(), anyString(), any(Storage.class)))
                .thenReturn(gcpImageAttemptMaker);
        when(gcpImageAttemptMaker.process()).thenThrow(PollerStoppedException.class);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.prepareImage(authenticatedContext, cloudStack, image, EXECUTED_DURING_PROVISIONING, null));

        verify(gcpImageAttemptMakerFactory, times(1))
                .create(anyString(), anyString(), anyString(), anyString(), anyString(), any(Storage.class));
        verify(gcpBucketRegisterService, times(1)).register(any(AuthenticatedContext.class));
        verify(gcpImageRegisterService, times(0))
                .register(any(AuthenticatedContext.class), anyString(), anyString(), any(CloudStack.class));
        assertTrue(cloudConnectorException.getMessage().contains("Image copy failed because the copy take too long time"));
    }

    @Test
    public void testPrepareImageDoesNotExistAndAndPollerExceptionHappensAndCloudbreakServiceExceptionThrows() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        CloudContext context = mock(CloudContext.class);
        Compute compute = mock(Compute.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Get imagesGet = mock(Compute.Images.Get.class);
        GcpImageAttemptMaker gcpImageAttemptMaker = mock(GcpImageAttemptMaker.class);
        Storage storage = mock(Storage.class);
        Storage.Objects storageObjects = mock(Storage.Objects.class);
        Storage.Objects.Rewrite storageObjectsRewrite = mock(Storage.Objects.Rewrite.class);
        RewriteResponse rewriteResponse = mock(RewriteResponse.class);
        com.sequenceiq.cloudbreak.cloud.model.Image image = createImage();

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(context);
        when(context.getName()).thenReturn("context");
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(compute.images()).thenReturn(images);
        when(images.get(anyString(), anyString())).thenReturn(imagesGet);
        when(imagesGet.execute()).thenReturn(null);
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(gcpStackUtil.getTarName(anyString())).thenReturn("tarname");
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpStackUtil.getBucket(anyString())).thenReturn("bucketName");
        when(gcpBucketRegisterService.register(any(AuthenticatedContext.class))).thenReturn("bucket");
        when(storage.objects()).thenReturn(storageObjects);
        when(rewriteResponse.getRewriteToken()).thenReturn("token");
        when(storageObjects.rewrite(anyString(), anyString(), anyString(), anyString(), any(StorageObject.class))).thenReturn(storageObjectsRewrite);
        when(storageObjectsRewrite.execute()).thenReturn(rewriteResponse);
        when(gcpImageAttemptMakerFactory.create(anyString(), anyString(), anyString(), anyString(), anyString(), any(Storage.class)))
                .thenReturn(gcpImageAttemptMaker);
        when(gcpImageAttemptMaker.process()).thenThrow(InterruptedException.class);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.prepareImage(authenticatedContext, cloudStack, image, EXECUTED_DURING_PROVISIONING, null));

        verify(gcpImageAttemptMakerFactory, times(1))
                .create(anyString(), anyString(), anyString(), anyString(), anyString(), any(Storage.class));
        verify(gcpBucketRegisterService, times(1)).register(any(AuthenticatedContext.class));
        verify(gcpImageRegisterService, times(0))
                .register(any(AuthenticatedContext.class), anyString(), anyString(), any(CloudStack.class));
        assertTrue(cloudConnectorException.getMessage().contains("Image copy failed because"));
    }

    @Test
    public void testPrepareImageDoesNotExistAndAndCloudBreakExceptionHappensAndCloudbreakServiceExceptionThrows() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        CloudContext context = mock(CloudContext.class);
        Compute compute = mock(Compute.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        Compute.Images images = mock(Compute.Images.class);
        Compute.Images.Get imagesGet = mock(Compute.Images.Get.class);
        Storage storage = mock(Storage.class);
        Storage.Objects storageObjects = mock(Storage.Objects.class);
        Storage.Objects.Rewrite storageObjectsRewrite = mock(Storage.Objects.Rewrite.class);
        com.sequenceiq.cloudbreak.cloud.model.Image image = createImage();

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(context);
        when(context.getName()).thenReturn("context");
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(compute.images()).thenReturn(images);
        when(images.get(anyString(), anyString())).thenReturn(imagesGet);
        when(imagesGet.execute()).thenReturn(null);
        when(gcpStackUtil.getImageName(anyString())).thenReturn("super-image");
        when(gcpStackUtil.getTarName(anyString())).thenReturn("tarname");
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpStackUtil.getBucket(anyString())).thenReturn("bucketName");
        when(gcpBucketRegisterService.register(any(AuthenticatedContext.class))).thenReturn("bucket");
        when(storage.objects()).thenReturn(storageObjects);
        when(storageObjects.rewrite(anyString(), anyString(), anyString(), anyString(), any(StorageObject.class))).thenReturn(storageObjectsRewrite);
        when(storageObjectsRewrite.execute()).thenThrow(IOException.class);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.prepareImage(authenticatedContext, cloudStack, image, EXECUTED_DURING_PROVISIONING, null));

        assertTrue(cloudConnectorException.getMessage().contains("Copying the image could not be started, " +
                "please check whether you have given access to CDP for storage API."));
        verify(gcpImageAttemptMakerFactory, times(0))
                .create(anyString(), anyString(), anyString(), anyString(), anyString(), any(Storage.class));
        verify(gcpBucketRegisterService, times(1)).register(any(AuthenticatedContext.class));
        verify(gcpImageRegisterService, times(0))
                .register(any(AuthenticatedContext.class), anyString(), anyString(), any(CloudStack.class));
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createImage() {
        return new com.sequenceiq.cloudbreak.cloud.model.Image(
                "super-image",
                Map.of(),
                "centos",
                "redhat",
                "x86_64",
                "http://url",
                "default",
                "1234-1234-123-123",
                Map.of(),
                "2019-10-24",
                1571884856L,
                null
        );
    }

}