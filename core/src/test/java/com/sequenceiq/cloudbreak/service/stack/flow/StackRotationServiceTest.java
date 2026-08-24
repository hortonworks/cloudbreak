package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.EMBEDDED_DB_SSL_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_3;
import static com.sequenceiq.cloudbreak.rotation.request.RotationSource.CLOUDBREAK;
import static com.sequenceiq.cloudbreak.rotation.request.RotationSource.FREEIPA;
import static com.sequenceiq.cloudbreak.rotation.request.RotationSource.REDBEAMS;
import static com.sequenceiq.cloudbreak.rotation.request.StepProgressCleanupStatus.FINISHED;
import static com.sequenceiq.cloudbreak.rotation.request.StepProgressCleanupStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.ConditionalRotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.request.StepProgressCleanupDescriptor;
import com.sequenceiq.cloudbreak.rotation.request.StepProgressResponse;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@ExtendWith(MockitoExtension.class)
public class StackRotationServiceTest {

    private static final String CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:878605d9-f9e9-44c6-9da6-e4bce9570ef5";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:envCrn1";

    private static final String DATABASE_CRN = "databaseCrn";

    @InjectMocks
    private StackRotationService underTest;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private SecretRotationStepProgressService stepProgressService;

    @Mock
    private SecretRotationValidationService secretRotationValidationService;

    @Mock
    private FreeIpaRotationV1Endpoint freeIpaRotationV1Endpoint;

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    private static StackIdView getStackIdView() {
        return new StackIdView() {

            @Override
            public Long getId() {
                return null;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getCrn() {
                return CRN;
            }
        };
    }

    @BeforeEach
    void setup() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "enabledSecretTypes", List.of(CloudbreakSecretType.values()), true);

        RotationContextProvider mockContextProvider = mock(RotationContextProvider.class);
        Map<CloudbreakSecretType, RotationContextProvider> contextProviderMap = Map.of(EXTERNAL_DATABASE_ROOT_PASSWORD, mockContextProvider);
        FieldUtils.writeField(underTest, "rotationContextProviderMap", contextProviderMap, true);
        lenient().when(mockContextProvider.getPollingTypes()).thenReturn(Map.of(REDBEAMS, TEST_2, FREEIPA, TEST_3));

