package com.sequenceiq.environment.environment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;

class ExperimentalFeaturesTest {

    static Object[][] overrideTunnelDataProvider() {
        return new Object[][]{
                // overrideTunnel overrideTunnelExpected
                {null, false},
                {false, false},
                {true, true},
        };
    }

    @ParameterizedTest(name = "overrideTunnel={0}")
    @MethodSource("overrideTunnelDataProvider")
    void builderTest(Boolean overrideTunnel, boolean overrideTunnelExpected) {
        ExperimentalFeatures result = ExperimentalFeatures.builder()
                .withOverrideTunnel(overrideTunnel)
                .withTunnel(Tunnel.CCM)
                .withIdBrokerMappingSource(IdBrokerMappingSource.IDBMMS)
                .withCloudStorageValidation(CloudStorageValidation.ENABLED)
                .withCcmV2TlsType(CcmV2TlsType.TWO_WAY_TLS)
                .build();

        assertThat(result).isNotNull();
        assertThat(result.isOverrideTunnel()).isEqualTo(overrideTunnelExpected);
        assertThat(result.getTunnel()).isEqualTo(Tunnel.CCM);
        assertThat(result.getIdBrokerMappingSource()).isEqualTo(IdBrokerMappingSource.IDBMMS);
        assertThat(result.getCloudStorageValidation()).isEqualTo(CloudStorageValidation.ENABLED);
        assertThat(result.getCcmV2TlsType()).isEqualTo(CcmV2TlsType.TWO_WAY_TLS);
    }

}