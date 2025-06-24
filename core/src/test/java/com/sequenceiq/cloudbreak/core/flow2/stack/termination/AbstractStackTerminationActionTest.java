package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalResourceAttributes;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.flow.core.FlowParameters;

@ExtendWith(MockitoExtension.class)
class AbstractStackTerminationActionTest {

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "stack-name";

    private static final String STACK_ORIGINAL_NAME = "stack-original-name";

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:1234:cluster:5678";

    private static final String STACK_CLOUD_PLATFORM = "AWS";

    private static final String STACK_PLATFORM_VARIANT = "AWS_NATIVE";

    private static final String STACK_REGION = "us-west-1";

    private static final String STACK_AVAILABILITY_ZONE = "us-west-1a";

    private static final Long TENANT_ID = 3L;

    private static final String ENV_CRN = "env-crn";

    @InjectMocks
    private TestStackTerminationAction underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private StackUtil stackUtil;

    @ParameterizedTest(name = "{index}: expectedResourceCount={1}")
    @MethodSource("createFlowContextTestDataProvider")
    void testCreateFlowContext(CloudResource cloudResource, int expectedResourceCount) {
        FlowParameters flowParameters = new FlowParameters("flowId", "flowTriggerUserCrn");
        StateContext<StackTerminationState, StackTerminationEvent> stateContext = mock(StateContext.class);
        ExtendedState extendedState = mock(ExtendedState.class);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(Map.of(AbstractStackTerminationAction.TERMINATION_TYPE, TerminationType.REGULAR));
        Payload payload = () -> STACK_ID;
        StackDto stack = getStackDto();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(stackUtil.getCloudCredential(ENV_CRN)).thenReturn(cloudCredential);
        CloudStack cloudStack = mock(CloudStack.class);
        when(cloudStackConverter.convert(stack)).thenReturn(cloudStack);
        Resource resource = mock(Resource.class);
        when(resourceService.getAllByStackId(STACK_ID)).thenReturn(List.of(resource));
        when(cloudResourceConverter.convert(resource)).thenReturn(cloudResource);

        StackTerminationContext result = underTest.createFlowContext(flowParameters, stateContext, payload);

        assertEquals(flowParameters, result.getFlowParameters());
        assertEquals(stack, result.getStack());
        CloudContext cloudContext = result.getCloudContext();
        assertEquals(STACK_ID, cloudContext.getId());
        assertEquals(STACK_NAME, cloudContext.getName());
        assertEquals(STACK_ORIGINAL_NAME, cloudContext.getOriginalName());
        assertEquals(STACK_CRN, cloudContext.getCrn());
        assertEquals(STACK_CLOUD_PLATFORM, cloudContext.getPlatform().value());
        assertEquals(STACK_PLATFORM_VARIANT, cloudContext.getVariant().value());
        assertEquals(STACK_REGION, cloudContext.getLocation().getRegion().getRegionName());
        assertEquals(STACK_AVAILABILITY_ZONE, cloudContext.getLocation().getAvailabilityZone().value());
        assertEquals(Crn.safeFromString(STACK_CRN).getAccountId(), cloudContext.getAccountId());
        assertEquals(TENANT_ID, cloudContext.getTenantId());
        assertEquals(cloudCredential, result.getCloudCredential());
        assertEquals(cloudStack, result.getCloudStack());
        assertEquals(expectedResourceCount, result.getCloudResources().size());
        assertEquals(TerminationType.REGULAR, result.getTerminationType());
    }

    private StackDto getStackDto() {
        StackDto stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(stack.getOriginalName()).thenReturn(STACK_ORIGINAL_NAME);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(stack.getCloudPlatform()).thenReturn(STACK_CLOUD_PLATFORM);
        when(stack.getPlatformVariant()).thenReturn(STACK_PLATFORM_VARIANT);
        when(stack.getRegion()).thenReturn(STACK_REGION);
        when(stack.getAvailabilityZone()).thenReturn(STACK_AVAILABILITY_ZONE);
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        when(stack.getTenant()).thenReturn(tenant);
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        return stack;
    }

    private static Stream<Arguments> createFlowContextTestDataProvider() {
        CloudResource cloudResourceWithAttributes = mock(CloudResource.class);
        when(cloudResourceWithAttributes.getParameterStrict(CloudResource.ATTRIBUTES, ExternalResourceAttributes.class))
                .thenReturn(mock(ExternalResourceAttributes.class));
        CloudResource cloudResourceWithoutAttributes = mock(CloudResource.class);
        when(cloudResourceWithoutAttributes.getParameterStrict(CloudResource.ATTRIBUTES, ExternalResourceAttributes.class)).thenReturn(null);
        return Stream.of(
                Arguments.of(cloudResourceWithAttributes, 0),
                Arguments.of(cloudResourceWithoutAttributes, 1)
        );
    }

    private static class TestStackTerminationAction extends AbstractStackTerminationAction<Payload> {
        protected TestStackTerminationAction() {
            super(Payload.class);
        }

        @Override
        protected void doExecute(StackTerminationContext context, Payload payload, Map<Object, Object> variables) {
        }
    }
}
