package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getCMStackDescriptorResponse;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;

@RunWith(MockitoJUnitRunner.class)
public class StackMatrixServiceTest {

    @Mock
    private DefaultCDHEntries defaultCDHEntries;

    @Mock
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Mock
    private ConverterUtil converterUtil;

    @InjectMocks
    private StackMatrixService stackMatrixService;

    @Test
    public void getStackMatrixWithoutAmbari() {
        setupStackEntries();

        when(converterUtil.convert(any(RepositoryInfo.class), eq(ClouderaManagerInfoV4Response.class))).thenReturn(new ClouderaManagerInfoV4Response());

        StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix();

        assertEquals(1L, stackMatrixV4Response.getCdh().size());

        assertEquals("6.1", stackMatrixV4Response.getCdh().get("6.1.0").getMinCM());
        assertEquals("6.1.0-1.cdh6.1.0.p0.770702", stackMatrixV4Response.getCdh().get("6.1.0").getVersion());
        assertNull(stackMatrixV4Response.getCdh().get("6.1.0").getClouderaManager().getRepository());
        assertNull(stackMatrixV4Response.getCdh().get("6.1.0").getClouderaManager().getVersion());

    }

    private void setupStackEntries() {

        Map<String, DefaultCDHInfo> cdhEntries = new HashMap<>();

        DefaultCDHInfo cdhInfo = getDefaultCDHInfo("6.1", "6.1.0-1.cdh6.1.0.p0.770702");
        cdhEntries.put("6.1.0", cdhInfo);
        when(converterUtil.convert(cdhInfo, ClouderaManagerStackDescriptorV4Response.class))
                .thenReturn(getCMStackDescriptorResponse("6.1", "6.1.0-1.cdh6.1.0.p0.770702"));

        when(defaultCDHEntries.getEntries()).thenReturn(cdhEntries);
    }
}