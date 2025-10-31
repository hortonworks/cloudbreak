package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm.ClusterToClouderaManagerV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;

@RunWith(MockitoJUnitRunner.class)
public class ClusterToClusterV4RequestConverterTest {

    @InjectMocks
    private ClusterToClusterV4RequestConverter underTest;

    @Mock
    private Cluster cluster;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private ClusterToClouderaManagerV4RequestConverter clouderaManagerV4RequestConverter;

    @Mock
    private CloudStorageConverter cloudStorageConverter;

    private Blueprint blueprint;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        blueprint = new Blueprint();
        blueprint.setName("CD Stub");

        when(cluster.getBlueprint()).thenReturn(blueprint);
    }

    @Test
    public void testConvertWhenClouderaManagerConversionSuccessfulThenExpectedClouderaManagerV4RequestShouldPlacedIn() {
        ClouderaManagerV4Request expected = mock(ClouderaManagerV4Request.class);
        when(clouderaManagerV4RequestConverter.convert(cluster)).thenReturn(expected);

        ClusterV4Request result = underTest.convert(cluster);
        assertEquals(expected, result.getCm());
    }

    @Test
    public void testConvertWhenThereIsNoFileSystemThenCloudStorageIsNull() {
        when(cluster.getFileSystem()).thenReturn(null);

        ClusterV4Request result = underTest.convert(cluster);

        assertNull(result.getCloudStorage());
    }

    @Test
    public void testConvertWhenFileSystemNotNullThenExpectedCloudStorageRequestShouldBePlaced() {
        FileSystem fileSystem = new FileSystem();
        CloudStorageRequest expected = new CloudStorageRequest();
        when(cluster.getFileSystem()).thenReturn(fileSystem);
        when(cloudStorageConverter.fileSystemToRequest(fileSystem)).thenReturn(expected);

        ClusterV4Request result = underTest.convert(cluster);

        assertEquals(expected, result.getCloudStorage());
        verify(cloudStorageConverter, times(1)).fileSystemToRequest(fileSystem);
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
        when(cluster.getProxyConfigCrn()).thenReturn(null);

        ClusterV4Request result = underTest.convert(cluster);

        assertNull(result.getProxyConfigCrn());
    }

    @Test
    public void testConvertWhenProxyConfigNotNullThenProxyConfigNameShouldBePassed() {
        String expected = "proxy name value";
        when(cluster.getProxyConfigCrn()).thenReturn(expected);

        ClusterV4Request result = underTest.convert(cluster);

        assertEquals(expected, result.getProxyConfigCrn());
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
    public void testConvertWhenEncryptionProfileIsNotNull() {
        String expected = "epName";

        when(cluster.getEncryptionProfileName()).thenReturn(expected);

        ClusterV4Request result = underTest.convert(cluster);

        assertEquals(expected, result.getEncryptionProfileName());
    }
}