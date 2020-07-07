package com.sequenceiq.cloudbreak.service;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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

@RunWith(MockitoJUnitRunner.class)
public class DefaultClouderaManagerRepoServiceTest {

    private static final String CDH = "CDH";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private StackMatrixService stackMatrixService;

    @InjectMocks
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

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
        when(stackMatrixService.getStackMatrix(null)).thenReturn(stackMatrixV4Response);
    }

    @Test
    public void testDefaultRepoWithCDHWhichDoesNotExists() throws Exception {
        ClouderaManagerRepo repo = defaultClouderaManagerRepoService.getDefault("redhat7", CDH, "2.6", null);
        assertNull(repo);
    }

    @Test
    public void testDefaultRepoWithOSWhichDoesNotExists() throws Exception {
        ClouderaManagerRepo repo = defaultClouderaManagerRepoService.getDefault("ubuntu", CDH, "6.1.0", null);
        assertNull(repo);
    }

    @Test
    public void testDefaultRepoWithStackTypeWhichDoesNotExists() throws Exception {
        ClouderaManagerRepo repo = defaultClouderaManagerRepoService.getDefault("redhat7", "NA", "6.1.0", null);
        assertNull(repo);
    }

    @Test
    public void testDefaultRepoWithCDHWhichExists() throws Exception {
        ClouderaManagerRepo repo = defaultClouderaManagerRepoService.getDefault("redhat7", CDH, "6.1.0", null);
        assertEquals("6.1.0", repo.getVersion());
        assertEquals("http://redhat7-base/6.1.0", repo.getBaseUrl());
        assertEquals("http://redhat7-gpg/6.1.0", repo.getGpgKeyUrl());
    }
}