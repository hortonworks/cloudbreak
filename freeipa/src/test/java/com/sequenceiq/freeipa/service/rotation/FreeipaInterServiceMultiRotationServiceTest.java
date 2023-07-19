package com.sequenceiq.freeipa.service.rotation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1RotationEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxRotationEndpoint;

@ExtendWith(MockitoExtension.class)
public class FreeipaInterServiceMultiRotationServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:9d74eee4:environment:12474ddc";

    private static final String INTERNAL_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    @Mock
    private DistroXV1RotationEndpoint distroXV1RotationEndpoint;

    @Mock
    private SdxRotationEndpoint sdxRotationEndpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @InjectMocks
    private FreeipaInterServiceMultiRotationService underTest;

    @BeforeEach
    void setup() {
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_CRN);
    }

    @Test
    void testCheck() {
        when(distroXV1RotationEndpoint.checkOngoingChildrenMultiSecretRotations(any(), any(), any())).thenReturn(Boolean.FALSE);
        when(sdxRotationEndpoint.checkOngoingMultiSecretChildrenRotations(any(), any(), any())).thenReturn(Boolean.FALSE);

        assertFalse(underTest.checkOngoingChildrenMultiSecretRotations(ENV_CRN, TestFreeipaMultiSecretType.IPA_MULTI_SECRET));

        verify(sdxRotationEndpoint).checkOngoingMultiSecretChildrenRotations(eq(ENV_CRN), any(), any());
        verify(distroXV1RotationEndpoint).checkOngoingChildrenMultiSecretRotations(eq(ENV_CRN), any(), any());
    }

    @Test
    void testCheckIfOngoing() {
        when(distroXV1RotationEndpoint.checkOngoingChildrenMultiSecretRotations(any(), any(), any())).thenReturn(Boolean.FALSE);
        when(sdxRotationEndpoint.checkOngoingMultiSecretChildrenRotations(any(), any(), any())).thenReturn(Boolean.TRUE);

        assertTrue(underTest.checkOngoingChildrenMultiSecretRotations(ENV_CRN, TestFreeipaMultiSecretType.IPA_MULTI_SECRET));

        verify(sdxRotationEndpoint).checkOngoingMultiSecretChildrenRotations(eq(ENV_CRN), any(), any());
        verify(distroXV1RotationEndpoint).checkOngoingChildrenMultiSecretRotations(eq(ENV_CRN), any(), any());
    }

    @Test
    void testMark() {
        doNothing().when(distroXV1RotationEndpoint).markMultiClusterChildrenResources(any(), any(), any());
        doNothing().when(sdxRotationEndpoint).markMultiClusterChildrenResources(any(), any(), any());

        underTest.markChildren(ENV_CRN, TestFreeipaMultiSecretType.IPA_MULTI_SECRET);

        verify(sdxRotationEndpoint).markMultiClusterChildrenResources(eq(ENV_CRN), any(), any());
        verify(distroXV1RotationEndpoint).markMultiClusterChildrenResources(eq(ENV_CRN), any(), any());
    }
}
