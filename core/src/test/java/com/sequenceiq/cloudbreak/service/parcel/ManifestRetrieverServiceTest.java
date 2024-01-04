package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@ExtendWith(MockitoExtension.class)
public class ManifestRetrieverServiceTest {

    private static final String BASE_URL = "http://test.com/";

    @Mock
    private RestClientFactory restClientFactory;

    @Mock
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @InjectMocks
    private ManifestRetrieverService underTest;

    @Test
    void testReadRepoManifestShouldReturnManifest() {
        mockManifestResponse(Response.Status.OK, Optional.of(getManifestJson("NIFI")));

        ImmutablePair<ManifestStatus, Manifest> actual = underTest.readRepoManifest(BASE_URL);

        assertEquals(ManifestStatus.SUCCESS, actual.left);
    }

    @Test
    void testReadRepoManifestShouldReturnParseError() {
        mockManifestResponse(Response.Status.OK, Optional.of("{...NIFI"));

        ImmutablePair<ManifestStatus, Manifest> actual = underTest.readRepoManifest(BASE_URL);

        assertEquals(ManifestStatus.COULD_NOT_PARSE, actual.left);
    }

    @Test
    void testReadRepoManifestShouldReturnFailed() {
        mockManifestResponse(Response.Status.NOT_FOUND, Optional.empty());

        ImmutablePair<ManifestStatus, Manifest> actual = underTest.readRepoManifest(BASE_URL);

        assertEquals(ManifestStatus.FAILED, actual.left);
    }

    private void mockManifestResponse(Response.Status status, Optional<String> manifest) {
        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder request = mock(Invocation.Builder.class);
        Response response = mock(Response.class);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        when(client.target(BASE_URL + "manifest.json")).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
        when(request.get()).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(status);
        manifest.ifPresent(s -> when(response.readEntity(String.class)).thenReturn(s));
    }

    private static String getManifestJson(String... componentNames) {
        Manifest manifest = new Manifest();
        manifest.setLastUpdated(System.currentTimeMillis());
        Parcel parcel = new Parcel();
        List<Component> components = Arrays.stream(componentNames).map(s -> {
            Component component = new Component();
            component.setName(s);
            return component;
        }).collect(Collectors.toList());
        parcel.setComponents(components);
        manifest.setParcels(List.of(parcel));
        return JsonUtil.writeValueAsStringSilentSafe(manifest);
    }
}
