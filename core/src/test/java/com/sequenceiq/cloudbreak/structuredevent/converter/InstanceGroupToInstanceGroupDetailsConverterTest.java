package com.sequenceiq.cloudbreak.structuredevent.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupToInstanceGroupDetailsConverterTest {

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private InstanceGroupToInstanceGroupDetailsConverter underTest;

    @Test
    void convertEmptyNoNullPointer() {
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceGroupDetails instanceGroupDetails = underTest.convert(instanceGroup);

        assertThat(instanceGroupDetails).isNotNull();
    }

    @Test
    void convert() {
        InstanceGroupDetails instanceGroupDetails = underTest.convert(createInstanceGroup());

        assertEquals("ATTACHED_VOLUMES", instanceGroupDetails.getTemporaryStorage());
        assertEquals(Boolean.TRUE, instanceGroupDetails.getAttributes().get("encrypted"));
    }

    private InstanceGroup createInstanceGroup() {
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        Map<String, Object> attributes = Map.of("encrypted", Boolean.TRUE);
        template.setAttributes(new Json(attributes));
        template.setCloudPlatform("AWS");
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }
}