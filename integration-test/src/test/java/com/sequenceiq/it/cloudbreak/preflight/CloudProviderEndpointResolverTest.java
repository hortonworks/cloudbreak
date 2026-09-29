package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;
import com.sequenceiq.it.cloudbreak.config.CloudProviderEndpointsProperties;

public class CloudProviderEndpointResolverTest {

    private CommonCloudProperties commonCloudProperties;

    private CloudProviderEndpointsProperties cloudProviderEndpointsProperties;

    private AwsProperties awsProperties;

    private CloudProviderEndpointResolver underTest;

    @BeforeMethod
    public void setUp() {
        commonCloudProperties = mock(CommonCloudProperties.class);
        cloudProviderEndpointsProperties = new CloudProviderEndpointsProperties();
        cloudProviderEndpointsProperties.setAws(List.of("ec2.%s.amazonaws.com"));
        cloudProviderEndpointsProperties.setAzure(List.of("management.azure.com"));
        cloudProviderEndpointsProperties.setGcp(List.of("oauth2.googleapis.com"));

        underTest = new CloudProviderEndpointResolver();
        ReflectionTestUtils.setField(underTest, "commonCloudProperties", commonCloudProperties);
        ReflectionTestUtils.setField(underTest, "cloudProviderEndpointsProperties", cloudProviderEndpointsProperties);
        awsProperties = new AwsProperties();
        awsProperties.setRegion("eu-central-1");
        ReflectionTestUtils.setField(underTest, "awsProperties", awsProperties);
    }

    @Test
    public void testResolveActiveProviderHostnamesForAws() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("AWS");

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).containsExactly("ec2.eu-central-1.amazonaws.com");
    }

    @Test
    public void testResolveActiveProviderHostnamesForAwsWithoutPlaceholder() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("AWS");
        cloudProviderEndpointsProperties.setAws(List.of("sts.amazonaws.com"));

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).containsExactly("sts.amazonaws.com");
    }

    @Test
    public void testResolveActiveProviderHostnamesForAzure() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("AZURE");

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).containsExactly("management.azure.com");
    }

    @Test
    public void testResolveActiveProviderHostnamesForGcp() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("GCP");

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).containsExactly("oauth2.googleapis.com");
    }

    @Test
    public void testResolveActiveProviderHostnamesForMock() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("MOCK");

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).isEmpty();
    }

    @Test
    public void testResolveActiveProviderHostnamesForYarn() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("YARN");

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).isEmpty();
    }
}
