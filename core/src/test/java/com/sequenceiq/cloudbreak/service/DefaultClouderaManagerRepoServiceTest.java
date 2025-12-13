package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.RepoTestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
public class DefaultClouderaManagerRepoServiceTest {

    private static final Architecture ARCH = Architecture.X86_64;

    @Mock
    private StackMatrixService stackMatrixService;

    @Mock
    private Image image;

    @Mock
    private com.sequenceiq.cloudbreak.cloud.model.Image imageModel;

    @Mock
    private StackTypeResolver stackTypeResolver;

    @Mock
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    @InjectMocks
    private DefaultClouderaManagerRepoService underTest;

    @BeforeEach
    public void init() throws Exception {
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();
        Map<String, ClouderaManagerStackDescriptorV4Response> cdhMap = new HashMap<>();

        ClouderaManagerInfoV4Response clouderaManagerInfoJson610 = new ClouderaManagerInfoV4Response();
        clouderaManagerInfoJson610.setVersion("6.1.0");
        clouderaManagerInfoJson610.setRepository(RepoTestUtil.getClouderaManagerRepositoryResponse("6.1.0"));

        ClouderaManagerInfoV4Response clouderaManagerInfoJson620 = new ClouderaManagerInfoV4Response();
        clouderaManagerInfoJson620.setVersion("6.2.0");
        clouderaManagerInfoJson620.setRepository(RepoTestUtil.getClouderaManagerRepositoryResponse("6.2.0"));

        ClouderaManagerStackDescriptorV4Response cdhDescriptor610 = new ClouderaManagerStackDescriptorV4Response();
        cdhDescriptor610.setClouderaManager(clouderaManagerInfoJson610);
        cdhMap.put("6.1.0", cdhDescriptor610);

        ClouderaManagerStackDescriptorV4Response cdhDescriptor620 = new ClouderaManagerStackDescriptorV4Response();
        cdhDescriptor620.setClouderaManager(clouderaManagerInfoJson620);
        cdhMap.put("6.2.0", cdhDescriptor620);

        stackMatrixV4Response.setCdh(cdhMap);
        lenient().when(stackMatrixService.getStackMatrix(any(), any(), any(), any(), any())).thenReturn(stackMatrixV4Response);

        lenient().when(imageModel.getOs()).thenReturn("centos7");
        lenient().when(imageModel.getOsType()).thenReturn("redhat7");
    }

    @Test
    public void testDefaultRepoWithCDHWhichDoesNotExists() throws Exception {
        ClouderaManagerRepo repo = underTest.getDefault(0L, "2.6", null, imageModel);
        assertNull(repo);
    }

    @Test
    public void testDefaultRepoWithOSWhichDoesNotExists() throws Exception {
        when(imageModel.getOs()).thenReturn("ubuntu");
        when(imageModel.getOsType()).thenReturn("ubuntu");
        ClouderaManagerRepo repo = underTest.getDefault(0L, "6.1.0", null, imageModel);
        assertNull(repo);
    }

    @Test
    public void testDefaultRepoWithCDHWhichExists() throws Exception {
        ClouderaManagerRepo repo = underTest.getDefault(0L, "6.1.0", null, imageModel);
        assertEquals("6.1.0", repo.getVersion());
        assertEquals("http://redhat7-base/6.1.0", repo.getBaseUrl());
        assertEquals("http://redhat7-gpg/6.1.0", repo.getGpgKeyUrl());
    }

    @Test
    public void testGetClouderaManagerRepoWithEmptyImage() throws CloudbreakImageCatalogException {
        Optional<ClouderaManagerRepo> repo = underTest.getClouderaManagerRepo(image);
        assertTrue(repo.isEmpty());
    }

    @Test
    public void testUnsupportedStackType() throws CloudbreakImageCatalogException {
        StackRepoDetails stackRepoDetails = new StackRepoDetails(Map.of(StackRepoDetails.REPO_ID_TAG, "CDX"), null);
        ImageStackDetails imageStackDetails = new ImageStackDetails("7.2.17", stackRepoDetails, "123");
        when(image.getStackDetails()).thenReturn(imageStackDetails);
        when(stackTypeResolver.determineStackType(any())).thenThrow(new CloudbreakImageCatalogException("Unsupported stack type: 'CDX'."));

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class,
                () -> underTest.getClouderaManagerRepo(image));

        assertEquals("Unsupported stack type: 'CDX'.", exception.getMessage());
    }

    @Test
    public void testNoClouderaManagerImage() throws CloudbreakImageCatalogException {
        StackRepoDetails stackRepoDetails = new StackRepoDetails(Map.of(StackRepoDetails.REPO_ID_TAG, "CDH"), null);
        ImageStackDetails imageStackDetails = new ImageStackDetails("7.2.17", stackRepoDetails, "123");
        when(image.getStackDetails()).thenReturn(imageStackDetails);
        when(image.getOsType()).thenReturn("redhat8");
        when(stackTypeResolver.determineStackType(any())).thenReturn(StackType.CDH);

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class,
                () -> underTest.getClouderaManagerRepo(image));

        assertEquals("Cloudera Manager repo was not found in image for os: 'redhat8'.", exception.getMessage());
    }

    @Test
    public void testEmptyClouderaManagerImage() throws CloudbreakImageCatalogException {
        StackRepoDetails stackRepoDetails = new StackRepoDetails(Map.of(StackRepoDetails.REPO_ID_TAG, "CDH"), null);
        ImageStackDetails imageStackDetails = new ImageStackDetails("7.2.17", stackRepoDetails, "123");
        when(image.getStackDetails()).thenReturn(imageStackDetails);
        when(image.getOsType()).thenReturn("redhat8");
        when(stackTypeResolver.determineStackType(any())).thenReturn(StackType.CDH);
        ClouderaManagerRepo repo = new ClouderaManagerRepo();
        repo.setBaseUrl("");
        when(imageToClouderaManagerRepoConverter.convert(image)).thenReturn(repo);

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class,
                () -> underTest.getClouderaManagerRepo(image));

        assertEquals("Cloudera Manager repo was not found in image for os: 'redhat8'.", exception.getMessage());
    }
}