        ConditionalRotationContextProvider conditionalRotationContextProvider = mock(ConditionalRotationContextProvider.class);
        Map<CloudbreakSecretType, ConditionalRotationContextProvider> conditionalRotationContextProviderMap =
                Map.of(EMBEDDED_DB_SSL_CERT, conditionalRotationContextProvider);
        FieldUtils.writeField(underTest, "conditionalRotationContextProviderMap", conditionalRotationContextProviderMap, true);
        lenient().when(conditionalRotationContextProvider.isApplicable(any())).thenReturn(Boolean.FALSE);
    }

    @Test
    void testRotationFiltering() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(anyString())).thenReturn(stackDto);
        when(secretRotationValidationService.validate(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(flowManager.triggerSecretRotation(anyLong(), anyString(), any(), any(), anyMap())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "flowchain"));

        assertThrows(BadRequestException.class, () ->
                underTest.rotateSecrets(CRN, List.of(EMBEDDED_DB_SSL_CERT.name()), null, Map.of()));

        underTest.rotateSecrets(CRN, List.of(EMBEDDED_DB_SSL_CERT.name(), EXTERNAL_DATABASE_ROOT_PASSWORD.name()), null, Map.of());

        ArgumentCaptor<List<SecretType>> secretTypesCaptor = ArgumentCaptor.forClass(List.class);
        verify(flowManager).triggerSecretRotation(anyLong(), anyString(), secretTypesCaptor.capture(), any(), anyMap());
        assertEquals(1, secretTypesCaptor.getValue().size());
        assertEquals(EXTERNAL_DATABASE_ROOT_PASSWORD, secretTypesCaptor.getValue().get(0));
    }

    @Test
    void testCleanupProgressWhenNoPollingProvided() {
        when(stepProgressService.delete(any(), any(), any())).thenReturn(StepProgressCleanupDescriptor.of(CLOUDBREAK, FINISHED, "crn", "secrettype"));

        List<StepProgressCleanupDescriptor> descriptors = underTest.cleanupProgress("crn", DatalakeSecretType.DBUS_UMS_ACCESS_KEY.value());

        assertEquals(1, descriptors.size());
        assertEquals(CLOUDBREAK, descriptors.get(0).rotationSource());
    }

    @Test
    void testCleanupProgress() {
        when(stepProgressService.delete(any(), any(), any())).thenReturn(StepProgressCleanupDescriptor.of(CLOUDBREAK, FINISHED, "crn", "secrettype"));
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getDatabaseServerCrn()).thenReturn("dbCrn");
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDtoService.getByCrn(any())).thenReturn(stackDto);

        List<StepProgressCleanupDescriptor> descriptors = underTest.cleanupProgress("crn", EXTERNAL_DATABASE_ROOT_PASSWORD.value());

        assertEquals(2, descriptors.size());
        assertEquals("dbCrn", descriptors.stream().filter(desc -> desc.rotationSource().equals(REDBEAMS)).findFirst().get().crn());
        assertEquals("crn", descriptors.stream().filter(desc -> desc.rotationSource().equals(CLOUDBREAK)).findFirst().get().crn());
        assertTrue(descriptors.stream().filter(desc -> !desc.rotationSource().equals(CLOUDBREAK))
                .allMatch(desc -> desc.status().equals(PENDING)));
    }

    @Test
    public void testRotateSecrets() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(anyString())).thenReturn(stackDto);
        when(secretRotationValidationService.validate(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(flowManager.triggerSecretRotation(anyLong(), anyString(), any(), any(), anyMap())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "flowchain"));

        underTest.rotateSecrets(CRN, List.of(EXTERNAL_DATABASE_ROOT_PASSWORD.name()), null, Map.of());

        verify(stackDtoService).getByCrn(eq(CRN));
        verify(secretRotationValidationService).validate(eq(CRN), eq(List.of(EXTERNAL_DATABASE_ROOT_PASSWORD)), eq(null), any());
        verify(secretRotationValidationService).validateEnabledSecretTypes(eq(List.of(EXTERNAL_DATABASE_ROOT_PASSWORD)), isNull());
        verify(flowManager).triggerSecretRotation(anyLong(), anyString(), any(), any(), anyMap());
    }

    @Test
    void getProgressResponseReturnsLocalProgressWhenFound() {
        StepProgressResponse expected = new StepProgressResponse();
        when(stepProgressService.getProgressResponse(eq(CRN), eq(EXTERNAL_DATABASE_ROOT_PASSWORD))).thenReturn(expected);

        StepProgressResponse result = underTest.getProgressResponse(CRN, EXTERNAL_DATABASE_ROOT_PASSWORD.value());

        assertEquals(expected, result);
        verify(freeIpaRotationV1Endpoint, never()).getProgress(anyString(), anyString());
        verify(databaseServerV4Endpoint, never()).getSecretRotationProgress(anyString(), anyString());
    }

    @Test
    void getProgressResponseFallsBackToFreeIpaWhenLocalNotFound() {
        when(stepProgressService.getProgressResponse(eq(CRN), eq(EXTERNAL_DATABASE_ROOT_PASSWORD)))
                .thenThrow(new NotFoundException("not found"));
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(eq(CRN))).thenReturn(stackDto);
        when(stackDto.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(stackDto.getCluster()).thenReturn(mock(ClusterView.class));
        StepProgressResponse expected = new StepProgressResponse();
        when(freeIpaRotationV1Endpoint.getProgress(eq("TEST_3"), eq(ENV_CRN))).thenReturn(expected);

        StepProgressResponse result = underTest.getProgressResponse(CRN, EXTERNAL_DATABASE_ROOT_PASSWORD.value());

        assertEquals(expected, result);
    }

    @Test
    void getProgressResponseFallsBackToRedbeamsWhenLocalNotFound() {
        when(stepProgressService.getProgressResponse(eq(CRN), eq(EXTERNAL_DATABASE_ROOT_PASSWORD)))
                .thenThrow(new NotFoundException("not found"));
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(eq(CRN))).thenReturn(stackDto);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getDatabaseServerCrn()).thenReturn(DATABASE_CRN);
        StepProgressResponse expected = new StepProgressResponse();
        when(databaseServerV4Endpoint.getSecretRotationProgress(eq("TEST_2"), eq(DATABASE_CRN))).thenReturn(expected);

        StepProgressResponse result = underTest.getProgressResponse(CRN, EXTERNAL_DATABASE_ROOT_PASSWORD.value());

        assertEquals(expected, result);
    }

    @Test
    void getProgressResponseThrowsNotFoundWhenNoUnderlyingServiceHasProgress() {
        when(stepProgressService.getProgressResponse(eq(CRN), eq(EXTERNAL_DATABASE_ROOT_PASSWORD)))
                .thenThrow(new NotFoundException("not found"));
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(eq(CRN))).thenReturn(stackDto);
        lenient().when(stackDto.getCluster()).thenReturn(mock(ClusterView.class));

        assertThrows(NotFoundException.class,
                () -> underTest.getProgressResponse(CRN, EXTERNAL_DATABASE_ROOT_PASSWORD.value()));
    }

    @Test
    void getProgressResponseThrowsOriginalNotFoundWhenStackDoesNotExist() {
        when(stepProgressService.getProgressResponse(eq(CRN), eq(EXTERNAL_DATABASE_ROOT_PASSWORD)))
                .thenThrow(new NotFoundException("not found"));
        when(stackDtoService.getByCrn(eq(CRN))).thenThrow(new NotFoundException("stack missing"));

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> underTest.getProgressResponse(CRN, EXTERNAL_DATABASE_ROOT_PASSWORD.value()));

        assertEquals("not found", thrown.getMessage());
        verify(freeIpaRotationV1Endpoint, never()).getProgress(anyString(), anyString());
        verify(databaseServerV4Endpoint, never()).getSecretRotationProgress(anyString(), anyString());
    }

    @Test
    void getProgressResponseThrowsNotFoundWhenSecretTypeHasNoRotationContextProvider() {
        when(stepProgressService.getProgressResponse(eq(CRN), eq(EMBEDDED_DB_SSL_CERT)))
                .thenThrow(new NotFoundException("not found"));

        assertThrows(NotFoundException.class,
                () -> underTest.getProgressResponse(CRN, EMBEDDED_DB_SSL_CERT.value()));

        verify(stackDtoService, never()).getByCrn(anyString());
        verify(freeIpaRotationV1Endpoint, never()).getProgress(anyString(), anyString());
        verify(databaseServerV4Endpoint, never()).getSecretRotationProgress(anyString(), anyString());
    }
}
