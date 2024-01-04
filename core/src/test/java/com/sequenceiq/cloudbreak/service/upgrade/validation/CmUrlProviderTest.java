package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM_BUILD_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@ExtendWith(MockitoExtension.class)
class CmUrlProviderTest {

    @Mock
    private RestClientFactory restClientFactory;

    @Mock
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @InjectMocks
    private CmUrlProvider underTest;

    @Test
    public void testUrlFromManifest() throws IOException {
        Image image = mock(Image.class);
        when(image.getOsType()).thenReturn("redhat7");
        when(image.getRepo()).thenReturn(Map.of("redhat7", "https://archive.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/"));
        when(image.getPackageVersions()).thenReturn(Map.of(CM.getKey(), "7.6.0", CM_BUILD_NUMBER.getKey(), "23760327"));
        Client client = mock(Client.class);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        WebTarget webTarget = mock(WebTarget.class);
        when(client.target("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json")).thenReturn(webTarget);
        Invocation.Builder invBuilder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(invBuilder);
        CmManifestFile manifestFile = new ObjectMapper()
                .readValue(CmUrlProviderTest.class.getResourceAsStream("release_manifest.json"), CmManifestFile.class);
        when(invBuilder.get(CmManifestFile.class)).thenReturn(manifestFile);

        String result = underTest.getCmRpmUrl(image);

        verify(paywallCredentialPopulator).populateWebTarget("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json", webTarget);
        assertEquals("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327p.el7.x86_64.rpm",
                result);
    }

    @Test
    public void testUrlLegacyNonArchive() {
        Image image = mock(Image.class);
        when(image.getOsType()).thenReturn("redhat7");
        when(image.getRepo()).thenReturn(Map.of("redhat7", "https://random.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/"));
        when(image.getPackageVersions()).thenReturn(Map.of(CM.getKey(), "7.6.0", CM_BUILD_NUMBER.getKey(), "23760327"));

        String result = underTest.getCmRpmUrl(image);

        verifyNoInteractions(restClientFactory);
        verifyNoInteractions(paywallCredentialPopulator);
        assertEquals("https://random.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327.el7.x86_64.rpm",
                result);
    }

    @Test
    public void testUrlLegacyArchiveButMissingCmPublic() {
        Image image = mock(Image.class);
        when(image.getOsType()).thenReturn("redhat7");
        when(image.getRepo()).thenReturn(Map.of("redhat7", "https://archive.cloudera.com/p/asdf/7.6.0-23760327/redhat7/yum/"));
        when(image.getPackageVersions()).thenReturn(Map.of(CM.getKey(), "7.6.0", CM_BUILD_NUMBER.getKey(), "23760327"));

        String result = underTest.getCmRpmUrl(image);

        verifyNoInteractions(restClientFactory);
        verifyNoInteractions(paywallCredentialPopulator);
        assertEquals("https://archive.cloudera.com/p/asdf/7.6.0-23760327/redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327.el7.x86_64.rpm",
                result);
    }

    @Test
    public void testLegacyReturnedIfCallFails() {
        Image image = mock(Image.class);
        when(image.getOsType()).thenReturn("redhat7");
        when(image.getRepo()).thenReturn(Map.of("redhat7", "https://archive.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/"));
        when(image.getPackageVersions()).thenReturn(Map.of(CM.getKey(), "7.6.0", CM_BUILD_NUMBER.getKey(), "23760327"));
        Client client = mock(Client.class);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        WebTarget webTarget = mock(WebTarget.class);
        when(client.target("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json")).thenReturn(webTarget);
        Invocation.Builder invBuilder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(invBuilder);
        when(invBuilder.get(CmManifestFile.class)).thenThrow(new RuntimeException("Test Failure"));

        String result = underTest.getCmRpmUrl(image);

        verify(paywallCredentialPopulator).populateWebTarget("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json", webTarget);
        assertEquals("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327.el7.x86_64.rpm",
                result);
    }

    @Test
    public void testLegacyReturnedIfManifestMissingSuitable() throws IOException {
        Image image = mock(Image.class);
        when(image.getOsType()).thenReturn("redhat7");
        when(image.getRepo()).thenReturn(Map.of("redhat7", "https://archive.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/"));
        when(image.getPackageVersions()).thenReturn(Map.of(CM.getKey(), "7.6.0", CM_BUILD_NUMBER.getKey(), "23760327"));
        Client client = mock(Client.class);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        WebTarget webTarget = mock(WebTarget.class);
        when(client.target("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json")).thenReturn(webTarget);
        Invocation.Builder invBuilder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(invBuilder);
        CmManifestFile manifestFile = new ObjectMapper()
                .readValue(CmUrlProviderTest.class.getResourceAsStream("release_manifest.json"), CmManifestFile.class);
        when(invBuilder.get(CmManifestFile.class)).thenReturn(manifestFile);
        manifestFile.getFiles().remove("redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327p.el7.x86_64.rpm");

        String result = underTest.getCmRpmUrl(image);

        verify(paywallCredentialPopulator).populateWebTarget("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json", webTarget);
        assertEquals("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327.el7.x86_64.rpm",
                result);
    }
}
