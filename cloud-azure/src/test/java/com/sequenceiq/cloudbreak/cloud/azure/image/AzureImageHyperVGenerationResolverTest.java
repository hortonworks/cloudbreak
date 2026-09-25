package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.azure.resourcemanager.compute.models.HyperVGenerationTypes;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;

class AzureImageHyperVGenerationResolverTest {

    private static final String IMAGE_NAME = "https://cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd";

    private final AzureImageHyperVGenerationResolver underTest = new AzureImageHyperVGenerationResolver();

    @Test
    void testResolveWhenGen1Tag() {
        assertEquals(HyperVGenerationTypes.V1, underTest.resolve(imageWithTags(Map.of("azure-hyper-v-generation", "1"))));
    }

    @Test
    void testResolveWhenGen2Tag() {
        assertEquals(HyperVGenerationTypes.V2, underTest.resolve(imageWithTags(Map.of("azure-hyper-v-generation", "2"))));
    }

    @Test
    void testResolveWhenTagAbsent() {
        assertNull(underTest.resolve(imageWithTags(Map.of("release-version", "7.2.18"))));
    }

    @Test
    void testResolveWhenNoTagsAtAll() {
        assertNull(underTest.resolve(imageWithTags(null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "3", "V2", "gen2", "two"})
    void testResolveWhenTagValueUnrecognised(String tagValue) {
        assertNull(underTest.resolve(imageWithTags(Map.of("azure-hyper-v-generation", tagValue))));
    }

    @Test
    void testResolveWhenImageIsNull() {
        assertNull(underTest.resolve((Image) null));
    }

    @Test
    void testResolveInstanceWhenGen2Parameter() {
        assertEquals(HyperVGenerationTypes.V2, underTest.resolve(instanceWithParameters(Map.of(AzureInstanceTemplate.HYPER_V_GENERATION, "V2"))));
    }

    @Test
    void testResolveInstanceWhenGen1Parameter() {
        assertEquals(HyperVGenerationTypes.V1, underTest.resolve(instanceWithParameters(Map.of(AzureInstanceTemplate.HYPER_V_GENERATION, "V1"))));
    }

    @Test
    void testResolveInstanceWhenParameterAbsent() {
        assertNull(underTest.resolve(instanceWithParameters(Map.of())));
    }

    @Test
    void testResolveInstanceWhenInstanceIsNull() {
        assertNull(underTest.resolve((CloudInstance) null));
    }

    private CloudInstance instanceWithParameters(Map<String, Object> parameters) {
        return new CloudInstance("instanceId", null, null, null, null, parameters);
    }

    private Image imageWithTags(Map<String, String> tags) {
        return new Image(IMAGE_NAME, Map.of(), "centos7", "redhat7", null, "http://catalog", "catalog", "imageId", Map.of(), null, 0L, tags);
    }
}
