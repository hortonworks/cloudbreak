package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;
import com.sequenceiq.it.cloudbreak.config.CloudProviderEndpointsProperties;

@ExtendWith(MockitoExtension.class)
class CloudProviderEndpointResolverTest {

    @Mock
    private CommonCloudProperties commonCloudProperties;

    @Spy
    private CloudProviderEndpointsProperties cloudProviderEndpointsProperties = new CloudProviderEndpointsProperties();

    @Spy
    private AwsProperties awsProperties = new AwsProperties();

    @InjectMocks
    private CloudProviderEndpointResolver underTest;

    @BeforeEach
    void setUp() {
        cloudProviderEndpointsProperties.setAws(List.of("ec2.%s.amazonaws.com"));
        cloudProviderEndpointsProperties.setAzure(List.of("management.azure.com"));
        cloudProviderEndpointsProperties.setGcp(List.of("oauth2.googleapis.com"));
        awsProperties.setRegion("eu-central-1");
    }

    @Test
    void testResolveActiveProviderHostnamesForAws() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("AWS");

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).containsExactly("ec2.eu-central-1.amazonaws.com");
    }

    @Test
    void testResolveActiveProviderHostnamesForAwsWithoutPlaceholder() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("AWS");
        cloudProviderEndpointsProperties.setAws(List.of("sts.amazonaws.com"));

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).containsExactly("sts.amazonaws.com");
    }

    @Test
    void testResolveActiveProviderHostnamesForAzure() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("AZURE");

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).containsExactly("management.azure.com");
    }

    @Test
    void testResolveActiveProviderHostnamesForGcp() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("GCP");

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).containsExactly("oauth2.googleapis.com");
    }

    @Test
    void testResolveActiveProviderHostnamesForMock() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("MOCK");

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).isEmpty();
    }

    @Test
    void testResolveActiveProviderHostnamesForYarn() {
        when(commonCloudProperties.getCloudProvider()).thenReturn("YARN");

        List<String> result = underTest.resolveActiveProviderHostnames();

        assertThat(result).isEmpty();
    }
}
