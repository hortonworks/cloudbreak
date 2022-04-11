package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.Template;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TemplateToInstanceTemplateV4RequestConverterTest {

    @InjectMocks
    private TemplateToInstanceTemplateV4RequestConverter underTest;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAttachedVolumesWithoutCount() {
        InstanceTemplateV4Request request = createInstanceTemplateV4Request(false);
        request.getAttachedVolumes().stream().findFirst().ifPresent(actual -> assertEquals(0, actual.getCount()));
    }

    @Test
    public void testAttachedVolumesWithCount() {
        InstanceTemplateV4Request request = createInstanceTemplateV4Request(true);
        request.getAttachedVolumes().stream().findFirst().ifPresent(actual -> assertEquals(1, actual.getCount()));
    }

    private InstanceTemplateV4Request createInstanceTemplateV4Request(boolean haveCount) {
        Template template = createTemplate(haveCount);
        return underTest.convert(template);
    }

    private Template createTemplate(boolean haveCount) {
        Template template = new Template();
        template.setAttributes(new Json(Map.of("someAttr", "value")));
        template.setSecretAttributes(new Json(Map.of("otherAttr", "value")).getValue());

        VolumeTemplate volume = new VolumeTemplate();
        volume.setVolumeSize(100);
        volume.setVolumeType("HDD");
        if (haveCount) {
            volume.setVolumeCount(1);
        }
        Set<VolumeTemplate> volumes = new HashSet<>();
        volumes.add(volume);
        template.setVolumeTemplates(volumes);
        return template;
    }
}
