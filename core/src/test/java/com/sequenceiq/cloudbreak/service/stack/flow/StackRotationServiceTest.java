package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SSSD_IPA_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_3;
import static com.sequenceiq.cloudbreak.rotation.request.RotationSource.CLOUDBREAK;
import static com.sequenceiq.cloudbreak.rotation.request.RotationSource.FREEIPA;
import static com.sequenceiq.cloudbreak.rotation.request.RotationSource.REDBEAMS;
import static com.sequenceiq.cloudbreak.rotation.request.StepProgressCleanupStatus.FINISHED;
import static com.sequenceiq.cloudbreak.rotation.request.StepProgressCleanupStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.request.StepProgressCleanupDescriptor;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@ExtendWith(MockitoExtension.class)
public class StackRotationServiceTest {

    private static final String CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:878605d9-f9e9-44c6-9da6-e4bce9570ef5";

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
        Map<CloudbreakSecretType, RotationContextProvider> contextProviderMap = Map.of(SSSD_IPA_PASSWORD, mockContextProvider);
        FieldUtils.writeField(underTest, "rotationContextProviderMap", contextProviderMap, true);
        lenient().when(mockContextProvider.getPollingTypes()).thenReturn(Map.of(REDBEAMS, TEST_2, FREEIPA, TEST_3));
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
        when(stackDto.getEnvironmentCrn()).thenReturn("envCrn");
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getDatabaseServerCrn()).thenReturn("dbCrn");
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDtoService.getByCrn(any())).thenReturn(stackDto);

        List<StepProgressCleanupDescriptor> descriptors = underTest.cleanupProgress("crn", SSSD_IPA_PASSWORD.value());

        assertEquals(3, descriptors.size());
        assertEquals("envCrn", descriptors.stream().filter(desc -> desc.rotationSource().equals(FREEIPA)).findFirst().get().crn());
        assertEquals("dbCrn", descriptors.stream().filter(desc -> desc.rotationSource().equals(REDBEAMS)).findFirst().get().crn());
        assertEquals("crn", descriptors.stream().filter(desc -> desc.rotationSource().equals(CLOUDBREAK)).findFirst().get().crn());
        assertTrue(descriptors.stream().filter(desc -> !desc.rotationSource().equals(CLOUDBREAK))
                .allMatch(desc -> desc.status().equals(PENDING)));
    }

    @Test
    public void testRotateSecrets() {
        Stack stack = new Stack();
        stack.setId(1L);
        when(stackDtoService.getStackViewByCrn(anyString())).thenReturn(stack);
        when(secretRotationValidationService.validate(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(flowManager.triggerSecretRotation(anyLong(), anyString(), any(), any(), anyMap())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, "flowchain"));

        underTest.rotateSecrets(CRN, List.of(CM_ADMIN_PASSWORD.name()), null, Map.of());

        verify(stackDtoService).getStackViewByCrn(eq(CRN));
        verify(secretRotationValidationService).validate(eq(CRN), eq(List.of(CM_ADMIN_PASSWORD)), eq(null), any());
        verify(secretRotationValidationService).validateEnabledSecretTypes(eq(List.of(CM_ADMIN_PASSWORD)), isNull());
        verify(flowManager).triggerSecretRotation(anyLong(), anyString(), any(), any(), anyMap());
    }
}
