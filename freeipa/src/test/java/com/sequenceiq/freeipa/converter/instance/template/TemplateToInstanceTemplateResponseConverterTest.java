package com.sequenceiq.freeipa.converter.instance.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeResponse;
import com.sequenceiq.freeipa.entity.Template;

@ExtendWith(MockitoExtension.class)
class TemplateToInstanceTemplateResponseConverterTest {

    private static final String INSTANCE_TYPE = "m5.2xlarge";

    private static final Integer VOLUME_COUNT = 2;

    private static final Integer VOLUME_SIZE = 100;

    private static final String VOLUME_TYPE = "gp2";

    @Mock
    private TemplateToVolumeResponseConverter volumeResponseConverter;

    @InjectMocks
    private TemplateToInstanceTemplateResponseConverter underTest;

    @Test
    void testConvertShouldSetInstanceTypeAndVolumes() {
        Template template = createTemplate();
        template.setAttributes(null);
        VolumeResponse volumeResponse = createVolumeResponse();
        when(volumeResponseConverter.convert(template)).thenReturn(volumeResponse);

        InstanceTemplateResponse result = underTest.convert(template);

        assertThat(result.getInstanceType()).isEqualTo(INSTANCE_TYPE);
        assertThat(result.getAttachedVolumes()).hasSize(1);
        assertThat(result.getAttachedVolumes()).containsExactly(volumeResponse);
        assertThat(result.getAttributes()).isEmpty();
    }

    @Test
    void testConvertShouldSetAttributesWhenAttributesNotNull() {
        Template template = createTemplate();
        Map<String, Object> attributeMap = Map.of("key1", "value1", "key2", 42);
        template.setAttributes(new Json(attributeMap));
        VolumeResponse volumeResponse = createVolumeResponse();
        when(volumeResponseConverter.convert(template)).thenReturn(volumeResponse);

        InstanceTemplateResponse result = underTest.convert(template);

        assertThat(result.getInstanceType()).isEqualTo(INSTANCE_TYPE);
        assertThat(result.getAttributes()).isEqualTo(attributeMap);
    }

    @Test
    void testConvertShouldNotSetAttributesWhenAttributesNull() {
        Template template = createTemplate();
        template.setAttributes(null);
        VolumeResponse volumeResponse = createVolumeResponse();
        when(volumeResponseConverter.convert(template)).thenReturn(volumeResponse);

        InstanceTemplateResponse result = underTest.convert(template);

        assertThat(result.getAttributes()).isEmpty();
    }

    @Test
    void testConvertShouldNotSetAttributesWhenAttributesMapIsNull() {
        Template template = createTemplate();
        template.setAttributes(new Json("null"));
        VolumeResponse volumeResponse = createVolumeResponse();
        when(volumeResponseConverter.convert(template)).thenReturn(volumeResponse);

        InstanceTemplateResponse result = underTest.convert(template);

        assertThat(result.getAttributes()).isEmpty();
    }

    @Test
    void testConvertShouldNotSetFallbackInstanceTypesOnResponseWhenSourceHasFallbackTypes() {
        Template template = createTemplate();
        template.setAttributes(null);
        Json fallbackJson = Json.silent(List.of("m5.xlarge", "m5.large"));
        template.setFallbackInstanceTypes(fallbackJson);
        VolumeResponse volumeResponse = createVolumeResponse();
        when(volumeResponseConverter.convert(template)).thenReturn(volumeResponse);

        InstanceTemplateResponse result = underTest.convert(template);

        assertThat(result.getFallbackInstanceTypes()).isNotNull();
    }

    @Test
    void testConvertShouldNotModifySourceFallbackInstanceTypesWhenSourceHasNullFallback() {
        Template template = createTemplate();
        template.setAttributes(null);
        template.setFallbackInstanceTypes(null);
        VolumeResponse volumeResponse = createVolumeResponse();
        when(volumeResponseConverter.convert(template)).thenReturn(volumeResponse);

        InstanceTemplateResponse result = underTest.convert(template);

        assertThat(result.getFallbackInstanceTypes()).isNull();
        assertThat(template.getFallbackInstanceTypes()).isNull();
    }

    @Test
    void testConvertShouldNotOverwriteSourceFallbackInstanceTypes() {
        Template template = createTemplate();
        template.setAttributes(null);
        Json originalFallback = Json.silent(List.of("m5.xlarge", "m5.large"));
        template.setFallbackInstanceTypes(originalFallback);
        VolumeResponse volumeResponse = createVolumeResponse();
        when(volumeResponseConverter.convert(template)).thenReturn(volumeResponse);

        underTest.convert(template);

        assertThat(template.getFallbackInstanceTypes()).isEqualTo(originalFallback);
    }

    private Template createTemplate() {
        Template template = new Template();
        template.setInstanceType(INSTANCE_TYPE);
        template.setVolumeCount(VOLUME_COUNT);
        template.setVolumeSize(VOLUME_SIZE);
        template.setVolumeType(VOLUME_TYPE);
        return template;
    }

    private VolumeResponse createVolumeResponse() {
        VolumeResponse volumeResponse = new VolumeResponse();
        volumeResponse.setCount(VOLUME_COUNT);
        volumeResponse.setSize(VOLUME_SIZE);
        volumeResponse.setType(VOLUME_TYPE);
        return volumeResponse;
    }
}
