package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleDescribeRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleDescribeResult;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class RollingVerticalScaleDescribeHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_ID = "i-abc123";

    @Mock
    private StackService stackService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CredentialService credentialService;

    @InjectMocks
    private RollingVerticalScaleDescribeHandler underTest;

    @Test
    void testInstanceNeedsScalingEmitsDescribeResult() {
        FreeIpaVerticalScaleParameters scaleConfig = new FreeIpaVerticalScaleParameters("master", "m5.2xlarge", "m5.xlarge", null);
        RollingVerticalScaleDescribeRequest request = new RollingVerticalScaleDescribeRequest(STACK_ID, INSTANCE_ID, scaleConfig);

        mockCloudProviderInstanceType("m5.xlarge");

        HandlerEvent<RollingVerticalScaleDescribeRequest> event = new HandlerEvent<>(new Event<>(request));
        Selectable result = underTest.doAccept(event);

        assertThat(result).isInstanceOf(RollingVerticalScaleDescribeResult.class);
        RollingVerticalScaleDescribeResult describeResult = (RollingVerticalScaleDescribeResult) result;
        assertThat(describeResult.getScaleConfig().getOriginalInstanceType()).isEqualTo("m5.xlarge");
        assertThat(describeResult.getScaleConfig().getTargetInstanceType()).isEqualTo("m5.2xlarge");
    }

    @Test
    void testInstanceAlreadyAtTargetEmitsSkipEvent() {
        FreeIpaVerticalScaleParameters scaleConfig = new FreeIpaVerticalScaleParameters("master", "m5.2xlarge", "m5.xlarge", null);
        RollingVerticalScaleDescribeRequest request = new RollingVerticalScaleDescribeRequest(STACK_ID, INSTANCE_ID, scaleConfig);

        mockCloudProviderInstanceType("m5.2xlarge");

        HandlerEvent<RollingVerticalScaleDescribeRequest> event = new HandlerEvent<>(new Event<>(request));
        Selectable result = underTest.doAccept(event);

        assertThat(result).isInstanceOf(StackEvent.class);
        assertThat(result.selector()).isEqualTo(FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_DESCRIBE_SKIP_EVENT.event());
    }

    @Test
    void testCloudDescribeFailureFallsBackToOriginalType() {
        FreeIpaVerticalScaleParameters scaleConfig = new FreeIpaVerticalScaleParameters("master", "m5.2xlarge", "m5.xlarge", null);
        RollingVerticalScaleDescribeRequest request = new RollingVerticalScaleDescribeRequest(STACK_ID, INSTANCE_ID, scaleConfig);

        // cloud describe not mocked → NPE caught internally → fallback to original type
        when(stackService.getStackById(STACK_ID)).thenReturn(mock(Stack.class));

        HandlerEvent<RollingVerticalScaleDescribeRequest> event = new HandlerEvent<>(new Event<>(request));
        Selectable result = underTest.doAccept(event);

        assertThat(result).isInstanceOf(RollingVerticalScaleDescribeResult.class);
        RollingVerticalScaleDescribeResult describeResult = (RollingVerticalScaleDescribeResult) result;
        assertThat(describeResult.getScaleConfig().getOriginalInstanceType()).isEqualTo("m5.xlarge");
    }

    private void mockCloudProviderInstanceType(String instanceType) {
        Stack stack = mock(Stack.class);
        when(stack.getRegion()).thenReturn("us-east-1");
        when(stack.getAvailabilityZone()).thenReturn("us-east-1a");
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getName()).thenReturn("test-stack");
        when(stack.getResourceCrn()).thenReturn("crn:test");
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getPlatformvariant()).thenReturn("AWS");
        when(stack.getOwner()).thenReturn("owner");
        when(stack.getAccountId()).thenReturn("account");
        when(stack.getEnvironmentCrn()).thenReturn("env-crn");
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);

        Credential credential = mock(Credential.class);
        when(credentialService.getCredentialByEnvCrn("env-crn")).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(mock(CloudCredential.class));

        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        Authenticator authenticator = mock(Authenticator.class);
        when(connector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(mock(AuthenticatedContext.class));
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        when(connector.metadata()).thenReturn(metadataCollector);
        when(metadataCollector.collectInstanceTypes(any(), anyList()))
                .thenReturn(new InstanceTypeMetadata(Map.of(INSTANCE_ID, instanceType)));
    }
}
