package com.sequenceiq.cloudbreak.structuredevent.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupToInstanceGroupDetailsConverterTest {

    @InjectMocks
    private InstanceGroupToInstanceGroupDetailsConverter underTest;

    @Test
    void convertEmptyNoNullPointer() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setSecurityGroup(new SecurityGroup());

        InstanceGroupDetails instanceGroupDetails = underTest.convert(instanceGroup, List.of());

        assertThat(instanceGroupDetails).isNotNull();
    }

    @Test
    void convert() {
        InstanceGroupDetails instanceGroupDetails = underTest.convert(createInstanceGroup(), List.of());

        assertEquals("ATTACHED_VOLUMES", instanceGroupDetails.getTemporaryStorage());
        // keep only encryted
        assertEquals(Boolean.TRUE, instanceGroupDetails.getAttributes().get("encrypted"));
        // remove everything else
        assertNull(instanceGroupDetails.getAttributes().get("everything-else"));
    }

    private InstanceGroup createInstanceGroup() {
        InstanceGroup instanceGroup = new InstanceGroup();
        Template template = new Template();
        Map<String, Object> attributes = Map.of("encrypted", Boolean.TRUE, "everything-else", Boolean.TRUE);
        template.setAttributes(new Json(attributes));
        template.setCloudPlatform("AWS");
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }
}