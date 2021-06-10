package com.sequenceiq.cloudbreak.converter.stack.instance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupV4RequestToInstanceGroupConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;

@RunWith(MockitoJUnitRunner.class)
public class InstanceGroupV4RequestToInstanceGroupConverterTest extends AbstractJsonConverterTest<InstanceGroupV4Request> {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private InstanceGroupV4RequestToInstanceGroupConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Test
    public void testConvert() {
        InstanceGroupV4Request request = getRequest("instance-group.json");
        // GIVEN
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(conversionService.convert(any(InstanceTemplateV4Request.class), eq(Template.class))).willReturn(new Template());
        given(conversionService.convert(any(SecurityGroupV4Request.class), eq(SecurityGroup.class))).willReturn(new SecurityGroup());
        given(conversionService.convert(any(InstanceGroupNetworkV4Request.class), eq(InstanceGroupNetwork.class)))
                .willReturn(new InstanceGroupNetwork());
        // WHEN
        InstanceGroup instanceGroup = underTest.convert(request);
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
