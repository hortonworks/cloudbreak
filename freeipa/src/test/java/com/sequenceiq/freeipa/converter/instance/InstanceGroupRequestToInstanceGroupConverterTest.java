package com.sequenceiq.freeipa.converter.instance;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.converter.instance.template.InstanceTemplateRequestToTemplateConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupRequestToInstanceGroupConverterTest {

    private static final String ACCOUNT_ID = "account_id";

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
        InstanceGroup result = underTest.convert(request, ACCOUNT_ID, MOCK.name());
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getGroupName()).isEqualTo(NAME);
        assertThat(result.getInstanceGroupType()).isEqualTo(InstanceGroupType.MASTER);
        assertThat(result.getSecurityGroup()).isEqualTo(securityGroup);
        assertThat(result.getNodeCount()).isEqualTo(nodeCount);
        assertThat(result.getInstanceMetaData().size()).isEqualTo(nodeCount);
        for (InstanceMetaData instanceMetaData : result.getInstanceMetaData()) {
            assertThat(instanceMetaData.getInstanceGroup()).isEqualTo(result);
        }
    }

    @Test
    void convertTestTemplateConversion() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        InstanceTemplateRequest instanceTemplateRequest = mock(InstanceTemplateRequest.class);
        request.setInstanceTemplateRequest(instanceTemplateRequest);
        Template template = mock(Template.class);
        when(templateConverter.convert(instanceTemplateRequest, MOCK, ACCOUNT_ID)).thenReturn(template);

        InstanceGroup result = underTest.convert(request, ACCOUNT_ID, MOCK.name());

        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);
    }

}
