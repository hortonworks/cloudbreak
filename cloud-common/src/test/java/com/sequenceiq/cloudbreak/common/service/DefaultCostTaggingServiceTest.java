package com.sequenceiq.cloudbreak.common.service;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType;
import com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCostTaggingServiceTest {

    @InjectMocks
    private DefaultCostTaggingService underTest;

    @Before
    public void before() {
        ReflectionTestUtils.setField(underTest, "cbVersion", "2.2.0");
    }

    @Test
    public void testPrepareAllTagsForTemplateShouldReturnAllResourceMap() {
        Map<String, String> result = underTest.prepareAllTagsForTemplate();
        Assert.assertEquals(7, result.size());
    }

    @Test
    public void testPrepareDefaultTagsForAWSShouldReturnAllDefaultMap() {
        Map<String, String> result = underTest.prepareDefaultTags("Apache", "apache1@apache.com", new HashMap<>(), CloudConstants.AWS);
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("Apache", result.get(DefaultApplicationTag.CB_ACOUNT_NAME.key()));
        Assert.assertEquals("apache1@apache.com", result.get(DefaultApplicationTag.CB_USER_NAME.key()));
        Assert.assertEquals("2.2.0", result.get(DefaultApplicationTag.CB_VERSION.key()));
        Assert.assertEquals("apache1@apache.com", result.get(DefaultApplicationTag.OWNER.key()));
    }

    @Test
    public void testPrepareDefaultTagsForGCPShouldReturnAllDefaultMap() {
        Map<String, String> result = underTest.prepareDefaultTags("Apache", "apache1@apache.com", new HashMap<>(), CloudConstants.GCP);
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("apache", result.get(DefaultApplicationTag.CB_ACOUNT_NAME.key()));
        Assert.assertEquals("apache1", result.get(DefaultApplicationTag.CB_USER_NAME.key()));
        Assert.assertEquals("2-2-0", result.get(DefaultApplicationTag.CB_VERSION.key()));
        Assert.assertEquals("apache1", result.get(DefaultApplicationTag.OWNER.key().toLowerCase()));
    }

    @Test
    public void testPrepareDefaultTagsForAZUREWhenOwnerPresentedShouldReturnAllDefaultMap() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put(DefaultApplicationTag.OWNER.key(), "appletree");
        Map<String, String> result = underTest.prepareDefaultTags("Apache", "apache1@apache.com", sourceMap, CloudConstants.AZURE);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Apache", result.get(DefaultApplicationTag.CB_ACOUNT_NAME.key()));
        Assert.assertEquals("apache1@apache.com", result.get(DefaultApplicationTag.CB_USER_NAME.key()));
        Assert.assertEquals("2.2.0", result.get(DefaultApplicationTag.CB_VERSION.key()));
        Assert.assertNull(result.get(DefaultApplicationTag.OWNER.key()));
    }

    @Test
    public void testPrepareInstanceTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareInstanceTagging();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.INSTANCE.key());
    }

    @Test
    public void testPrepareTemplateTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareTemplateTagging();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.TEMPLATE.key());
    }

    @Test
    public void testPrepareStorageTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareStorageTagging();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.STORAGE.key());
    }

    @Test
    public void testPrepareDiskTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareDiskTagging();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.DISK.key());
    }

    @Test
    public void testPrepareIpTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareIpTagging();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.IP.key());
    }

    @Test
    public void testPrepareNetworkTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareNetworkTagging();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.NETWORK.key());
    }

    @Test
    public void testPrepareSecurityTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareSecurityTagging();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.SECURITY.key());
    }

}