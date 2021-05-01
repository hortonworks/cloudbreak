package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.http.HttpResponse;
import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.context.InvalidGcpContextException;

@ExtendWith(MockitoExtension.class)
public class GcpCredentialVerifierTest {

    @InjectMocks
    private GcpCredentialVerifier underTest;

    @Mock
    private GcpContextBuilder contextBuilder;

    @Mock
    private Compute compute;

    @Test
    public void testCheckGcpContextValidityIfContextNullShouldThrowInvalidGcpContextException() {
        assertThrows(InvalidGcpContextException.class, () -> underTest.checkGcpContextValidity(null));
    }

    @Test
    public void testCheckGcpContextValidityIfProjectIdNullShouldThrowInvalidGcpContextException() {
        GcpContext cloudContext = new GcpContext(
                "name",
                location(region("location")),
                null,
                "serviceAccountId",
                compute,
                false,
                1,
                true);
        assertThrows(InvalidGcpContextException.class, () -> underTest.checkGcpContextValidity(cloudContext));
    }

    @Test
    public void testCheckGcpContextValidityIfServiceAccountIdNullShouldThrowInvalidGcpContextException() {
        GcpContext cloudContext = new GcpContext(
                "name",
                location(region("location")),
                "projectId",
                null,
                compute,
                false,
                1,
                true);
        assertThrows(InvalidGcpContextException.class, () -> underTest.checkGcpContextValidity(cloudContext));
    }

    @Test
    public void testCheckGcpContextValidityIfComputeNullShouldThrowInvalidGcpContextException() {
        GcpContext cloudContext = new GcpContext(
                "name",
                location(region("location")),
                "projectId",
                "serviceAccountId",
                null,
                false,
                1,
                true);
        assertThrows(InvalidGcpContextException.class, () -> underTest.checkGcpContextValidity(cloudContext));
    }

    @Test
    public void testPreCheckOfGooglePermissionWhenEverythingWorksFine() throws IOException {
        Compute.Regions regions = mock(Compute.Regions.class);
        Compute.Regions.List list = mock(Compute.Regions.List.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(list.executeUsingHead()).thenReturn(httpResponse);
        when(regions.list(anyString())).thenReturn(list);
        when(compute.regions()).thenReturn(regions);
        GcpContext cloudContext = new GcpContext(
                "name",
                location(region("location")),
                "projectId",
                "serviceAccountId",
                compute,
                false,
                1,
                true);
        underTest.preCheckOfGooglePermission(cloudContext);
    }
}