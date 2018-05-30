package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigurationsViewProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;

public class StackToCloudStackConverterTest {

    @InjectMocks
    private StackToCloudStackConverter underTest;

    @Mock
    private SecurityRuleRepository securityRuleRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private ComponentConfigProvider componentConfigProvider;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    @Mock
    private ConversionService conversionService;

    @Mock
    private Stack stack;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private Cluster cluster;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stack.getCluster()).thenReturn(cluster);
    }

    @Test
    public void testConvertWhenThereIsNoFileSystemInClusterThenCloudFileSystemShouldBeNull() {
        when(cluster.getFileSystem()).thenReturn(null);

        CloudStack result = underTest.convert(stack);

        assertFalse(result.getFileSystem().isPresent());
        verify(conversionService, times(0)).convert(any(FileSystem.class), eq(SpiFileSystem.class));
    }

    @Test

    public void testConvertWhenThereIsAFileSystemInClusterThenExpectedSpiFileSystemShouldPlacedInCloudStack() {
        FileSystem fileSystem = new FileSystem();
        SpiFileSystem expected = mock(SpiFileSystem.class);
        when(cluster.getFileSystem()).thenReturn(fileSystem);
        when(conversionService.convert(fileSystem, SpiFileSystem.class)).thenReturn(expected);

        CloudStack result = underTest.convert(stack);

        assertTrue(result.getFileSystem().isPresent());
        assertEquals(expected, result.getFileSystem().get());
        verify(conversionService, times(1)).convert(fileSystem, SpiFileSystem.class);
    }

}