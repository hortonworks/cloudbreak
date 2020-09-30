package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.common.api.type.EncryptionType;

@ExtendWith(MockitoExtension.class)
public class TemplateToInstanceTemplateV4ResponseConverterTest {

    @Spy
    private ProviderParameterCalculator providerParameterCalculator;

    @InjectMocks
    private TemplateToInstanceTemplateV4ResponseConverter underTest;

    @Test
    public void testAwsEncryptionConvert() {
        String encyptionKey = "aKey";
        Template template = new Template();
        Map<String, String> attributes = Maps.newHashMap();
        attributes.put(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, encyptionKey);
        attributes.put(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "CUSTOM");
        attributes.put("cloudPlatform", "AWS");
        template.setAttributes(new Json(attributes));
        template.setVolumeTemplates(Sets.newHashSet());
        template.setCloudPlatform("AWS");

        InstanceTemplateV4Response response = underTest.convert(template);

        assertEquals(response.getAws().getEncryption().getType(), EncryptionType.CUSTOM);
        assertEquals(response.getAws().getEncryption().getKey(), encyptionKey);
    }
}
