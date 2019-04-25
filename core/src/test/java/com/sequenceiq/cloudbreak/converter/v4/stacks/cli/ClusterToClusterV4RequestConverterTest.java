package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm.ClusterToClouderaManagerV4RequestConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterToClusterV4RequestConverterTest {

    @InjectMocks
    private ClusterToClusterV4RequestConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private Cluster cluster;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private ClusterToClouderaManagerV4RequestConverter clouderaManagerV4RequestConverter;

    private Blueprint blueprint;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        blueprint = new Blueprint();
        blueprint.setName("CD Stub");

        when(cluster.getBlueprint()).thenReturn(blueprint);
    }

    @Test
    public void testConvertWhenAmbariConversionSuccessfulThenExpectedAmbariV4RequestShouldPlacedIn() {
        when(blueprintService.isAmbariBlueprint(blueprint)).thenReturn(Boolean.TRUE);
        AmbariV4Request expected = mock(AmbariV4Request.class);
        when(conversionService.convert(cluster, AmbariV4Request.class)).thenReturn(expected);

        ClusterV4Request result = underTest.convert(cluster);
        assertEquals(expected, result.getAmbari());
        assertNull(result.getCm());
    }

    @Test
    public void testConvertWhenClouderaManagerConversionSuccessfulThenExpectedClouderaManagerV4RequestShouldPlacedIn() {
        when(blueprintService.isAmbariBlueprint(blueprint)).thenReturn(Boolean.FALSE);
        ClouderaManagerV4Request expected = mock(ClouderaManagerV4Request.class);
        when(clouderaManagerV4RequestConverter.convert(cluster)).thenReturn(expected);

        ClusterV4Request result = underTest.convert(cluster);
        assertEquals(expected, result.getCm());
        assertNull(result.getAmbari());
    }

    @Test
    public void testConvertWhenThereIsNoFileSystemThenCloudStorageIsNull() {
        when(cluster.getFileSystem()).thenReturn(null);

        ClusterV4Request result = underTest.convert(cluster);

        assertNull(result.getCloudStorage());
        verify(conversionService, times(0)).convert(null, CloudStorageV4Request.class);
        verify(conversionService, times(0)).convert(any(FileSystem.class), eq(CloudStorageV4Request.class));
    }

    @Test
    public void testConvertWhenFileSystemNotNullThenExpectedCloudStorageRequestShouldBePlaced() {
        FileSystem fileSystem = new FileSystem();
        CloudStorageV4Request expected = new CloudStorageV4Request();
        when(cluster.getFileSystem()).thenReturn(fileSystem);
        when(conversionService.convert(fileSystem, CloudStorageV4Request.class)).thenReturn(expected);

        ClusterV4Request result = underTest.convert(cluster);

        assertEquals(expected, result.getCloudStorage());
        verify(conversionService, times(1)).convert(fileSystem, CloudStorageV4Request.class);
    }

    @Test
    public void testConvertWhenLdapConfigIsNullThenLdapConfigNameShouldBeNull() {
        when(cluster.getLdapConfig()).thenReturn(null);

        ClusterV4Request result = underTest.convert(cluster);

        assertNull(result.getLdapName());
    }

    @Test
    public void testConvertWhenLdapConfigIsNotNullThenLdapConfigNameShouldBePassedFromConfig() {
        String expected = "ldapName";
        LdapConfig ldapConfig = mock(LdapConfig.class);
        when(cluster.getLdapConfig()).thenReturn(ldapConfig);
        when(ldapConfig.getName()).thenReturn(expected);

        ClusterV4Request result = underTest.convert(cluster);

        assertEquals(expected, result.getLdapName());
    }

    @Test
    public void testConvertNameIsPassedProperly() {
        String expected = "name";
        when(cluster.getName()).thenReturn(expected);

        ClusterV4Request result = underTest.convert(cluster);

        assertEquals(expected, result.getName());
    }

    @Test
    public void testConvertWhenProxyConfigIsNullThenProxyNameShouldBeNull() {
        when(cluster.getProxyConfig()).thenReturn(null);

        ClusterV4Request result = underTest.convert(cluster);

        assertNull(result.getProxyName());
    }

    @Test
    public void testConvertWhenProxyConfigNotNullThenProxyConfigNameShouldBePassed() {
        String expected = "proxy name value";
        ProxyConfig proxyConfig = mock(ProxyConfig.class);
        when(proxyConfig.getName()).thenReturn(expected);
        when(cluster.getProxyConfig()).thenReturn(proxyConfig);

        ClusterV4Request result = underTest.convert(cluster);

        assertEquals(expected, result.getProxyName());
    }

    @Test
    public void testConvertWhenRdsConfigNullThenRdsConfigNamesShouldBeEmpty() {
        when(cluster.getRdsConfigs()).thenReturn(null);

        ClusterV4Request result = underTest.convert(cluster);

        assertTrue(result.getDatabases().isEmpty());
    }

    @Test
    public void testConvertWhenRdsConfigIsNotNullButItsEmptyThenRdsConfigNamesShouldBeEmpty() {
        when(cluster.getRdsConfigs()).thenReturn(Collections.emptySet());

        ClusterV4Request result = underTest.convert(cluster);

        assertTrue(result.getDatabases().isEmpty());
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

        ClusterV4Request result = underTest.convert(cluster);

        assertEquals(1L, result.getDatabases().size());
    }

    @Test
    public void testConvertSettingExecutorTypeToNull() {
        ClusterV4Request result = underTest.convert(cluster);

        assertNotNull(result);
        assertNull(result.getExecutorType());
    }

}