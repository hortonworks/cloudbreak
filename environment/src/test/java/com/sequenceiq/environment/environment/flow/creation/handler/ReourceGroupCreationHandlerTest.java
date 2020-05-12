package com.sequenceiq.environment.environment.flow.creation.handler;


import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.prerequisite.AzurePrerequisiteCreateRequest;
import com.sequenceiq.cloudbreak.cloud.model.prerequisite.EnvironmentPrerequisitesCreateRequest;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.featureswitch.AzureSingleResourceGroupFeatureSwitch;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern;
import com.sequenceiq.environment.parameters.dto.AzureParametersDto;
import com.sequenceiq.environment.parameters.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class ReourceGroupCreationHandlerTest {

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final long ENVIRONMENT_ID = 1L;

    private static final String START_NETWORK_CREATION_EVENT_SELECTOR = "START_NETWORK_CREATION_EVENT";

    private static final String REGION_NAME = "regionName";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private AzureSingleResourceGroupFeatureSwitch azureSingleResourceGroupFeatureSwitch;

    @Mock
    private CostTagging costTagging;

    @Mock
    private EventBus eventBus;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ParametersService parameterService;

    @Mock
    private Clock clock;

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private ResourceGroupCreationHandler underTest;

    @Mock
    private Setup setup;

    @Test
    void testWhenEnvironmentNotFound() {
        Event<EnvironmentDto> event = getEnvironmentDtoEvent(null);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());
        ArgumentCaptor<Event> failureEventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

        underTest.accept(event);

        verifyFailureIsInvoked(failureEventArgumentCaptor);
        verify(parameterService, never()).updateResourceGroupName(any(), anyString());
        verify(setup, never()).createEnvironmentPrerequisites(any());
    }

    @Test
    void testWhenFeatureSwitchNotActive() {
        Event<EnvironmentDto> event = getEnvironmentDtoEvent(null);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(getEnvironment()));
        when(azureSingleResourceGroupFeatureSwitch.isActive()).thenReturn(false);
        ArgumentCaptor<BaseNamedFlowEvent> envCreationEventArgumentCaptor = ArgumentCaptor.forClass(BaseNamedFlowEvent.class);

        underTest.accept(event);

        verify(eventBus, never()).notify(anyString(), any(Event.class));
        verify(azureSingleResourceGroupFeatureSwitch).isActive();
        verifyNextFlowStepIsInvoked(envCreationEventArgumentCaptor);
        verify(parameterService, never()).updateResourceGroupName(any(), anyString());
        verify(setup, never()).createEnvironmentPrerequisites(any());
    }

    @Test
    void testWhenNoResourceGroupDto() {
        Event<EnvironmentDto> event = getEnvironmentDtoEvent(null);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(getEnvironment()));
        when(azureSingleResourceGroupFeatureSwitch.isActive()).thenReturn(true);
        ArgumentCaptor<BaseNamedFlowEvent> envCreationEventArgumentCaptor = ArgumentCaptor.forClass(BaseNamedFlowEvent.class);

        underTest.accept(event);

        verify(eventBus, never()).notify(anyString(), any(Event.class));
        verify(azureSingleResourceGroupFeatureSwitch).isActive();
        verifyNextFlowStepIsInvoked(envCreationEventArgumentCaptor);
        verify(parameterService, never()).updateResourceGroupName(any(), anyString());
        verify(setup, never()).createEnvironmentPrerequisites(any());
    }

    @Test
    void testWhenNotAzure() {
        Event<EnvironmentDto> event = getEnvironmentDtoEvent(null);
        event.getData().setCloudPlatform(AWS.name());
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(getEnvironment()));
        ArgumentCaptor<BaseNamedFlowEvent> envCreationEventArgumentCaptor = ArgumentCaptor.forClass(BaseNamedFlowEvent.class);

        underTest.accept(event);

        verify(eventBus, never()).notify(anyString(), any(Event.class));
        verifyNextFlowStepIsInvoked(envCreationEventArgumentCaptor);
        verify(parameterService, never()).updateResourceGroupName(any(), anyString());
        verify(setup, never()).createEnvironmentPrerequisites(any());
    }

    @Test
    void testWhenResourceGroupDtoUseMultiple() {
        Event<EnvironmentDto> event = getEnvironmentDtoEvent(
                AzureResourceGroupDto.builder()
                        .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE)
                        .build()
        );
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(getEnvironment()));
        when(azureSingleResourceGroupFeatureSwitch.isActive()).thenReturn(true);
        ArgumentCaptor<BaseNamedFlowEvent> envCreationEventArgumentCaptor = ArgumentCaptor.forClass(BaseNamedFlowEvent.class);

        underTest.accept(event);

        verify(eventBus, never()).notify(anyString(), any(Event.class));
        verify(azureSingleResourceGroupFeatureSwitch).isActive();
        verifyNextFlowStepIsInvoked(envCreationEventArgumentCaptor);
        verify(parameterService, never()).updateResourceGroupName(any(), anyString());
        verify(setup, never()).createEnvironmentPrerequisites(any());
    }

    @Test
    void testWhenResourceGroupDtoUseExisting() {
        Event<EnvironmentDto> event = getEnvironmentDtoEvent(
                AzureResourceGroupDto.builder()
                        .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                        .withResourceGroupCreation(ResourceGroupCreation.USE_EXISTING)
                        .build()
        );
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(getEnvironment()));
        when(azureSingleResourceGroupFeatureSwitch.isActive()).thenReturn(true);
        ArgumentCaptor<BaseNamedFlowEvent> envCreationEventArgumentCaptor = ArgumentCaptor.forClass(BaseNamedFlowEvent.class);

        underTest.accept(event);

        verify(eventBus, never()).notify(anyString(), any(Event.class));
        verify(azureSingleResourceGroupFeatureSwitch).isActive();
        verifyNextFlowStepIsInvoked(envCreationEventArgumentCaptor);
        verify(parameterService, never()).updateResourceGroupName(any(), anyString());
    }

    @Test
    void testWhenResourceGroupDtoCreateNew() {
        Event<EnvironmentDto> event = getEnvironmentDtoEvent(
                AzureResourceGroupDto.builder()
                        .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                        .withResourceGroupCreation(ResourceGroupCreation.CREATE_NEW)
                        .build()
        );
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(getEnvironment()));
        when(azureSingleResourceGroupFeatureSwitch.isActive()).thenReturn(true);
        ArgumentCaptor<BaseNamedFlowEvent> envCreationEventArgumentCaptor = ArgumentCaptor.forClass(BaseNamedFlowEvent.class);
        ArgumentCaptor<EnvironmentPrerequisitesCreateRequest> environmentPrerequisitesCreateRequestArgumentCaptor
                = ArgumentCaptor.forClass(EnvironmentPrerequisitesCreateRequest.class);
        when(costTagging.mergeTags(any())).thenReturn(Map.of());
        prepareCloudConnectorSetup();

        underTest.accept(event);

        verify(eventBus, never()).notify(anyString(), any(Event.class));
        verify(azureSingleResourceGroupFeatureSwitch).isActive();
        verifyNextFlowStepIsInvoked(envCreationEventArgumentCaptor);
        verifyResourceGroupCreateIsInvoked(environmentPrerequisitesCreateRequestArgumentCaptor);
        verify(clock).getCurrentTimeMillis();
        verify(parameterService).updateResourceGroupName(any(), anyString());
        verify(setup).createEnvironmentPrerequisites(any());
    }

    private void verifyFailureIsInvoked(ArgumentCaptor<Event> failureEventArgumentCaptor) {
        verify(eventBus).notify(anyString(), failureEventArgumentCaptor.capture());
        Event<EnvCreationFailureEvent> envCreationFailureEvent = failureEventArgumentCaptor.getValue();
        assertEquals(ENVIRONMENT_NAME, envCreationFailureEvent.getData().getResourceName());
        assertEquals(ENVIRONMENT_CRN, envCreationFailureEvent.getData().getResourceCrn());
    }

    private void verifyResourceGroupCreateIsInvoked(ArgumentCaptor<EnvironmentPrerequisitesCreateRequest> environmentPrerequisitesCreateRequestArgumentCaptor) {
        verify(setup).createEnvironmentPrerequisites(environmentPrerequisitesCreateRequestArgumentCaptor.capture());
        AzurePrerequisiteCreateRequest azurePrerequisiteCreateRequest = environmentPrerequisitesCreateRequestArgumentCaptor.getValue().getAzure();
        assertEquals(REGION_NAME, azurePrerequisiteCreateRequest.getLocationName());
        assertEquals(ENVIRONMENT_NAME + "_0", azurePrerequisiteCreateRequest.getResourceGroupName());
    }

    private void verifyNextFlowStepIsInvoked(ArgumentCaptor<BaseNamedFlowEvent> envCreationEventArgumentCaptor) {
        verify(eventSender).sendEvent(envCreationEventArgumentCaptor.capture(), any());
        assertEquals(START_NETWORK_CREATION_EVENT_SELECTOR, envCreationEventArgumentCaptor.getValue().selector());
        assertEquals(ENVIRONMENT_NAME, envCreationEventArgumentCaptor.getValue().getResourceName());
        assertEquals(ENVIRONMENT_CRN, envCreationEventArgumentCaptor.getValue().getResourceCrn());
    }

    private Event<EnvironmentDto> getEnvironmentDtoEvent(AzureResourceGroupDto azureResourceGroupDto) {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName(ENVIRONMENT_NAME);
        environmentDto.setResourceCrn(ENVIRONMENT_CRN);
        environmentDto.setId(ENVIRONMENT_ID);
        environmentDto.setCloudPlatform(AZURE.name());
        environmentDto.setLocation(LocationDto.builder().withName(REGION_NAME).build());
        environmentDto.setTags(new EnvironmentTags(Map.of("envTag1Key", "envTag1Value"), Map.of("DefaultTag1Key", "DefaultTag1Value")));
        environmentDto.setParameters(ParametersDto.builder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(azureResourceGroupDto)
                        .build())
                .build());
        return new Event<>(environmentDto);
    }

    private void prepareCloudConnectorSetup() {
        CloudConnector<Object> cloudConnector = mock(CloudConnector.class);
        when(cloudConnector.setup()).thenReturn(setup);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
    }

    private Environment getEnvironment() {
        Environment environment = new Environment();
        environment.setId(ENVIRONMENT_ID);
        return environment;
    }
}
