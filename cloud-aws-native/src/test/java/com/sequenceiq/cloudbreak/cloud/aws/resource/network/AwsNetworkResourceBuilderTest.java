package com.sequenceiq.cloudbreak.cloud.aws.resource.network;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AwsNetworkResourceBuilderTest {

    @InjectMocks
    private AwsNetworkResourceBuilder underTest;

    @Mock
    private AwsStackUtil awsStackUtil;

    @Mock
    private AwsContext awsContext;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private Network network;

    @Mock
    private Security security;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Test
    public void testBuildIfExists() throws Exception {
        CloudResource resource = CloudResource.builder().name("res").type(ResourceType.AWS_VPC).status(CommonStatus.CREATED).build();

        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(awsStackUtil.isExistingNetwork(amazonEc2Client, network)).thenReturn(true);

        CloudResource actual = underTest.build(awsContext, ac, network, security, resource);

        Assertions.assertEquals("res", actual.getName());
        verify(awsContext).putParameter(AwsNetworkResourceBuilder.VPC_NAME, "res");
    }
}
