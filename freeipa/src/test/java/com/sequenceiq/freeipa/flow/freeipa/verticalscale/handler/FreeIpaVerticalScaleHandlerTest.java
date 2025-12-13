package com.sequenceiq.freeipa.flow.freeipa.verticalscale.handler;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaVerticalScaleHandlerTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EventBus eventBus;

    @Mock
    private FreeIpaVerticalScaleService verticalScale;

    @InjectMocks
    private FreeIpaVerticalScaleHandler underTest;

    @Test
    void testType() {
        assertEquals(com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleRequest.class, underTest.type());
    }

    @Test
    void testAcceptWhenEverythingIsFine() throws Exception {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("region")))
                .withAccountId("account")
                .build();

        CloudCredential credential = new CloudCredential("id", "alma", Map.of("accessKey", "ac", "secretKey", "secret"), "acc");

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = Group.builder().build();
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = CloudStack.builder()
                .groups(singletonList(group1))
                .network(network)
                .instanceAuthentication(instanceAuthentication)
                .build();

        VerticalScaleRequest freeIPAVerticalScaleRequest = new VerticalScaleRequest();
        freeIPAVerticalScaleRequest.setGroup("master");
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType("ec2big");
        freeIPAVerticalScaleRequest.setTemplate(instanceTemplateRequest);

        com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleRequest freeIpaVerticalScaleRequest
                = new com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleRequest(
                context,
                credential,
                cloudStack,
                List.of(),
                freeIPAVerticalScaleRequest);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);

        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(verticalScale.verticalScale(
                any(AuthenticatedContext.class),
                any(com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleRequest.class),
                any(CloudConnector.class))
        ).thenReturn(List.of());

        underTest.accept(new Event<>(freeIpaVerticalScaleRequest));
    }

    @Test
    void testAcceptWhenErrorOccurs() throws Exception {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("region")))
                .withAccountId("account")
                .build();

        CloudCredential credential = new CloudCredential("id", "alma", Map.of("accessKey", "ac", "secretKey", "secret"), "acc");

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = Group.builder().build();
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = CloudStack.builder()
                .groups(singletonList(group1))
                .network(network)
                .instanceAuthentication(instanceAuthentication)
                .build();

        VerticalScaleRequest freeIPAVerticalScaleRequest = new VerticalScaleRequest();
        freeIPAVerticalScaleRequest.setGroup("master");
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType("ec2big");
        freeIPAVerticalScaleRequest.setTemplate(instanceTemplateRequest);

        com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleRequest freeIpaVerticalScaleRequest
                = new com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleRequest(
                context,
                credential,
                cloudStack,
                List.of(),
                freeIPAVerticalScaleRequest);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        EventBus eventBus = mock(EventBus.class);

        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(verticalScale.verticalScale(
                any(AuthenticatedContext.class),
                any(com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleRequest.class),
                any(CloudConnector.class))
        ).thenThrow(new RuntimeException());
        verify(eventBus, times(0)).notify(any(), any(Event.class));

        underTest.accept(new Event<>(freeIpaVerticalScaleRequest));
    }
}