package com.sequenceiq.environment.environment.flow.encryptionprofile.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileClusterResolverTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:1234:datalake:dl";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:1234:cluster:dh";

    private static final String LEGACY_ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_fips_v1";

    private static final String NON_LEGACY_DEFAULT_ENCRYPTION_PROFILE_CRN =
            "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_fips_140_3";

    private static final String OWN_ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:1234:encryptionProfile:custom";

    @Mock
    private SdxService sdxService;

    @Mock
    private DatahubService datahubService;

    @InjectMocks
    private EncryptionProfileClusterResolver underTest;

    @Test
    void testIsDatalake() {
        assertThat(underTest.isDatalake(stack(DATALAKE_CRN, StackType.DATALAKE))).isTrue();
        assertThat(underTest.isDatalake(stack(DATAHUB_CRN, StackType.WORKLOAD))).isFalse();
        assertThat(underTest.isDatalake(new StackViewV4Response())).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {LEGACY_ENCRYPTION_PROFILE_CRN})
    void testDatalakeWithoutOwnEncryptionProfile(String encryptionProfileCrn) {
        when(sdxService.getDetailByCrn(DATALAKE_CRN)).thenReturn(datalakeDetail(encryptionProfileCrn));

        assertThat(underTest.hasOwnEncryptionProfile(stack(DATALAKE_CRN, StackType.DATALAKE))).isFalse();
        verify(datahubService, never()).getByCrn(any(), anySet());
    }

    @ParameterizedTest
    @ValueSource(strings = {OWN_ENCRYPTION_PROFILE_CRN, NON_LEGACY_DEFAULT_ENCRYPTION_PROFILE_CRN})
    void testDatalakeWithOwnEncryptionProfile(String encryptionProfileCrn) {
        when(sdxService.getDetailByCrn(DATALAKE_CRN)).thenReturn(datalakeDetail(encryptionProfileCrn));

        assertThat(underTest.hasOwnEncryptionProfile(stack(DATALAKE_CRN, StackType.DATALAKE))).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {LEGACY_ENCRYPTION_PROFILE_CRN})
    void testDatahubWithoutOwnEncryptionProfile(String encryptionProfileCrn) {
        when(datahubService.getByCrn(eq(DATAHUB_CRN), anySet())).thenReturn(datahubDetail(encryptionProfileCrn));

        assertThat(underTest.hasOwnEncryptionProfile(stack(DATAHUB_CRN, StackType.WORKLOAD))).isFalse();
        verify(sdxService, never()).getDetailByCrn(any());
    }

    @Test
    void testDatahubWithOwnEncryptionProfile() {
        when(datahubService.getByCrn(eq(DATAHUB_CRN), anySet())).thenReturn(datahubDetail(OWN_ENCRYPTION_PROFILE_CRN));

        assertThat(underTest.hasOwnEncryptionProfile(stack(DATAHUB_CRN, StackType.WORKLOAD))).isTrue();
    }

    @Test
    void testMissingDetailsMeansNoOwnEncryptionProfile() {
        when(sdxService.getDetailByCrn(DATALAKE_CRN)).thenReturn(null);
        when(datahubService.getByCrn(eq(DATAHUB_CRN), anySet())).thenReturn(new StackV4Response());

        assertThat(underTest.hasOwnEncryptionProfile(stack(DATALAKE_CRN, StackType.DATALAKE))).isFalse();
        assertThat(underTest.hasOwnEncryptionProfile(stack(DATAHUB_CRN, StackType.WORKLOAD))).isFalse();
    }

    @Test
    void testLookupFailureIsPropagated() {
        when(datahubService.getByCrn(eq(DATAHUB_CRN), anySet())).thenThrow(new DatahubOperationFailedException("boom"));

        assertThatThrownBy(() -> underTest.hasOwnEncryptionProfile(stack(DATAHUB_CRN, StackType.WORKLOAD)))
                .isInstanceOf(DatahubOperationFailedException.class)
                .hasMessage("boom");
    }

    private StackViewV4Response stack(String crn, StackType stackType) {
        StackViewV4Response stack = new StackViewV4Response();
        stack.setCrn(crn);
        stack.setName(crn);
        stack.setStackType(stackType.name());
        return stack;
    }

    private SdxClusterDetailResponse datalakeDetail(String encryptionProfileCrn) {
        SdxClusterDetailResponse detail = new SdxClusterDetailResponse();
        detail.setStackV4Response(datahubDetail(encryptionProfileCrn));
        return detail;
    }

    private StackV4Response datahubDetail(String encryptionProfileCrn) {
        StackV4Response stack = new StackV4Response();
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setEncryptionProfileCrn(encryptionProfileCrn);
        stack.setCluster(cluster);
        return stack;
    }
}
