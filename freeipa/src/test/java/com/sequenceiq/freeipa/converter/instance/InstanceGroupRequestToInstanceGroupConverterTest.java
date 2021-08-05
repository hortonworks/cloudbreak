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

import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.converter.instance.template.InstanceTemplateRequestToTemplateConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupRequestToInstanceGroupConverterTest {

    private static final String ACCOUNT_ID = "account_id";

    private static final String NAME = "NAME";

    private static final String HOSTNAME = "HOSTNAME";

    private static final String DOMAINNAME = "DOMAINNAME";

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

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(MOCK.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        // GIVEN
        given(defaultInstanceGroupProvider.createDefaultTemplate(eq(MOCK), eq(ACCOUNT_ID), eq(null))).willReturn(template);
        given(securityGroupConverter.convert(eq(securityGroupRequest))).willReturn(securityGroup);
        // WHEN
        InstanceGroup result = underTest.convert(request, networkRequest, ACCOUNT_ID, stack, freeIpaServerRequest,
                detailedEnvironmentResponse, null);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getGroupName()).isEqualTo(NAME);
        assertThat(result.getInstanceGroupType()).isEqualTo(InstanceGroupType.MASTER);
        assertThat(result.getSecurityGroup()).isEqualTo(securityGroup);
        assertThat(result.getNodeCount()).isEqualTo(nodeCount);
        assertThat(result.getInstanceMetaData().size()).isEqualTo(nodeCount);
        int i = 0;
        for (InstanceMetaData instanceMetaData : result.getInstanceMetaData()) {
            assertThat(instanceMetaData.getInstanceGroup()).isEqualTo(result);
            assertThat(instanceMetaData.getDiscoveryFQDN()).startsWith(HOSTNAME + i);
            i++;
        }
    }

    @Test
    void convertTestTemplateConversion() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(MOCK.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        InstanceTemplateRequest instanceTemplateRequest = mock(InstanceTemplateRequest.class);
        request.setInstanceTemplateRequest(instanceTemplateRequest);
        Template template = mock(Template.class);
        when(templateConverter.convert(instanceTemplateRequest, MOCK, ACCOUNT_ID, null)).thenReturn(template);

        InstanceGroup result = underTest.convert(
                request,
                networkRequest,
                ACCOUNT_ID,
                stack,
                freeIpaServerRequest,
                detailedEnvironmentResponse,
                null);
        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);
    }

    @Test
    void convertTestTemplateConversionWithDiskEncryptionSetId() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(MOCK.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        InstanceTemplateRequest instanceTemplateRequest = mock(InstanceTemplateRequest.class);
        request.setInstanceTemplateRequest(instanceTemplateRequest);
        Template template = mock(Template.class);
        when(templateConverter.convert(instanceTemplateRequest, MOCK, ACCOUNT_ID, null)).thenReturn(template);

        InstanceGroup result = underTest.convert(
                request,
                networkRequest,
                ACCOUNT_ID,
                stack,
                freeIpaServerRequest,
                detailedEnvironmentResponse,
                null);

        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);
    }

    @Test
    void convertTestTemplateConversionWithEncryptionKey() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        InstanceTemplateRequest instanceTemplateRequest = mock(InstanceTemplateRequest.class);
        request.setInstanceTemplateRequest(instanceTemplateRequest);
        Template template = mock(Template.class);

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(MOCK.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(templateConverter.convert(instanceTemplateRequest, MOCK, ACCOUNT_ID, "dummyEncryptionKey")).thenReturn(template);

        InstanceGroup result = underTest.convert(request,
                networkRequest,
                ACCOUNT_ID,
                stack,
                freeIpaServerRequest,
                detailedEnvironmentResponse,
                "dummyEncryptionKey");

        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);
    }

}
