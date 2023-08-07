package com.sequenceiq.cloudbreak.service;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

@RunWith(MockitoJUnitRunner.class)
public class DefaultClouderaManagerRepoServiceTest {

    private static final String CDH = "CDH";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private StackMatrixService stackMatrixService;

    @Mock
    private Image image;

    @Mock
    private StackTypeResolver stackTypeResolver;

    @Mock
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    @InjectMocks
    private DefaultClouderaManagerRepoService underTest;

    @Before
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
        when(stackMatrixService.getStackMatrix(any(), any())).thenReturn(stackMatrixV4Response);
    }

    @Test
    public void testDefaultRepoWithCDHWhichDoesNotExists() throws Exception {
        ClouderaManagerRepo repo = underTest.getDefault("redhat7", "centos7", CDH, "2.6", null);
        assertNull(repo);
    }

    @Test
    public void testDefaultRepoWithOSWhichDoesNotExists() throws Exception {
        ClouderaManagerRepo repo = underTest.getDefault("ubuntu", "ubuntu", CDH, "6.1.0", null);
        assertNull(repo);
    }

    @Test
    public void testDefaultRepoWithStackTypeWhichDoesNotExists() throws Exception {
        ClouderaManagerRepo repo = underTest.getDefault("redhat7", "centos7", "NA", "6.1.0", null);
        assertNull(repo);
    }

    @Test
    public void testDefaultRepoWithCDHWhichExists() throws Exception {
        ClouderaManagerRepo repo = underTest.getDefault("redhat7", "centos7", CDH, "6.1.0", null);
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

        Assertions.assertEquals("Unsupported stack type: 'CDX'.", exception.getMessage());
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

        Assertions.assertEquals("Cloudera Manager repo was not found in image for os: 'redhat8'.", exception.getMessage());
    }
}