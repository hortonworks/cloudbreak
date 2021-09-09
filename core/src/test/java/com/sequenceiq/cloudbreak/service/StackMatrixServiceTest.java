package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getCMStackDescriptorResponse;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

@RunWith(MockitoJUnitRunner.class)
public class StackMatrixServiceTest {

    private static final Long WORKSPACE_ID = 0L;

    private static final String PLATFORM = CloudPlatform.AWS.name();

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

    @Mock
    private ImageBasedDefaultCDHEntries imageBasedDefaultCDHEntries;

    @Mock
    private RepositoryInfoToClouderaManagerInfoV4ResponseConverter repositoryInfoToClouderaManagerInfoV4ResponseConverter;

    @Mock
    private StackInfoToClouderaManagerStackDescriptorV4ResponseConverter stackInfoToClouderaManagerStackDescriptorV4ResponseConverter;

    @InjectMocks
    private StackMatrixService stackMatrixService;

    @Test
    public void getStackMatrixWithoutAmbari() throws Exception {
        setupStackEntries();

        when(repositoryInfoToClouderaManagerInfoV4ResponseConverter.convert(any(RepositoryInfo.class))).thenReturn(new ClouderaManagerInfoV4Response());

        StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix(WORKSPACE_ID, PLATFORM, IMAGE_CATALOG_NAME);

        assertEquals(1L, stackMatrixV4Response.getCdh().size());

        assertEquals("6.1.0-1.cdh6.1.0.p0.770702", stackMatrixV4Response.getCdh().get("6.1.0").getVersion());
        assertNull(stackMatrixV4Response.getCdh().get("6.1.0").getClouderaManager().getRepository());
        assertNull(stackMatrixV4Response.getCdh().get("6.1.0").getClouderaManager().getVersion());
    }

    private void setupStackEntries() throws Exception {
        Map<String, ImageBasedDefaultCDHInfo> cdhEntries = new HashMap<>();

        DefaultCDHInfo cdhInfo = getDefaultCDHInfo("6.1.0-1.cdh6.1.0.p0.770702");
        cdhEntries.put("6.1.0", new ImageBasedDefaultCDHInfo(cdhInfo, mock(Image.class)));
        when(stackInfoToClouderaManagerStackDescriptorV4ResponseConverter.convert(cdhInfo))
                .thenReturn(getCMStackDescriptorResponse("6.1.0-1.cdh6.1.0.p0.770702"));

        when(imageBasedDefaultCDHEntries.getEntries(WORKSPACE_ID, PLATFORM, IMAGE_CATALOG_NAME)).thenReturn(cdhEntries);
    }
}