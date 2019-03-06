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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAmbariRepoServiceTest {

    @InjectMocks
    private DefaultAmbariRepoService defaultAmbariRepoService;

    @Mock
    private StackMatrixService stackMatrixService;

    @Before
    public void init() {
        Map<String, RepositoryInfo> entries = new HashMap<>();

        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();
        Map<String, AmbariStackDescriptorV4Response> hdpMap = new HashMap<>();
        Map<String, AmbariStackDescriptorV4Response> hdfMap = new HashMap<>();

        AmbariInfoV4Response ambariInfoJson26 = new AmbariInfoV4Response();
        ambariInfoJson26.setVersion("2.6");
        ambariInfoJson26.setRepository(getAmbariRepoJson("2.6"));

        AmbariInfoV4Response ambariInfoJson27 = new AmbariInfoV4Response();
        ambariInfoJson27.setVersion("2.7");
        ambariInfoJson27.setRepository(getAmbariRepoJson("2.7"));

        AmbariStackDescriptorV4Response hdpDescriptor26 = new AmbariStackDescriptorV4Response();
        hdpDescriptor26.setAmbari(ambariInfoJson26);
        hdpMap.put("2.7", hdpDescriptor26);

        AmbariStackDescriptorV4Response hdpDescriptor30 = new AmbariStackDescriptorV4Response();
        hdpDescriptor30.setAmbari(ambariInfoJson27);
        hdpMap.put("3.0", hdpDescriptor30);

        stackMatrixV4Response.setHdp(hdpMap);

        AmbariStackDescriptorV4Response hdfDescriptor31 = new AmbariStackDescriptorV4Response();
        hdfDescriptor31.setAmbari(ambariInfoJson27);
        hdfMap.put("3.1", hdfDescriptor31);

        stackMatrixV4Response.setHdp(hdpMap);
        stackMatrixV4Response.setHdf(hdfMap);

        when(stackMatrixService.getStackMatrix()).thenReturn(stackMatrixV4Response);

        RepositoryInfo repositoryInfo26 = new RepositoryInfo();
        repositoryInfo26.setVersion("2.6");
        repositoryInfo26.setRepo(getAmbariRepo("2.6"));
        entries.put("2.6", repositoryInfo26);

        RepositoryInfo repositoryInfo27 = new RepositoryInfo();
        repositoryInfo27.setVersion("2.7");
        repositoryInfo27.setRepo(getAmbariRepo("2.7"));
        entries.put("2.7", repositoryInfo27);
        defaultAmbariRepoService.setEntries(entries);
    }

    private Map<String, AmbariRepositoryV4Response> getAmbariRepoJson(String version) {
        Map<String, AmbariRepositoryV4Response> ambariRepo = new HashMap<>();

        AmbariRepositoryV4Response redhat6RepoDetails = new AmbariRepositoryV4Response();
        redhat6RepoDetails.setBaseUrl("http://redhat6-base/" + version);
        redhat6RepoDetails.setGpgKeyUrl("http://redhat6-gpg/" + version);
        ambariRepo.put("redhat6", redhat6RepoDetails);

        AmbariRepositoryV4Response redhat7RepoDetails = new AmbariRepositoryV4Response();
        redhat7RepoDetails.setBaseUrl("http://redhat7-base/" + version);
        redhat7RepoDetails.setGpgKeyUrl("http://redhat7-gpg/" + version);
        ambariRepo.put("redhat7", redhat7RepoDetails);

        return ambariRepo;
    }

    private Map<String, RepositoryDetails> getAmbariRepo(String version) {
        Map<String, RepositoryDetails> ambariRepo = new HashMap<>();

        RepositoryDetails redhat6RepositoryDetails = new RepositoryDetails();
        redhat6RepositoryDetails.setBaseurl("http://redhat6-base/" + version);
        redhat6RepositoryDetails.setGpgkey("http://redhat6-gpg/" + version);
        ambariRepo.put("redhat6", redhat6RepositoryDetails);

        RepositoryDetails redhat7RepositoryDetails = new RepositoryDetails();
        redhat7RepositoryDetails.setBaseurl("http://redhat7-base/" + version);
        redhat7RepositoryDetails.setGpgkey("http://redhat7-gpg/" + version);
        ambariRepo.put("redhat7", redhat7RepositoryDetails);

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