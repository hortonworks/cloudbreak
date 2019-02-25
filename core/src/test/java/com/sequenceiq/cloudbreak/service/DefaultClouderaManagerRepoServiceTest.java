package com.sequenceiq.cloudbreak.service;


import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.service.exception.RepositoryCannotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class DefaultClouderaManagerRepoServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Before
    public void init() {
        Map<String, RepositoryInfo> entries = new HashMap<>();

        RepositoryInfo repositoryInfo = new RepositoryInfo();
        repositoryInfo.setVersion("6.1.0");

        Map<String, RepositoryDetails> repoMap = Maps.newHashMap();
        RepositoryDetails repositoryDetails = new RepositoryDetails();
        repositoryDetails.setBaseurl("https://archive.cloudera.com/cm6/6.1.0/redhat6/yum/");
        repositoryDetails.setGpgkey("https://archive.cloudera.com/cm6/6.1.0/redhat6/yum/RPM-GPG-KEY-cloudera");

        repoMap.put("redhat7", repositoryDetails);
        repoMap.put("amazonlinux2", repositoryDetails);
        repositoryInfo.setRepo(repoMap);

        entries.put("6.1", repositoryInfo);
        defaultClouderaManagerRepoService.setEntries(entries);
    }

    @Test
    public void testDefaultRepo() {
        ClouderaManagerRepo repo = defaultClouderaManagerRepoService.getDefault("redhat7");
        assertNotNull(repo);

        repo = defaultClouderaManagerRepoService.getDefault("amazonlinux2");
        assertNotNull(repo);
    }

    @Test
    public void testDefaultRepoWhenOsTypeNotExist() {
        thrown.expect(RepositoryCannotFoundException.class);
        thrown.expectMessage("Repository informations cannot found by given osType!");

        defaultClouderaManagerRepoService.getDefault("redhat8");
    }
}