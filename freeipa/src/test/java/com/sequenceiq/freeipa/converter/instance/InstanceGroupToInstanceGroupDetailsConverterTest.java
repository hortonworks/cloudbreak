package com.sequenceiq.freeipa.converter.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Template;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupToInstanceGroupDetailsConverterTest {

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

        assertEquals(Boolean.TRUE, instanceGroupDetails.getAttributes().get("encrypted"));
        assertNull(instanceGroupDetails.getAttributes().get("everything-else"));
    }

    private InstanceGroup createInstanceGroup() {
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        Map<String, Object> attributes = Map.of("encrypted", Boolean.TRUE, "everything-else", Boolean.TRUE);
        template.setAttributes(new Json(attributes));
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }
}
