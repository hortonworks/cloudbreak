package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

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
        assertEquals(Optional.of(HyperVGenerationTypes.V1), underTest.resolve(imageWithTags(Map.of("azure-hyper-v-generation", "1"))));
    }

    @Test
    void testResolveWhenGen2Tag() {
        assertEquals(Optional.of(HyperVGenerationTypes.V2), underTest.resolve(imageWithTags(Map.of("azure-hyper-v-generation", "2"))));
    }

    @Test
    void testResolveWhenTagAbsent() {
        assertTrue(underTest.resolve(imageWithTags(Map.of("release-version", "7.2.18"))).isEmpty());
    }

    @Test
    void testResolveWhenNoTagsAtAll() {
        assertTrue(underTest.resolve(imageWithTags(null)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "3", "V2", "gen2", "two"})
    void testResolveWhenTagValueUnrecognised(String tagValue) {
        assertTrue(underTest.resolve(imageWithTags(Map.of("azure-hyper-v-generation", tagValue))).isEmpty());
    }

    @Test
    void testResolveWhenImageIsNull() {
        assertTrue(underTest.resolve((Image) null).isEmpty());
    }

    @Test
    void testResolveInstanceWhenGen2Parameter() {
        assertEquals(Optional.of(HyperVGenerationTypes.V2), underTest.resolve(instanceWithParameters(Map.of(AzureInstanceTemplate.HYPER_V_GENERATION, "V2"))));
    }

    @Test
    void testResolveInstanceWhenGen1Parameter() {
        assertEquals(Optional.of(HyperVGenerationTypes.V1), underTest.resolve(instanceWithParameters(Map.of(AzureInstanceTemplate.HYPER_V_GENERATION, "V1"))));
    }

    @Test
    void testResolveInstanceWhenParameterAbsent() {
        assertTrue(underTest.resolve(instanceWithParameters(Map.of())).isEmpty());
    }

    @Test
    void testResolveInstanceWhenInstanceIsNull() {
        assertTrue(underTest.resolve((CloudInstance) null).isEmpty());
    }

    private CloudInstance instanceWithParameters(Map<String, Object> parameters) {
        return new CloudInstance("instanceId", null, null, null, null, parameters);
    }

    private Image imageWithTags(Map<String, String> tags) {
        return new Image(IMAGE_NAME, Map.of(), "centos7", "redhat7", null, "http://catalog", "catalog", "imageId", Map.of(), null, 0L, tags);
    }
}
