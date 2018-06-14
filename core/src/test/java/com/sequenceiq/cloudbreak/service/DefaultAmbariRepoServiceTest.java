package com.sequenceiq.cloudbreak.service;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.AmbariInfoJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackDescriptor;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAmbariRepoServiceTest {

    @InjectMocks
    private DefaultAmbariRepoService defaultAmbariRepoService;

    @Mock
    private StackMatrixService stackMatrixService;

    @Before
    public void init() {
        StackMatrix stackMatrix = new StackMatrix();
        Map<String, StackDescriptor> hdpMap = new HashMap<>();
        Map<String, StackDescriptor> hdfMap = new HashMap<>();

        AmbariInfoJson ambariInfoJson26 = new AmbariInfoJson();
        ambariInfoJson26.setVersion("2.6");
        ambariInfoJson26.setRepo(getAmbariRepo("2.6"));

        AmbariInfoJson ambariInfoJson27 = new AmbariInfoJson();
        ambariInfoJson27.setVersion("2.7");
        ambariInfoJson27.setRepo(getAmbariRepo("2.7"));

        StackDescriptor hdpDescriptor26 = new StackDescriptor();
        hdpDescriptor26.setAmbari(ambariInfoJson26);
        hdpMap.put("2.7", hdpDescriptor26);

        StackDescriptor hdpDescriptor30 = new StackDescriptor();
        hdpDescriptor30.setAmbari(ambariInfoJson27);
        hdpMap.put("3.0", hdpDescriptor30);

        stackMatrix.setHdp(hdpMap);

        StackDescriptor hdfDescriptor31 = new StackDescriptor();
        hdfDescriptor31.setAmbari(ambariInfoJson27);
        hdfMap.put("3.1", hdfDescriptor31);

        stackMatrix.setHdp(hdpMap);
        stackMatrix.setHdf(hdfMap);

        when(stackMatrixService.getStackMatrix()).thenReturn(stackMatrix);
    }

    private Map<String, AmbariRepoDetailsJson> getAmbariRepo(String version) {
        Map<String, AmbariRepoDetailsJson> ambariRepo = new HashMap<>();

        AmbariRepoDetailsJson redhat6RepoDetails = new AmbariRepoDetailsJson();
        redhat6RepoDetails.setBaseUrl("http://redhat6-base/" + version);
        redhat6RepoDetails.setGpgKeyUrl("http://redhat6-gpg/" + version);
        ambariRepo.put("redhat6", redhat6RepoDetails);

        AmbariRepoDetailsJson redhat7RepoDetails = new AmbariRepoDetailsJson();
        redhat7RepoDetails.setBaseUrl("http://redhat7-base/" + version);
        redhat7RepoDetails.setGpgKeyUrl("http://redhat7-gpg/" + version);
        ambariRepo.put("redhat7", redhat7RepoDetails);

        return ambariRepo;
    }

    @Test
    public void testDefaultAmbariRepoWithoutClusterTypeAndVersion() {
        AmbariRepo ambariRepo = defaultAmbariRepoService.getDefault("redhat7");
        assertEquals("2.7", ambariRepo.getVersion());
    }

    @Test
    public void testDefaultAmbariRepoWithHDPWhichDoesNotExists() {
        AmbariRepo ambariRepo = defaultAmbariRepoService.getDefault("redhat7", "HDP", "2.6");
        assertNull(ambariRepo);
    }

    @Test
    public void testDefaultAmbariRepoWithHDPWhichExists() {
        AmbariRepo ambariRepo = defaultAmbariRepoService.getDefault("redhat6", "HDP", "2.7");
        assertEquals("2.6", ambariRepo.getVersion());
        assertEquals("http://redhat6-base/2.6", ambariRepo.getBaseUrl());
        assertEquals("http://redhat6-gpg/2.6", ambariRepo.getGpgKeyUrl());

        ambariRepo = defaultAmbariRepoService.getDefault("redhat7", "HDP", "3.0");
        assertEquals("2.7", ambariRepo.getVersion());
        assertEquals("http://redhat7-base/2.7", ambariRepo.getBaseUrl());
        assertEquals("http://redhat7-gpg/2.7", ambariRepo.getGpgKeyUrl());
    }
}