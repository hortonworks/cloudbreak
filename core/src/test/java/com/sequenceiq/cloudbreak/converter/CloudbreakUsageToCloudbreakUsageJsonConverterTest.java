package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;

public class CloudbreakUsageToCloudbreakUsageJsonConverterTest extends AbstractEntityConverterTest<CloudbreakUsage> {

    @InjectMocks
    private CloudbreakUsageToCloudbreakUsageJsonConverter underTest;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Before
    public void setUp() {
        underTest = new CloudbreakUsageToCloudbreakUsageJsonConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(restRequestThreadLocalService.getCloudbreakUser()).willReturn(new CloudbreakUser("userId", "john.smith@example.com", "tenant"));
        // WHEN
        CloudbreakUsageJson result = underTest.convert(getSource());
        // THEN
        assertEquals(GCP, result.getProvider());
        assertEquals("john.smith@example.com", result.getUsername());
        assertAllFieldsNotNull(result, Lists.newArrayList("availabilityZone", "duration"));
    }

    @Test
    public void testConvertWithAwsProvider() {
        // GIVEN
        getSource().setProvider(AWS);
        getSource().setRegion("us_east_1");
        given(restRequestThreadLocalService.getCloudbreakUser()).willReturn(new CloudbreakUser("userId", "john.smith@example.com", "tenant"));
        // WHEN
        CloudbreakUsageJson result = underTest.convert(getSource());
        // THEN
        assertEquals(AWS, result.getProvider());
        assertEquals("john.smith@example.com", result.getUsername());
        assertAllFieldsNotNull(result, Lists.newArrayList("availabilityZone", "duration"));
    }

    @Test
    public void testConvertWithGcpProvider() {
        // GIVEN
        getSource().setProvider(GCP);
        getSource().setRegion("us_central1");
        getSource().setAvailabilityZone("us_central1_a");
        given(restRequestThreadLocalService.getCloudbreakUser()).willReturn(new CloudbreakUser("userId", "john.smith@example.com", "tenant"));
        // WHEN
        CloudbreakUsageJson result = underTest.convert(getSource());
        // THEN
        assertEquals(GCP, result.getProvider());
        assertEquals("john.smith@example.com", result.getUsername());
        assertAllFieldsNotNull(result, Lists.newArrayList("availabilityZone", "duration"));
    }

    @Override
    public CloudbreakUsage createSource() {
        return TestUtil.gcpCloudbreakUsage(1L);
    }

}
