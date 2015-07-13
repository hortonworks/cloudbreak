package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

public class CloudbreakUsageToJsonEntityConverterTest extends AbstractEntityConverterTest<CloudbreakUsage> {
    @InjectMocks
    private CloudbreakUsageToJsonConverter underTest;

    @Mock
    private UserDetailsService userDetailsService;

    private CbUser user;

    @Before
    public void setUp() {
        underTest = new CloudbreakUsageToJsonConverter();
        user = createCbUser();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).willReturn(user);
        // WHEN
        CloudbreakUsageJson result = underTest.convert(getSource());
        // THEN
        assertEquals(CloudPlatform.AZURE.name(), result.getProvider());
        assertEquals("john.smith@example.com", result.getUsername());
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithAwsProvider() {
        // GIVEN
        getSource().setProvider(CloudPlatform.AWS.name());
        getSource().setRegion(CloudRegion.US_EAST_1.name());
        given(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).willReturn(user);
        // WHEN
        CloudbreakUsageJson result = underTest.convert(getSource());
        // THEN
        assertEquals(CloudPlatform.AWS.name(), result.getProvider());
        assertEquals("john.smith@example.com", result.getUsername());
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithGcpProvider() {
        // GIVEN
        getSource().setProvider(CloudPlatform.GCP.name());
        getSource().setRegion(CloudRegion.US_CENTRAL1_A.name());
        given(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).willReturn(user);
        // WHEN
        CloudbreakUsageJson result = underTest.convert(getSource());
        // THEN
        assertEquals(CloudPlatform.GCP.name(), result.getProvider());
        assertEquals("john.smith@example.com", result.getUsername());
        assertAllFieldsNotNull(result);
    }

    @Override
    public CloudbreakUsage createSource() {
        return TestUtil.azureCloudbreakUsage(1L);
    }

    private CbUser createCbUser() {
        return new CbUser("dummyUserId", "john.smith@example.com", "dummyAccount",
                Arrays.asList(CbUserRole.ADMIN, CbUserRole.USER), "John", "Smith");
    }
}
