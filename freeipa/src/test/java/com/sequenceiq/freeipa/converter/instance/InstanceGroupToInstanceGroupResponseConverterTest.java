package com.sequenceiq.freeipa.converter.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.converter.instance.template.TemplateToInstanceTemplateResponseConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupAvailabilityZone;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupAvailabilityZoneService;

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

    @Mock
    private InstanceGroupAvailabilityZoneService availabilityZoneService;

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
        instanceGroup.setId(3L);
        instanceGroup.setInstanceGroupNetwork(new InstanceGroupNetwork());
        Set<InstanceGroupAvailabilityZone> instanceGroupAvailabilityZones = Set.of("1", "2", "3").stream().map(s -> {
            InstanceGroupAvailabilityZone instanceGroupAvailabilityZone = new InstanceGroupAvailabilityZone();
            instanceGroupAvailabilityZone.setAvailabilityZone(s);
            instanceGroupAvailabilityZone.setInstanceGroup(instanceGroup);
            return instanceGroupAvailabilityZone;
        }).collect(Collectors.toSet());
        instanceGroup.setAvailabilityZones(instanceGroupAvailabilityZones);
        when(availabilityZoneService.findAllByInstanceGroupId(instanceGroup.getId())).thenReturn(instanceGroupAvailabilityZones);

        InstanceGroupResponse instanceGroupResponse = underTest.convert(instanceGroup, true);

        assertEquals(Set.of("1", "2", "3"), instanceGroupResponse.getAvailabilityZones());
    }

    private InstanceGroup getInstanceGroup() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of());
        return instanceGroup;
    }

}
