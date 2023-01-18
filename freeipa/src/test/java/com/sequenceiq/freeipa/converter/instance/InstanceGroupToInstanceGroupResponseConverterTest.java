package com.sequenceiq.freeipa.converter.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.converter.instance.template.TemplateToInstanceTemplateResponseConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupToInstanceGroupResponseConverterTest {
    @Mock
    private TemplateToInstanceTemplateResponseConverter templateResponseConverter;

    @Mock
    private SecurityGroupToSecurityGroupResponseConverter securityGroupConverter;

    @Mock
    private InstanceGroupNetworkToInstanceGroupNetworkResponseConverter instanceGroupNetworkConverter;

    @Mock
    private InstanceMetaDataToInstanceMetaDataResponseConverter metaDataConverter;

    @InjectMocks
    private InstanceGroupToInstanceGroupResponseConverter underTest;

    @Test
    void testConvertAvailabilityZonesIsNull() {
        InstanceGroupResponse instanceGroupResponse = underTest.convert(getInstanceGroup(), true);
        assertEquals(Set.of(), instanceGroupResponse.getAvailabilityZones());
    }

    @Test
    void testConvertAvailabilityZonesIsEmpty() {
        InstanceGroup instanceGroup = getInstanceGroup();
        instanceGroup.setInstanceGroupNetwork(new InstanceGroupNetwork());
        instanceGroup.setAvailabilityZones(Set.of());
        InstanceGroupResponse instanceGroupResponse = underTest.convert(instanceGroup, true);
        assertEquals(Set.of(), instanceGroupResponse.getAvailabilityZones());
    }

    @Test
    void testConvertAvailabilityZonesIsNotEmpty() {
        InstanceGroup instanceGroup = getInstanceGroup();
        instanceGroup.setInstanceGroupNetwork(new InstanceGroupNetwork());
        instanceGroup.setAvailabilityZones(Set.of("1", "2", "3"));
        InstanceGroupResponse instanceGroupResponse = underTest.convert(instanceGroup, true);
        assertEquals(Set.of("1", "2", "3"), instanceGroupResponse.getAvailabilityZones());
    }

    private InstanceGroup getInstanceGroup() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of());
        return instanceGroup;
    }

}
