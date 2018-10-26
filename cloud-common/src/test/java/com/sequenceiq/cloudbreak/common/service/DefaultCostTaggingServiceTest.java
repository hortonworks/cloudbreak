package com.sequenceiq.cloudbreak.common.service;

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType;
import com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag;
import com.sequenceiq.cloudbreak.service.Clock;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCostTaggingServiceTest {

    private static final CloudbreakUser CB_USER = new CloudbreakUser("123", "apache1@apache.com", "apache1@apache.com", "tenant");

    @Mock
    private Clock clock;

    @InjectMocks
    private DefaultCostTaggingService underTest;

    @Before
    public void before() {

        ReflectionTestUtils.setField(underTest, "cbVersion", "2.2.0");
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPrepareAllTagsForTemplateShouldReturnAllResourceMap() {
        Map<String, String> result = underTest.prepareAllTagsForTemplate();
        Assert.assertEquals(7L, result.size());
    }

    @Test
    public void testPrepareDefaultTagsForAWSShouldReturnAllDefaultMap() {
        long epochSeconds = 1526991986L;
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochSecond(epochSeconds));

        Map<String, String> result = underTest.prepareDefaultTags(CB_USER, new HashMap<>(), CloudConstants.AWS);

        Assert.assertEquals(4L, result.size());
        Assert.assertEquals("apache1@apache.com", result.get(DefaultApplicationTag.CB_USER_NAME.key()));
        Assert.assertEquals("2.2.0", result.get(DefaultApplicationTag.CB_VERSION.key()));
        Assert.assertEquals("apache1@apache.com", result.get(DefaultApplicationTag.OWNER.key()));
        Assert.assertEquals(String.valueOf(epochSeconds), result.get("cb-creation-timestamp"));
    }

    @Test
    public void testPrepareDefaultTagsForGCPShouldReturnAllDefaultMap() {
        long epochSeconds = 1526991986L;
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochSecond(epochSeconds));

        Map<String, String> result = underTest.prepareDefaultTags(CB_USER, new HashMap<>(), CloudConstants.GCP);

        Assert.assertEquals(4L, result.size());
        Assert.assertEquals("apache1", result.get(DefaultApplicationTag.CB_USER_NAME.key()));
        Assert.assertEquals("2-2-0", result.get(DefaultApplicationTag.CB_VERSION.key()));
        Assert.assertEquals("apache1", result.get(DefaultApplicationTag.OWNER.key().toLowerCase()));
        Assert.assertEquals(String.valueOf(epochSeconds), result.get("cb-creation-timestamp"));
    }

    @Test
    public void testPrepareDefaultTagsForAZUREWhenOwnerPresentedShouldReturnAllDefaultMap() {
        long epochSeconds = 1526991986L;
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochSecond(epochSeconds));
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put(DefaultApplicationTag.OWNER.key(), "appletree");

        Map<String, String> result = underTest.prepareDefaultTags(CB_USER, sourceMap, CloudConstants.AZURE);

        Assert.assertEquals(3L, result.size());
        Assert.assertEquals("apache1@apache.com", result.get(DefaultApplicationTag.CB_USER_NAME.key()));
        Assert.assertEquals("2.2.0", result.get(DefaultApplicationTag.CB_VERSION.key()));
        Assert.assertNull(result.get(DefaultApplicationTag.OWNER.key()));
        Assert.assertEquals(String.valueOf(epochSeconds), result.get("cb-creation-timestamp"));
    }

    @Test
    public void testPrepareInstanceTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareInstanceTagging();
        Assert.assertEquals(1L, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.INSTANCE.key());
    }

    @Test
    public void testPrepareTemplateTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareTemplateTagging();
        Assert.assertEquals(1L, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.TEMPLATE.key());
    }

    @Test
    public void testPrepareStorageTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareStorageTagging();
        Assert.assertEquals(1L, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.STORAGE.key());
    }

    @Test
    public void testPrepareDiskTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareDiskTagging();
        Assert.assertEquals(1L, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.DISK.key());
    }

    @Test
    public void testPrepareIpTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareIpTagging();
        Assert.assertEquals(1L, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.IP.key());
    }

    @Test
    public void testPrepareNetworkTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareNetworkTagging();
        Assert.assertEquals(1L, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.NETWORK.key());
    }

    @Test
    public void testPrepareSecurityTaggingShouldReturnAMapWithSingleEntry() {
        Map<String, String> result = underTest.prepareSecurityTagging();
        Assert.assertEquals(1L, result.size());
        Assert.assertEquals(result.get(DefaultApplicationTag.CB_RESOURCE_TYPE.key()), CloudbreakResourceType.SECURITY.key());
    }
}