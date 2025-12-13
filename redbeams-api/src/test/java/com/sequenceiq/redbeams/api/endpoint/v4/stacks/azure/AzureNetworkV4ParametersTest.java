package com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class AzureNetworkV4ParametersTest {

    private AzureNetworkV4Parameters underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AzureNetworkV4Parameters();
    }

    @Test
    void testGettersAndSetters() {
        underTest.setSubnets("someSubnets");
        assertThat(underTest.getSubnets()).isEqualTo("someSubnets");
    }

    @Test
    void testAsMap() {
        underTest.setSubnets("someSubnets");

        assertThat(underTest.asMap()).containsOnly(Map.entry("subnets", "someSubnets"),
                Map.entry("cloudPlatform", "AZURE"));
    }

    @Test
    void testGetCloudPlatform() {
        assertThat(underTest.getCloudPlatform()).isEqualTo(CloudPlatform.AZURE);
    }

    @Test
    void testParse() {
        Map<String, Object> parameters = Map.of("subnets", "someSubnets");

        underTest.parse(parameters);

        assertThat(underTest.getSubnets()).isEqualTo("someSubnets");
    }

}
