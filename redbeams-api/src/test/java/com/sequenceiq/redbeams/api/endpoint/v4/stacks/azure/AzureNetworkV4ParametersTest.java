package com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class AzureNetworkV4ParametersTest {

    private AzureNetworkV4Parameters underTest;

    @Before
    public void setUp() {
        underTest = new AzureNetworkV4Parameters();
    }

    @Test
    public void testGettersAndSetters() {
        underTest.setVirtualNetwork("someVirtualNetwork");
        assertThat(underTest.getVirtualNetwork()).isEqualTo("someVirtualNetwork");
    }

    @Test
    public void testAsMap() {
        underTest.setVirtualNetwork("someVirtualNetwork");

        assertThat(underTest.asMap()).containsOnly(Map.entry("virtualNetwork", "someVirtualNetwork"),
                Map.entry("cloudPlatform", "AZURE"));
    }

    @Test
    public void testGetCloudPlatform() {
        assertThat(underTest.getCloudPlatform()).isEqualTo(CloudPlatform.AZURE);
    }

    @Test
    public void testParse() {
        Map<String, Object> parameters = Map.of("virtualNetwork", "someVirtualNetwork");

        underTest.parse(parameters);

        assertThat(underTest.getVirtualNetwork()).isEqualTo("someVirtualNetwork");
    }

}
