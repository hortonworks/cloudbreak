package com.sequenceiq.cloudbreak.converter.v2.cli;

import static com.sequenceiq.cloudbreak.api.model.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.model.ResourceStatus.USER_MANAGED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public class ClusterToClusterV2RequestConverterTest {

    @InjectMocks
    private ClusterToClusterV2RequestConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private Cluster cluster;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvertWhenAmbariConversionSuccessfulThenExpectedAmbariV2RequestShouldPlacedIn() {
        AmbariV2Request expected = mock(AmbariV2Request.class);
        when(conversionService.convert(cluster, AmbariV2Request.class)).thenReturn(expected);

        ClusterV2Request result = underTest.convert(cluster);
        assertEquals(expected, result.getAmbari());
    }

    @Test
    public void testConvertSettingExecutorTypeToNull() {
        ClusterV2Request result = underTest.convert(cluster);

        assertNull(result.getExecutorType());
    }

    @Test
    public void testConvertWhenThereIsNoFileSystemThenCloudStorageIsNull() {
        when(cluster.getFileSystem()).thenReturn(null);

        ClusterV2Request result = underTest.convert(cluster);

        assertNull(result.getCloudStorage());
        verify(conversionService, times(0)).convert(null, CloudStorageRequest.class);
        verify(conversionService, times(0)).convert(any(FileSystem.class), eq(CloudStorageRequest.class));
    }

    @Test
    public void testConvertWhenFileSystemNotNullThenExpectedCloudStorageRequestShouldBePlaced() {
        FileSystem fileSystem = new FileSystem();
        CloudStorageRequest expected = new CloudStorageRequest();
        when(cluster.getFileSystem()).thenReturn(fileSystem);
        when(conversionService.convert(fileSystem, CloudStorageRequest.class)).thenReturn(expected);

        ClusterV2Request result = underTest.convert(cluster);

        assertEquals(expected, result.getCloudStorage());
        verify(conversionService, times(1)).convert(fileSystem, CloudStorageRequest.class);
    }

    @Test
    public void testConvertWhenLdapConfigIsNullThenLdapConfigNameShouldBeNull() {
        when(cluster.getLdapConfig()).thenReturn(null);

        ClusterV2Request result = underTest.convert(cluster);

        assertNull(result.getLdapConfigName());
    }

    @Test
    public void testConvertWhenLdapConfigIsNotNullThenLdapConfigNameShouldBePassedFromConfig() {
        String expected = "ldapName";
        LdapConfig ldapConfig = mock(LdapConfig.class);
        when(cluster.getLdapConfig()).thenReturn(ldapConfig);
        when(ldapConfig.getName()).thenReturn(expected);

        ClusterV2Request result = underTest.convert(cluster);

        assertEquals(expected, result.getLdapConfigName());
    }

    @Test
    public void testConvertNameIsPassedProperly() {
        String expected = "name";
        when(cluster.getName()).thenReturn(expected);

        ClusterV2Request result = underTest.convert(cluster);

        assertEquals(expected, result.getName());
    }

    @Test
    public void testConvertWhenProxyConfigIsNullThenProxyNameShouldBeNull() {
        when(cluster.getProxyConfig()).thenReturn(null);

        ClusterV2Request result = underTest.convert(cluster);

        assertNull(result.getProxyName());
    }

    @Test
    public void testConvertWhenProxyConfigNotNullThenProxyConfigNameShouldBePassed() {
        String expected = "proxy name value";
        ProxyConfig proxyConfig = mock(ProxyConfig.class);
        when(proxyConfig.getName()).thenReturn(expected);
        when(cluster.getProxyConfig()).thenReturn(proxyConfig);

        ClusterV2Request result = underTest.convert(cluster);

        assertEquals(expected, result.getProxyName());
    }

    @Test
    public void testConvertWhenRdsConfigNullThenRdsConfigNamesShouldBeEmpty() {
        when(cluster.getRdsConfigs()).thenReturn(null);

        ClusterV2Request result = underTest.convert(cluster);

        assertTrue(result.getRdsConfigNames().isEmpty());
    }

    @Test
    public void testConvertWhenRdsConfigIsNotNullButItsEmptyThenRdsConfigNamesShouldBeEmpty() {
        when(cluster.getRdsConfigs()).thenReturn(Collections.emptySet());

        ClusterV2Request result = underTest.convert(cluster);

        assertTrue(result.getRdsConfigNames().isEmpty());
    }

    @Test
    public void testConvertWhenRdsConfigsContainsElementsThenUserManagedOnesNameShouldBeStored() {
        RDSConfig notUserManaged = new RDSConfig();
        notUserManaged.setId(0L);
        notUserManaged.setStatus(DEFAULT);

        RDSConfig userManaged = new RDSConfig();
        notUserManaged.setId(1L);
        userManaged.setStatus(USER_MANAGED);

        Set<RDSConfig> rdsConfigs = new LinkedHashSet<>(2);
        rdsConfigs.add(notUserManaged);
        rdsConfigs.add(userManaged);

        when(cluster.getRdsConfigs()).thenReturn(rdsConfigs);

        ClusterV2Request result = underTest.convert(cluster);

        assertEquals(1L, result.getRdsConfigNames().size());
    }

}