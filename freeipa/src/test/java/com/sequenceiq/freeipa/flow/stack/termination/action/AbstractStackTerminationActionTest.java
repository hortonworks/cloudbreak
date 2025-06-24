package com.sequenceiq.freeipa.flow.stack.termination.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalResourceAttributes;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class AbstractStackTerminationActionTest {

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "stack-name";

    private static final String STACK_CRN = "crn:cdp:freeipa:us-west-1:1234:environment:5678";

    private static final String STACK_CLOUD_PLATFORM = "AWS";

    private static final String STACK_PLATFORM_VARIANT = "AWS_NATIVE";

    private static final String STACK_REGION = "us-west-1";

    private static final String STACK_AVAILABILITY_ZONE = "us-west-1a";

    private static final String STACK_OWNER = "owner";

    private static final String STACK_ACCOUNT_ID = "1234";

    private static final String ENV_CRN = "env-crn";

    @InjectMocks
    private TestStackTerminationAction underTest;

    @Mock
    private StackService stackService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private ResourceToCloudResourceConverter resourceConverter;

    @Mock
    private ResourceService resourceService;


    @ParameterizedTest
    @MethodSource("createFlowContextTestDataProvider")
    void testCreateFlowContext(CloudResource cloudResource, int expectedResourceCount) {
        FlowParameters flowParameters = new FlowParameters("flowId", "flowTriggerUserCrn");
        StackEvent payload = new StackEvent(STACK_ID);
        Stack stack = getStack();
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        Credential credential = mock(Credential.class);
        when(credentialService.getCredentialByEnvCrn(ENV_CRN)).thenReturn(credential);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        CloudStack cloudStack = mock(CloudStack.class);
        when(cloudStackConverter.convert(stack)).thenReturn(cloudStack);
        Resource resource = mock(Resource.class);
        when(resourceService.findAllByStackId(STACK_ID)).thenReturn(List.of(resource));
        when(resourceConverter.convert(resource)).thenReturn(cloudResource);

        StackTerminationContext result = underTest.createFlowContext(flowParameters, null, payload);

        assertEquals(flowParameters, result.getFlowParameters());
        assertEquals(stack, result.getStack());
        CloudContext cloudContext = result.getCloudContext();
        assertEquals(STACK_ID, cloudContext.getId());
        assertEquals(STACK_NAME, cloudContext.getName());
        assertEquals(STACK_CRN, cloudContext.getCrn());
        assertEquals(STACK_CLOUD_PLATFORM, cloudContext.getPlatform().value());
        assertEquals(STACK_PLATFORM_VARIANT, cloudContext.getVariant().value());
        assertEquals(STACK_REGION, cloudContext.getLocation().getRegion().getRegionName());
        assertEquals(STACK_AVAILABILITY_ZONE, cloudContext.getLocation().getAvailabilityZone().value());
        assertEquals(STACK_OWNER, cloudContext.getUserName());
        assertEquals(STACK_ACCOUNT_ID, cloudContext.getAccountId());
        assertEquals(cloudCredential, result.getCloudCredential());
        assertEquals(cloudStack, result.getCloudStack());
        assertEquals(expectedResourceCount, result.getCloudResources().size());
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        stack.setResourceCrn(STACK_CRN);
        stack.setCloudPlatform(STACK_CLOUD_PLATFORM);
        stack.setPlatformvariant(STACK_PLATFORM_VARIANT);
        stack.setRegion(STACK_REGION);
        stack.setAvailabilityZone(STACK_AVAILABILITY_ZONE);
        stack.setOwner(STACK_OWNER);
        stack.setAccountId(STACK_ACCOUNT_ID);
        stack.setEnvironmentCrn(ENV_CRN);
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

    private static class TestStackTerminationAction extends AbstractStackTerminationAction<StackEvent> {
        protected TestStackTerminationAction() {
            super(StackEvent.class);
        }

        @Override
        protected void doExecute(StackTerminationContext context, StackEvent payload, java.util.Map<Object, Object> variables) {
        }
    }
}
