package com.sequenceiq.freeipa.converter.instance;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.converter.instance.template.InstanceTemplateRequestToTemplateConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider;

@RunWith(MockitoJUnitRunner.class)
public class InstanceGroupRequestToInstanceGroupConverterTest {

    private static final String ACCOUNT_ID = "account_id";

    private static final String CLOUD_PLATFORM = "MOCK";

    private static final String NAME = "NAME";

    @InjectMocks
    private InstanceGroupRequestToInstanceGroupConverter underTest;

    @Mock
    private InstanceTemplateRequestToTemplateConverter templateConverter;

    @Mock
    private SecurityGroupRequestToSecurityGroupConverter securityGroupConverter;

    @Mock
    private DefaultInstanceGroupProvider defaultInstanceGroupProvider;

    @Test
    public void testConvertWithNullTemplate() {
        int nodeCount = 2;
        InstanceGroupRequest request = new InstanceGroupRequest();
        request.setName(NAME);
        request.setType(InstanceGroupType.MASTER);
        request.setNodeCount(nodeCount);

        Template template = mock(Template.class);
        SecurityGroupRequest securityGroupRequest = mock(SecurityGroupRequest.class);
        SecurityGroup securityGroup = mock(SecurityGroup.class);
        request.setSecurityGroup(securityGroupRequest);

        // GIVEN
        given(defaultInstanceGroupProvider.createDefaultTemplate(eq(MOCK), eq(ACCOUNT_ID))).willReturn(template);
        given(securityGroupConverter.convert(eq(securityGroupRequest))).willReturn(securityGroup);
        // WHEN
        InstanceGroup result = underTest.convert(request, ACCOUNT_ID, CLOUD_PLATFORM);
        // THEN
        assertEquals(NAME, result.getGroupName());
        assertEquals(InstanceGroupType.MASTER, result.getInstanceGroupType());
        assertEquals(securityGroup, result.getSecurityGroup());
        assertEquals(nodeCount, result.getNodeCount());
        assertEquals(nodeCount, result.getInstanceMetaData().size());
        for (InstanceMetaData instanceMetaData : result.getInstanceMetaData()) {
            assertEquals(result, instanceMetaData.getInstanceGroup());
        }
    }

}
