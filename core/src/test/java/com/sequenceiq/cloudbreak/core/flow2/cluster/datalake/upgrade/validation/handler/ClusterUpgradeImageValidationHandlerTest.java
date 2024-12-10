package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudPlatformValidationWarningException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.ClusterUpgradeImageValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeImageValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.service.parcel.ParcelAvailabilityService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.validation.ParcelSizeService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class ClusterUpgradeImageValidationHandlerTest {

    private static final String VALIDATION_EXCEPTION_MESSAGE = "Validation error happened.";

    private static final CloudPlatformVariant CLOUD_PLATFORM_VARIANT = new CloudPlatformVariant("TestPlatform", "TestVariant");

    private static final long REQUIRED_FREE_SPACE = 1024L;

    @InjectMocks
    private ClusterUpgradeImageValidationHandler underTest;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private ParcelAvailabilityService parcelAvailabilityService;

    @Mock
    private ParcelSizeService parcelSizeService;

    @Mock
    private StackDtoService stackDtoService;

    @Test
    void testDoAcceptWhenImageTermsAreSignedThenSuccess() {
        setupCloudContext();
        setupCloudConnector(cloudContext, cloudCredential);
        Validator imageValidator = new ValidatorBuilder(cloudConnector).withSucceedingImageValidator().build().get();
        HandlerEvent<ClusterUpgradeImageValidationEvent> event = getHandlerEvent();
        ClusterUpgradeImageValidationEvent request = event.getData();
        Set<Response> responses = Collections.emptySet();
        when(parcelAvailabilityService.validateAvailability(request.getTargetImage(), request.getResourceId())).thenReturn(responses);
        when(parcelSizeService.getRequiredFreeSpace(responses)).thenReturn(REQUIRED_FREE_SPACE);

        Selectable nextFlowStepSelector = underTest.doAccept(event);

        assertEquals(START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        assertEquals(REQUIRED_FREE_SPACE, ((ClusterUpgradeImageValidationFinishedEvent) nextFlowStepSelector).getRequiredFreeSpace());
        verify(parcelAvailabilityService).validateAvailability(request.getTargetImage(), request.getResourceId());
        verify(parcelSizeService).getRequiredFreeSpace(responses);
        verify(cloudContext).getPlatformVariant();
        verify(cloudPlatformConnectors).get(CLOUD_PLATFORM_VARIANT);
        verify(imageValidator).validate(eq(authenticatedContext), any(CloudStack.class));
    }

    @Test
    void testDoAcceptWhenValidatorWarning() {
        setupCloudContext();
        setupCloudConnector(cloudContext, cloudCredential);
        Validator imageValidator = new ValidatorBuilder(cloudConnector).withImageValidatorWarning().build().get();
        HandlerEvent<ClusterUpgradeImageValidationEvent> event = getHandlerEvent();
        ClusterUpgradeImageValidationEvent request = event.getData();
        Set<Response> responses = Collections.emptySet();
        when(parcelAvailabilityService.validateAvailability(request.getTargetImage(), request.getResourceId())).thenReturn(responses);
        when(parcelSizeService.getRequiredFreeSpace(responses)).thenReturn(REQUIRED_FREE_SPACE);

        ClusterUpgradeImageValidationFinishedEvent nextFlowStepSelector = (ClusterUpgradeImageValidationFinishedEvent) underTest.doAccept(event);

        assertTrue(nextFlowStepSelector.getWarningMessages().contains(VALIDATION_EXCEPTION_MESSAGE));
    }

    @Test
    void testDoAcceptShouldReturnWithFailureEventWhenTheTheParcelsAreNotAvailable() {
        HandlerEvent<ClusterUpgradeImageValidationEvent> event = getHandlerEvent();
        ClusterUpgradeImageValidationEvent request = event.getData();
        when(parcelAvailabilityService.validateAvailability(request.getTargetImage(), request.getResourceId()))
                .thenThrow(new UpgradeValidationFailedException("Failed to get parcels."));

        Selectable nextFlowStepSelector = underTest.doAccept(event);

        assertEquals(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        verify(parcelAvailabilityService).validateAvailability(request.getTargetImage(), request.getResourceId());
    }

    @Test
    void testDoAcceptWhenImageTermsAreNotSignedThenFailure() {
        setupCloudContext();
        setupCloudConnector(cloudContext, cloudCredential);
        Validator imageValidator = new ValidatorBuilder(cloudConnector).withFailingImageValidator().build().get();
        when(parcelAvailabilityService.validateAvailability(any(), any())).thenReturn(Collections.emptySet());
        when(parcelSizeService.getRequiredFreeSpace(any())).thenReturn(REQUIRED_FREE_SPACE);

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        verify(parcelAvailabilityService).validateAvailability(any(), any());
        verify(parcelSizeService).getRequiredFreeSpace(any());
        verify(cloudContext).getPlatformVariant();
        verify(cloudPlatformConnectors).get(CLOUD_PLATFORM_VARIANT);
        verify(imageValidator).validate(eq(authenticatedContext), any(CloudStack.class));
        ClusterUpgradeValidationFailureEvent failureEvent = (ClusterUpgradeValidationFailureEvent) nextFlowStepSelector;
        assertEquals(VALIDATION_EXCEPTION_MESSAGE, failureEvent.getException().getMessage());
    }

    @Test
    void testDoAcceptWhenNoImageValidatorIsPresentThenSuccess() {
        setupCloudContext();
        setupCloudConnector(cloudContext, cloudCredential);
        new ValidatorBuilder(cloudConnector).withNoImageValidator();
        when(parcelAvailabilityService.validateAvailability(any(), any())).thenReturn(Collections.emptySet());
        when(parcelSizeService.getRequiredFreeSpace(any())).thenReturn(REQUIRED_FREE_SPACE);

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        verify(parcelAvailabilityService).validateAvailability(any(), any());
        verify(parcelSizeService).getRequiredFreeSpace(any());
        verify(cloudContext).getPlatformVariant();
        verify(cloudPlatformConnectors).get(CLOUD_PLATFORM_VARIANT);
    }

    private void setupCloudContext() {
        when(cloudContext.getPlatformVariant()).thenReturn(CLOUD_PLATFORM_VARIANT);
    }

    private HandlerEvent<ClusterUpgradeImageValidationEvent> getHandlerEvent() {
        ClusterUpgradeImageValidationEvent clusterUpgradeImageValidationEvent =
                new ClusterUpgradeImageValidationEvent(1L, "imageId", cloudStack, cloudCredential, cloudContext, mock(Image.class));
        HandlerEvent<ClusterUpgradeImageValidationEvent> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(clusterUpgradeImageValidationEvent);
        return handlerEvent;
    }

    private void setupCloudConnector(CloudContext cloudContext, CloudCredential cloudCredential) {
        cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(cloudPlatformConnectors.get(CLOUD_PLATFORM_VARIANT)).thenReturn(cloudConnector);
    }

    private static class ValidatorBuilder {

        private final CloudConnector cloudConnector;

        private Optional<Validator> imageValidatorOptional = Optional.empty();

        ValidatorBuilder(CloudConnector cloudConnector) {
            this.cloudConnector = cloudConnector;
        }

        ValidatorBuilder withFailingImageValidator() {
            setupImageValidator(true);
            return this;
        }

        ValidatorBuilder withSucceedingImageValidator() {
            setupImageValidator(false);
            return this;
        }

        ValidatorBuilder withNoImageValidator() {
            when(cloudConnector.validators(ValidatorType.IMAGE)).thenReturn(List.of());
            imageValidatorOptional = Optional.empty();
            return this;
        }

        ValidatorBuilder withImageValidatorWarning() {
            setupImageValidatorWithWarningException();
            return this;
        }

        Optional<Validator> build() {
            return imageValidatorOptional;
        }

        private void setupImageValidator(boolean failValidation) {
            Validator imageValidator = mock(Validator.class);
            imageValidatorOptional = Optional.of(imageValidator);
            when(cloudConnector.validators(ValidatorType.IMAGE)).thenReturn(List.of(imageValidator));
            if (failValidation) {
                doThrow(new CloudConnectorException(VALIDATION_EXCEPTION_MESSAGE)).when(imageValidator).validate(any(), any());
            }
        }

        private void setupImageValidatorWithWarningException() {
            Validator imageValidator = mock(Validator.class);
            imageValidatorOptional = Optional.of(imageValidator);
            when(cloudConnector.validators(ValidatorType.IMAGE)).thenReturn(List.of(imageValidator));
            doThrow(new CloudPlatformValidationWarningException(VALIDATION_EXCEPTION_MESSAGE)).when(imageValidator).validate(any(), any());
        }
    }
}
