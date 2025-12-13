package com.sequenceiq.cloudbreak.cloud.gcp.setup;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpStorageFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;

@ExtendWith(MockitoExtension.class)
public class GcpBucketRegisterServiceTest {

    @InjectMocks
    public GcpBucketRegisterService underTest;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Mock
    private GcpStorageFactory gcpStorageFactory;

    @Test
    public void testRegisterBucketWhenEverythingWorksFine() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Storage storage = mock(Storage.class);
        Storage.Buckets buckets = mock(Storage.Buckets.class);
        Storage.Buckets.Insert insert = mock(Storage.Buckets.Insert.class);
        Storage.Buckets.Get get = mock(Storage.Buckets.Get.class);
        Location location = location(region("region"));

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getName()).thenReturn("name");
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(cloudContext.getAccountId()).thenReturn("id");
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpLabelUtil.transformLabelKeyOrValue(anyString())).thenReturn("super-bucket");
        when(storage.buckets()).thenReturn(buckets);
        when(buckets.get(anyString())).thenReturn(get);
        when(get.execute()).thenReturn(new Bucket());

        String bucketName = underTest.register(authenticatedContext);

        assertEquals("super-bucket", bucketName);
        verify(insert, times(0)).execute();
        verify(get, times(1)).execute();
    }

    @Test
    public void testRegisterBucketWhenBucketGetThrowGoogleJsonResponseExceptionWithNONotFoundShouldCreateBucket() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Storage storage = mock(Storage.class);
        Storage.Buckets buckets = mock(Storage.Buckets.class);
        Storage.Buckets.Insert insert = mock(Storage.Buckets.Insert.class);
        Storage.Buckets.Get get = mock(Storage.Buckets.Get.class);
        Location location = location(region("region"));

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getName()).thenReturn("name");
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(cloudContext.getAccountId()).thenReturn("id");
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpLabelUtil.transformLabelKeyOrValue(anyString())).thenReturn("super-bucket");
        when(storage.buckets()).thenReturn(buckets);
        when(buckets.get(anyString())).thenReturn(get);
        when(get.execute()).thenThrow(GoogleJsonResponseException.class);
        when(buckets.insert(anyString(), any(Bucket.class))).thenReturn(insert);
        when(insert.execute()).thenReturn(new Bucket());

        String bucketName = underTest.register(authenticatedContext);

        assertEquals("super-bucket", bucketName);
        verify(insert, times(1)).execute();
        verify(get, times(1)).execute();
    }

    @Test
    public void testRegisterBucketWhenBucketGetThrowGoogleJsonResponseExceptionWithNotFoundShouldCreateBucket() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Storage storage = mock(Storage.class);
        Storage.Buckets buckets = mock(Storage.Buckets.class);
        Storage.Buckets.Insert insert = mock(Storage.Buckets.Insert.class);
        Storage.Buckets.Get get = mock(Storage.Buckets.Get.class);
        Location location = location(region("region"));

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getName()).thenReturn("name");
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(cloudContext.getAccountId()).thenReturn("id");
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpLabelUtil.transformLabelKeyOrValue(anyString())).thenReturn("super-bucket");
        when(storage.buckets()).thenReturn(buckets);
        when(buckets.get(anyString())).thenReturn(get);
        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);
        when(googleJsonResponseException.getStatusCode()).thenReturn(404);
        when(get.execute()).thenThrow(googleJsonResponseException);
        when(buckets.insert(anyString(), any(Bucket.class))).thenReturn(insert);
        when(insert.execute()).thenReturn(new Bucket());

        String bucketName = underTest.register(authenticatedContext);

        assertEquals("super-bucket", bucketName);
        verify(insert, times(1)).execute();
        verify(get, times(1)).execute();
    }

    @Test
    public void testRegisterBucketWhenBucketGetThrowIOExceptionWithNotFoundShouldNOTCreateBucket() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Storage storage = mock(Storage.class);
        Storage.Buckets buckets = mock(Storage.Buckets.class);
        Storage.Buckets.Insert insert = mock(Storage.Buckets.Insert.class);
        Storage.Buckets.Get get = mock(Storage.Buckets.Get.class);
        Location location = location(region("region"));

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getName()).thenReturn("name");
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(cloudContext.getAccountId()).thenReturn("id");
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpLabelUtil.transformLabelKeyOrValue(anyString())).thenReturn("super-bucket");
        when(storage.buckets()).thenReturn(buckets);
        when(buckets.get(anyString())).thenReturn(get);
        IOException ioException = mock(IOException.class);
        when(get.execute()).thenThrow(ioException);

        String bucketName = underTest.register(authenticatedContext);

        assertEquals("super-bucket", bucketName);
        verify(insert, times(0)).execute();
        verify(get, times(1)).execute();
    }

    @Test
    public void testRegisterBucketWhenBucketGetThrowGoogleJsonResponseExceptionWithNotFoundAndCreateDropExceptionShouldThrowException() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Storage storage = mock(Storage.class);
        Storage.Buckets buckets = mock(Storage.Buckets.class);
        Storage.Buckets.Insert insert = mock(Storage.Buckets.Insert.class);
        Storage.Buckets.Get get = mock(Storage.Buckets.Get.class);
        Location location = location(region("region"));

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getName()).thenReturn("name");
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(cloudContext.getAccountId()).thenReturn("id");
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpLabelUtil.transformLabelKeyOrValue(anyString())).thenReturn("super-bucket");
        when(storage.buckets()).thenReturn(buckets);
        when(buckets.get(anyString())).thenReturn(get);
        GoogleJsonError details = new GoogleJsonError();
        details.setMessage("somethingAwful");
        GoogleJsonResponseException googleJsonResponseException = new GoogleJsonResponseException(
                new HttpResponseException.Builder(404, "", new HttpHeaders()), details);
        when(get.execute()).thenThrow(googleJsonResponseException);
        when(buckets.insert(anyString(), any(Bucket.class))).thenReturn(insert);
        when(insert.execute()).thenThrow(googleJsonResponseException);

        GoogleJsonResponseException result = assertThrows(GoogleJsonResponseException.class,
                () -> underTest.register(authenticatedContext));

        assertEquals(404, result.getStatusCode());
        verify(insert, times(1)).execute();
        verify(get, times(1)).execute();
    }

    @Test
    public void testRegisterBucketWhenBucketGetThrowGoogleJsonResponseExceptionWithNotFoundAndExceptionWhichNot404ShouldThrowException() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Storage storage = mock(Storage.class);
        Storage.Buckets buckets = mock(Storage.Buckets.class);
        Storage.Buckets.Insert insert = mock(Storage.Buckets.Insert.class);
        Storage.Buckets.Get get = mock(Storage.Buckets.Get.class);
        Location location = location(region("region"));

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getName()).thenReturn("name");
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(cloudContext.getAccountId()).thenReturn("id");
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpLabelUtil.transformLabelKeyOrValue(anyString())).thenReturn("super-bucket");
        when(storage.buckets()).thenReturn(buckets);
        when(buckets.get(anyString())).thenReturn(get);
        GoogleJsonError details = new GoogleJsonError();
        details.setMessage("somethingAwful");
        GoogleJsonResponseException googleJsonResponseException = new GoogleJsonResponseException(
                new HttpResponseException.Builder(404, "", new HttpHeaders()), details);
        when(get.execute()).thenThrow(googleJsonResponseException);
        when(buckets.insert(anyString(), any(Bucket.class))).thenReturn(insert);
        when(insert.execute()).thenThrow(googleJsonResponseException);

        GoogleJsonResponseException result = assertThrows(GoogleJsonResponseException.class,
                () -> underTest.register(authenticatedContext));

        assertEquals(404, result.getStatusCode());
        verify(insert, times(1)).execute();
        verify(get, times(1)).execute();
    }

    @Test
    public void testRegisterBucketWhenBucketGetThrowGoogleJsonResponseExceptionWithNotFoundAndExceptionWhichNot405ShouldNOTThrowException() throws IOException {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Storage storage = mock(Storage.class);
        Storage.Buckets buckets = mock(Storage.Buckets.class);
        Storage.Buckets.Insert insert = mock(Storage.Buckets.Insert.class);
        Storage.Buckets.Get get = mock(Storage.Buckets.Get.class);
        Location location = location(region("region"));

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getName()).thenReturn("name");
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn("project-id");
        when(cloudContext.getAccountId()).thenReturn("id");
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(gcpStorageFactory.buildStorage(any(CloudCredential.class), anyString())).thenReturn(storage);
        when(gcpLabelUtil.transformLabelKeyOrValue(anyString())).thenReturn("super-bucket");
        when(storage.buckets()).thenReturn(buckets);
        when(buckets.get(anyString())).thenReturn(get);
        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);
        when(googleJsonResponseException.getStatusCode()).thenReturn(404);
        when(get.execute()).thenThrow(googleJsonResponseException);
        when(googleJsonResponseException.getStatusCode()).thenReturn(404);
        when(buckets.insert(anyString(), any(Bucket.class))).thenReturn(insert);
        GoogleJsonResponseException exception = mock(GoogleJsonResponseException.class);
        when(insert.execute()).thenThrow(exception);
        when(exception.getStatusCode()).thenReturn(HttpStatus.SC_CONFLICT);

        String bucketName = underTest.register(authenticatedContext);

        assertEquals("super-bucket", bucketName);
        verify(insert, times(1)).execute();
        verify(get, times(1)).execute();
    }

}