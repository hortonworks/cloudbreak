package com.sequenceiq.cloudbreak.service;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackDescriptorV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariRepoDetails;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAmbariRepoServiceTest {

    @InjectMocks
    private DefaultAmbariRepoService defaultAmbariRepoService;

    @Mock
    private StackMatrixService stackMatrixService;

    @Before
    public void init() {
        Map<String, AmbariInfo> entries = new HashMap<>();

        StackMatrixV4 stackMatrixV4 = new StackMatrixV4();
        Map<String, StackDescriptorV4> hdpMap = new HashMap<>();
        Map<String, StackDescriptorV4> hdfMap = new HashMap<>();

        AmbariInfoJson ambariInfoJson26 = new AmbariInfoJson();
        ambariInfoJson26.setVersion("2.6");
        ambariInfoJson26.setRepo(getAmbariRepoJson("2.6"));

        AmbariInfoJson ambariInfoJson27 = new AmbariInfoJson();
        ambariInfoJson27.setVersion("2.7");
        ambariInfoJson27.setRepo(getAmbariRepoJson("2.7"));

        StackDescriptorV4 hdpDescriptor26 = new StackDescriptorV4();
        hdpDescriptor26.setAmbari(ambariInfoJson26);
        hdpMap.put("2.7", hdpDescriptor26);

        StackDescriptorV4 hdpDescriptor30 = new StackDescriptorV4();
        hdpDescriptor30.setAmbari(ambariInfoJson27);
        hdpMap.put("3.0", hdpDescriptor30);

        stackMatrixV4.setHdp(hdpMap);

        StackDescriptorV4 hdfDescriptor31 = new StackDescriptorV4();
        hdfDescriptor31.setAmbari(ambariInfoJson27);
        hdfMap.put("3.1", hdfDescriptor31);

        stackMatrixV4.setHdp(hdpMap);
        stackMatrixV4.setHdf(hdfMap);

        when(stackMatrixService.getStackMatrix()).thenReturn(stackMatrixV4);

        AmbariInfo ambariInfo26 = new AmbariInfo();
        ambariInfo26.setVersion("2.6");
        ambariInfo26.setRepo(getAmbariRepo("2.6"));
        entries.put("2.6", ambariInfo26);

        AmbariInfo ambariInfo27 = new AmbariInfo();
        ambariInfo27.setVersion("2.7");
        ambariInfo27.setRepo(getAmbariRepo("2.7"));
        entries.put("2.7", ambariInfo27);
        defaultAmbariRepoService.setEntries(entries);
    }

    private Map<String, AmbariRepoDetailsJson> getAmbariRepoJson(String version) {
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

    private Map<String, AmbariRepoDetails> getAmbariRepo(String version) {
        Map<String, AmbariRepoDetails> ambariRepo = new HashMap<>();

        AmbariRepoDetails redhat6RepoDetails = new AmbariRepoDetails();
        redhat6RepoDetails.setBaseurl("http://redhat6-base/" + version);
        redhat6RepoDetails.setGpgkey("http://redhat6-gpg/" + version);
        ambariRepo.put("redhat6", redhat6RepoDetails);

        AmbariRepoDetails redhat7RepoDetails = new AmbariRepoDetails();
        redhat7RepoDetails.setBaseurl("http://redhat7-base/" + version);
        redhat7RepoDetails.setGpgkey("http://redhat7-gpg/" + version);
        ambariRepo.put("redhat7", redhat7RepoDetails);

        return ambariRepo;
    }

    @Test
    public void testDefaultAmbariRepoWithoutClusterTypeAndVersion() {
        AmbariRepo ambariRepo = defaultAmbariRepoService.getDefault("redhat7");
        assertNotNull(ambariRepo);
    }

    @Test
    public void testDefaultAmbariRepoWithoutClusterTypeAndVersionShouldReturnNullWhenOsTypeNotExist() {
        AmbariRepo ambariRepo = defaultAmbariRepoService.getDefault("redhat8");
        assertNull(ambariRepo);
    }

    @Test
    public void testDefaultAmbariRepoWithHDPWhichDoesNotExists() {
        AmbariRepo ambariRepo = defaultAmbariRepoService.getDefault("redhat7", "HDP", "2.6");
        assertNull(ambariRepo);
    }

    @Test
    public void testDefaultAmbariRepoWithOSWhichDoesNotExists() {
        AmbariRepo ambariRepo = defaultAmbariRepoService.getDefault("ubuntu", "HDP", "2.7");
        assertNull(ambariRepo);
    }

    @Test
    public void testDefaultAmbariRepoWithStackTypeWhichDoesNotExists() {
        AmbariRepo ambariRepo = defaultAmbariRepoService.getDefault("redhat7", "NA", "2.7");
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

    @Test
    public void testDefaultAmbariRepoWithHDFWhichExists() {
        AmbariRepo ambariRepo = defaultAmbariRepoService.getDefault("redhat7", "HDF", "3.1");
        assertEquals("2.7", ambariRepo.getVersion());
        assertEquals("http://redhat7-base/2.7", ambariRepo.getBaseUrl());
        assertEquals("http://redhat7-gpg/2.7", ambariRepo.getGpgKeyUrl());
    }
}