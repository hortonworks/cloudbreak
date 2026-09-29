package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class CmUrlProviderTest {

    @Mock
    private RestClientFactory restClientFactory;

    @Mock
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @InjectMocks
    private CmUrlProvider underTest;

    @ParameterizedTest
    @EnumSource(OsType.class)
    public void testUrlFromManifest(OsType osType) throws IOException {
        String osTypeString = osType.getOsType();
        ClusterUpgradeProperties properties = createProperties(osType, null,
                "https://archive.cloudera.com/p/cm-public/7.6.0-23760327/" + osTypeString + "/yum/");
        Client client = mock(Client.class);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        WebTarget webTarget = mock(WebTarget.class);
        when(client.target("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json")).thenReturn(webTarget);
        Invocation.Builder invBuilder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(invBuilder);
        CmManifestFile manifestFile = new ObjectMapper()
                .readValue(CmUrlProviderTest.class.getResourceAsStream("release_manifest.json"), CmManifestFile.class);
        when(invBuilder.get(CmManifestFile.class)).thenReturn(manifestFile);

        String result = underTest.getCmRpmUrl(properties);

        verify(paywallCredentialPopulator).populateWebTarget("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json", webTarget);
        assertEquals("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/"
                        + osTypeString
                        + "/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327p."
                        + osType.getParcelPostfix()
                        + ".x86_64.rpm",
                result);
    }

    @Test
    public void testUrlLegacyNonArchive() {
        ClusterUpgradeProperties properties = createProperties(OsType.CENTOS7, Architecture.X86_64.getName(),
                "https://random.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/");

        String result = underTest.getCmRpmUrl(properties);

        verifyNoInteractions(restClientFactory);
        verifyNoInteractions(paywallCredentialPopulator);
        assertEquals("https://random.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327.el7.x86_64.rpm",
                result);
    }

    @Test
    public void testUrlLegacyNonArchiveArm64() {
        ClusterUpgradeProperties properties = createProperties(OsType.RHEL8, Architecture.ARM64.getName(),
                "https://random.cloudera.com/p/cm-public/7.6.0-23760327/redhat8/yum/");

        String result = underTest.getCmRpmUrl(properties);

        verifyNoInteractions(restClientFactory);
        verifyNoInteractions(paywallCredentialPopulator);
        assertEquals("https://random.cloudera.com/p/cm-public/7.6.0-23760327/redhat8/yum/RPMS/aarch64/cloudera-manager-server-7.6.0-23760327.el8.aarch64.rpm",
                result);
    }

    @Test
    public void testUrlLegacyNullArchitectureFallsBackToX86() {
        ClusterUpgradeProperties properties = createProperties(OsType.CENTOS7, null,
                "https://random.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/");

        String result = underTest.getCmRpmUrl(properties);

        verifyNoInteractions(restClientFactory);
        verifyNoInteractions(paywallCredentialPopulator);
        assertEquals("https://random.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327.el7.x86_64.rpm",
                result);
    }

    @Test
    public void testUrlLegacyArchiveButMissingCmPublic() {
        ClusterUpgradeProperties properties = createProperties(OsType.CENTOS7, Architecture.X86_64.getName(),
                "https://archive.cloudera.com/p/asdf/7.6.0-23760327/redhat7/yum/");

        String result = underTest.getCmRpmUrl(properties);

        verifyNoInteractions(restClientFactory);
        verifyNoInteractions(paywallCredentialPopulator);
        assertEquals("https://archive.cloudera.com/p/asdf/7.6.0-23760327/redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327.el7.x86_64.rpm",
                result);
    }

    @Test
    public void testLegacyReturnedIfCallFails() {
        ClusterUpgradeProperties properties = createProperties(OsType.CENTOS7, Architecture.X86_64.getName(),
                "https://archive.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/");
        Client client = mock(Client.class);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        WebTarget webTarget = mock(WebTarget.class);
        when(client.target("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json")).thenReturn(webTarget);
        Invocation.Builder invBuilder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(invBuilder);
        when(invBuilder.get(CmManifestFile.class)).thenThrow(new RuntimeException("Test Failure"));

        String result = underTest.getCmRpmUrl(properties);

        verify(paywallCredentialPopulator).populateWebTarget("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json", webTarget);
        assertEquals("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327.el7.x86_64.rpm",
                result);
    }

    @Test
    public void testLegacyReturnedIfManifestMissingSuitable() throws IOException {
        ClusterUpgradeProperties properties = createProperties(OsType.CENTOS7, Architecture.X86_64.getName(),
                "https://archive.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/");
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

        String result = underTest.getCmRpmUrl(properties);

        verify(paywallCredentialPopulator).populateWebTarget("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/release_manifest.json", webTarget);
        assertEquals("https://archive.cloudera.com/p/cm-public/7.6.0-23760327/redhat7/yum/RPMS/x86_64/cloudera-manager-server-7.6.0-23760327.el7.x86_64.rpm",
                result);
    }

    @Test
    void testCacheReusesUrlWhenOnlyUnrelatedUpgradePropertiesDiffer() {
        CmUrlProvider target = spy(underTest);
        CmUrlProvider cachedProvider = withCache(target);
        ClusterUpgradeProperties first = createProperties(OsType.RHEL8, Architecture.X86_64.getName(), "https://repo.example.com/yum/");
        ClusterUpgradeProperties second = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.3", "different-image-version", OsType.RHEL8, Architecture.X86_64.getName(), null, Set.of(),
                createProperties(OsType.RHEL8, Architecture.X86_64.getName(), "https://repo.example.com/yum/").getClouderaManagerRepo());

        assertEquals(cachedProvider.getCmRpmUrl(first), cachedProvider.getCmRpmUrl(second));

        verify(target, times(1)).getCmRpmUrl(any());
    }

    @ParameterizedTest
    @CsvSource({
            "https://other.example.com/yum/, 7.6.0, 23760327, RHEL8, x86_64",
            "https://repo.example.com/yum/, 7.7.0, 23760327, RHEL8, x86_64",
            "https://repo.example.com/yum/, 7.6.0, 23760328, RHEL8, x86_64",
            "https://repo.example.com/yum/, 7.6.0, 23760327, CENTOS7, x86_64",
            "https://repo.example.com/yum/, 7.6.0, 23760327, RHEL8, arm64"
    })
    void testCacheSeparatesDifferentCmPackagesForSameImage(String baseUrl, String version, String buildNumber, OsType osType, String architecture) {
        CmUrlProvider target = spy(underTest);
        CmUrlProvider cachedProvider = withCache(target);
        ClusterUpgradeProperties first = createProperties(OsType.RHEL8, Architecture.X86_64.getName(), "https://repo.example.com/yum/");
        ClusterUpgradeProperties second = createProperties(osType, architecture, baseUrl);
        second.getClouderaManagerRepo().setVersion(version);
        second.getClouderaManagerRepo().setBuildNumber(buildNumber);

        assertNotEquals(cachedProvider.getCmRpmUrl(first), cachedProvider.getCmRpmUrl(second));

        verify(target, times(2)).getCmRpmUrl(any());
    }

    private CmUrlProvider withCache(CmUrlProvider target) {
        CacheInterceptor interceptor = new CacheInterceptor();
        interceptor.setCacheManager(new ConcurrentMapCacheManager(CmUrlCache.CM_URL_CACHE));
        interceptor.setCacheOperationSources(new AnnotationCacheOperationSource());
        interceptor.afterPropertiesSet();
        interceptor.afterSingletonsInstantiated();
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(interceptor);
        return (CmUrlProvider) factory.getProxy();
    }

    private ClusterUpgradeProperties createProperties(OsType osType, String architecture, String baseUrl) {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setBaseUrl(baseUrl);
        cmRepo.setVersion("7.6.0");
        cmRepo.setBuildNumber("23760327");
        return ClusterUpgradePropertiesTestUtils.withTargetProducts("7.2.18", "base-image", osType, architecture, null, Set.of(), cmRepo);
    }
}