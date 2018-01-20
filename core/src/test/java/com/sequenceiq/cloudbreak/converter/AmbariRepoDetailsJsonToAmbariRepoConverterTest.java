package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;

@RunWith(MockitoJUnitRunner.class)
public class AmbariRepoDetailsJsonToAmbariRepoConverterTest {

    @InjectMocks
    private AmbariRepoDetailsJsonToAmbariRepoConverter underTest;

    @Test(expected = CloudbreakImageCatalogException.class)
    public void testEmptyRepo() throws CloudbreakImageCatalogException {
        underTest.convert(new HashMap<>(), "ver", Boolean.TRUE);
    }

    @Test(expected = CloudbreakImageCatalogException.class)
    public void testNullRepo() throws CloudbreakImageCatalogException {
        underTest.convert(null, "ver", Boolean.TRUE);
    }

    @Test(expected = CloudbreakImageCatalogException.class)
    public void testMultipleRepos() throws CloudbreakImageCatalogException {
        Map<String, String> map = new HashMap<>();
        map.put("redhat6", "url1");
        map.put("redhat7", "url2");
        underTest.convert(map, "ver", Boolean.TRUE);
    }
}
