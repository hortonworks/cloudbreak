package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;

@ExtendWith(MockitoExtension.class)
public class ManifestRetrieverServiceTest {

    private static final String BASE_URL = "http://test.com";

    @Mock
    private RestClientFactory restClientFactory;

    @Mock
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @InjectMocks
    private ManifestRetrieverService underTest;

    @Test
    void testReadRepoManifestShouldReturnManifest() {
        Response response = mock(Response.class);
        Manifest manifest = mock(Manifest.class);
        when(response.readEntity(Manifest.class)).thenReturn(manifest);
        mockManifestResponse(Response.Status.OK, response);

        ImmutablePair<ManifestStatus, Manifest> actual = underTest.readRepoManifest(BASE_URL);

        assertEquals(ManifestStatus.SUCCESS, actual.left);
        assertEquals(manifest, actual.right);
    }

    @Test
    void testReadRepoManifestShouldReturnParseError() {
        Response response = mock(Response.class);
        when(response.readEntity(Manifest.class)).thenThrow(new ProcessingException("Json processing failed."));
        mockManifestResponse(Response.Status.OK, response);

        ImmutablePair<ManifestStatus, Manifest> actual = underTest.readRepoManifest(BASE_URL);

        assertEquals(ManifestStatus.COULD_NOT_PARSE, actual.left);
    }

    @Test
    void testReadRepoManifestShouldReturnFailed() {
        Response response = mock(Response.class);
        mockManifestResponse(Response.Status.NOT_FOUND, response);

        ImmutablePair<ManifestStatus, Manifest> actual = underTest.readRepoManifest(BASE_URL);

        assertEquals(ManifestStatus.FAILED, actual.left);
    }

    private void mockManifestResponse(Response.Status status, Response response) {
        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder request = mock(Invocation.Builder.class);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        when(client.target(BASE_URL + "/manifest.json")).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
        when(request.get()).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(status);
    }
}