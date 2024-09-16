package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getCMStackDescriptorResponse;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.RepositoryInfoToClouderaManagerInfoV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.StackInfoToClouderaManagerStackDescriptorV4ResponseConverter;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
public class StackMatrixServiceTest {

    private static final Long WORKSPACE_ID = 0L;

    private static final String PLATFORM = CloudPlatform.AWS.name();

    private static final ImageCatalogPlatform IMAGECATALOGPLATFORM = imageCatalogPlatform(PLATFORM);

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

    private static final String OS = "centos7";

    private static final String CLUSTER_VERSION = "6.1.0";

    @Mock
    private ImageBasedDefaultCDHEntries imageBasedDefaultCDHEntries;

    @Mock
    private RepositoryInfoToClouderaManagerInfoV4ResponseConverter repositoryInfoToClouderaManagerInfoV4ResponseConverter;

    @Mock
    private StackInfoToClouderaManagerStackDescriptorV4ResponseConverter stackInfoToClouderaManagerStackDescriptorV4ResponseConverter;

    @InjectMocks
    private StackMatrixService stackMatrixService;

    @Test
    void getStackMatrixWithoutAmbari() throws Exception {
        setupStackEntries();

        when(repositoryInfoToClouderaManagerInfoV4ResponseConverter.convert(any(RepositoryInfo.class))).thenReturn(new ClouderaManagerInfoV4Response());

        StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix(WORKSPACE_ID, IMAGECATALOGPLATFORM, OS, IMAGE_CATALOG_NAME);

        assertEquals(1L, stackMatrixV4Response.getCdh().size());

        assertEquals("6.1.0-1.cdh6.1.0.p0.770702", stackMatrixV4Response.getCdh().get(CLUSTER_VERSION).getVersion());
        assertNull(stackMatrixV4Response.getCdh().get(CLUSTER_VERSION).getClouderaManager().getRepository());
        assertNull(stackMatrixV4Response.getCdh().get(CLUSTER_VERSION).getClouderaManager().getVersion());
    }

    @Test
    void getSupportedOperatingSystemsWithSupportedOs() throws Exception {
        setupStackEntries();
        when(repositoryInfoToClouderaManagerInfoV4ResponseConverter.convert(any(RepositoryInfo.class))).thenReturn(new ClouderaManagerInfoV4Response());

        Set<String> result = stackMatrixService.getSupportedOperatingSystems(WORKSPACE_ID, CLUSTER_VERSION, IMAGECATALOGPLATFORM, OS, IMAGE_CATALOG_NAME);

        assertThat(result).containsOnly(OS);
    }

    @Test
    void getSupportedOperatingSystemsWithUnsupportedOs() throws Exception {
        String os = "redhat8";
        when(imageBasedDefaultCDHEntries.getEntries(WORKSPACE_ID, IMAGECATALOGPLATFORM, os, IMAGE_CATALOG_NAME)).thenReturn(Map.of());

        Set<String> result = stackMatrixService.getSupportedOperatingSystems(WORKSPACE_ID, CLUSTER_VERSION, IMAGECATALOGPLATFORM, os, IMAGE_CATALOG_NAME);

        assertThat(result).isEmpty();
    }

    private void setupStackEntries() throws Exception {
        Map<String, ImageBasedDefaultCDHInfo> cdhEntries = new HashMap<>();

        DefaultCDHInfo cdhInfo = getDefaultCDHInfo("6.1.0-1.cdh6.1.0.p0.770702");
        Image image = mock(Image.class);
        when(image.getOs()).thenReturn(OS);
        cdhEntries.put(CLUSTER_VERSION, new ImageBasedDefaultCDHInfo(cdhInfo, image));
        when(stackInfoToClouderaManagerStackDescriptorV4ResponseConverter.convert(cdhInfo))
                .thenReturn(getCMStackDescriptorResponse("6.1.0-1.cdh6.1.0.p0.770702"));

        when(imageBasedDefaultCDHEntries.getEntries(WORKSPACE_ID, IMAGECATALOGPLATFORM, OS, IMAGE_CATALOG_NAME)).thenReturn(cdhEntries);
    }
}