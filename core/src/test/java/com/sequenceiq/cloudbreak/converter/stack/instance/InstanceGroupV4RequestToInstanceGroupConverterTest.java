package com.sequenceiq.cloudbreak.converter.stack.instance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupV4RequestToInstanceGroupConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.network.InstanceGroupNetworkV4RequestToInstanceGroupNetworkConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.SecurityGroupV4RequestToSecurityGroupConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.InstanceTemplateV4RequestToTemplateConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;

@ExtendWith(MockitoExtension.class)
class InstanceGroupV4RequestToInstanceGroupConverterTest extends AbstractJsonConverterTest<InstanceGroupV4Request> {

    @InjectMocks
    private InstanceGroupV4RequestToInstanceGroupConverter underTest;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private SecurityGroupV4RequestToSecurityGroupConverter securityGroupV4RequestToSecurityGroupConverter;

    @Mock
    private InstanceGroupNetworkV4RequestToInstanceGroupNetworkConverter instanceGroupNetworkV4RequestToInstanceGroupNetworkConverter;

    @Mock
    private InstanceTemplateV4RequestToTemplateConverter instanceTemplateV4RequestToTemplateConverter;

    @Test
    void testConvert() {
        InstanceGroupV4Request request = getRequest("instance-group.json");
        // GIVEN
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(instanceTemplateV4RequestToTemplateConverter.convert(any(InstanceTemplateV4Request.class), eq(true))).willReturn(new Template());
        given(securityGroupV4RequestToSecurityGroupConverter.convert(any(SecurityGroupV4Request.class))).willReturn(new SecurityGroup());
        given(instanceGroupNetworkV4RequestToInstanceGroupNetworkConverter.convert(any(InstanceGroupNetworkV4Request.class)))
                .willReturn(new InstanceGroupNetwork());
        // WHEN
        InstanceGroup instanceGroup = underTest.convert(request, "variant");
        // THEN
        assertAllFieldsNotNull(instanceGroup, List.of("stack", "targetGroups"));
    }

    private Mappable getMappable() {
        return new Mappable() {
            @Override
            public Map<String, Object> asMap() {
                return new HashMap<>(Map.of("key", "value"));
            }

            @Override
            public CloudPlatform getCloudPlatform() {
                return null;
            }
        };
    }

    @Override
    public Class<InstanceGroupV4Request> getRequestClass() {
        return InstanceGroupV4Request.class;
    }
}
