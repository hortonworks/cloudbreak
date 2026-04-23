package com.sequenceiq.freeipa.flow.stack.modify.tags.handler;

import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_FREEIPA_STACK_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.AwsConnector;
import com.sequenceiq.cloudbreak.cloud.aws.AwsNativeConnector;
import com.sequenceiq.cloudbreak.cloud.aws.AwsNativeResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConnector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConnector;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpConnector;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsCloudResourcesHandlerEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class ModifyUserDefinedTagsCloudResourcesHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String STACK_CRN = "crn";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("newKey", "newValue", "custom", "value2");

    @Mock
    private StackService stackService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private ResourceService resourceService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private AwsNativeConnector awsNativeConnector;

    @Mock
    private AwsNativeResourceConnector awsNativeResourceConnector;

    @Mock
    private AzureConnector azureConnector;

    @Mock
    private AzureResourceConnector azureResourceConnector;

    @Mock
    private GcpConnector gcpConnector;

    @Mock
    private GcpResourceConnector gcpResourceConnector;

    @Mock
    private AwsConnector awsConnector;

    @Mock
    private AwsResourceConnector awsResourceConnector;

    @Mock
    private CredentialService credentialService;

    private HandlerEvent<ModifyUserDefinedTagsCloudResourcesHandlerEvent> event;

    @InjectMocks
    private ModifyUserDefinedTagsCloudResourcesHandler underTest;

    @BeforeEach
    void setUp() {
        ModifyUserDefinedTagsCloudResourcesHandlerEvent request = new ModifyUserDefinedTagsCloudResourcesHandlerEvent(STACK_ID, "operationId",
                USER_DEFINED_TAGS);
        event = new HandlerEvent<>(new Event<>(request));
    }

    static Stream<Arguments> testDoAcceptSuccessDataProvider() {
        return Stream.of(
                Arguments.of("AWS_NATIVE"),
                Arguments.of("AZURE"),
                Arguments.of("GCP"),
                Arguments.of("AWS")
        );
    }

    @ParameterizedTest
    @MethodSource("testDoAcceptSuccessDataProvider")
    void testDoAcceptSuccess(String cloudPlatformVariant) {
        CloudConnector cloudConnector = switch (cloudPlatformVariant) {
            case "AWS_NATIVE" -> awsNativeConnector;
            case "AZURE" -> azureConnector;
            case "GCP" -> gcpConnector;
            case "AWS" -> awsConnector;
            default -> throw new IllegalStateException("Unexpected value: " + cloudPlatformVariant);
        };
        ResourceConnector resourceConnector = switch (cloudPlatformVariant) {
            case "AWS_NATIVE" -> awsNativeResourceConnector;
            case "AZURE" -> azureResourceConnector;
            case "GCP" -> gcpResourceConnector;
            case "AWS" -> awsResourceConnector;
            default -> throw new IllegalStateException("Unexpected value: " + cloudPlatformVariant);
        };
        Stack stack = new Stack();
        stack.setResourceCrn(STACK_CRN);

        Map<String, String> userDefinedTags = new HashMap<>(Map.of("custom", "value"));
        Map<String, String> applicationTags = new HashMap<>(Map.of("application", "app"));
        Map<String, String> defaultTags = new HashMap<>(Map.of("owner", "john doe", "creation-timestamp", "1773042126"));
        stack.setTags(new Json(Map.of("userDefinedTags", userDefinedTags, "applicationTags", applicationTags, "defaultTags", defaultTags)));

        CloudResource cloudResource1 = mock(CloudResource.class);
        CloudResource cloudResource2 = mock(CloudResource.class);
        Credential credential = mock(Credential.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        Authenticator authenticator = mock(Authenticator.class);

        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(List.of(cloudResource1, cloudResource2));
        when(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn())).thenReturn(credential);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(CloudContext.class), eq(cloudCredential))).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(ModifyUserDefinedTagsEvent.class, result);
        assertEquals(MODIFY_USER_DEFINED_TAGS_FREEIPA_STACK_EVENT.name(), result.getSelector());
        verify(resourceConnector).updateTags(authenticatedContext, List.of(cloudResource1, cloudResource2), USER_DEFINED_TAGS);
    }

    @Test
    void testDoAcceptFailure() {
        doThrow(new RuntimeException("error")).when(stackService).getStackById(STACK_ID);
        Selectable result = underTest.doAccept(event);

        assertInstanceOf(ModifyUserDefinedTagsFailedEvent.class, result);
        assertEquals(FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.name(), result.getSelector());
    }
}