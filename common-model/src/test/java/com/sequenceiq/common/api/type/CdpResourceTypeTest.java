package com.sequenceiq.common.api.type;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CdpResourceTypeTest {

    static Object[][] prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateTypeDataProvider() {
        return new Object[][]{
                // type, cdpResourceTypeExpected
                {null, CdpResourceType.DEFAULT},
                {"", CdpResourceType.DEFAULT},
                {"foo", CdpResourceType.DEFAULT},
                {"distrox", CdpResourceType.DATAHUB},
                {"DISTROX", CdpResourceType.DATAHUB},
                {"1-DISTROX-2", CdpResourceType.DATAHUB},
                {"workload", CdpResourceType.DATAHUB},
                {"WORKLOAD", CdpResourceType.DATAHUB},
                {"3-WORKLOAD-4", CdpResourceType.DATAHUB},
                {"sdx", CdpResourceType.DATALAKE},
                {"SDX", CdpResourceType.DATALAKE},
                {"5-SDX-6", CdpResourceType.DATALAKE},
                {"datalake", CdpResourceType.DATALAKE},
                {"DATALAKE", CdpResourceType.DATALAKE},
                {"7-DATALAKE-8", CdpResourceType.DATALAKE},
        };
    }

    @ParameterizedTest(name = "type={0}")
    @MethodSource("prescribeSslCertificateIfNeededTestWhenSslBadSslCertificateTypeDataProvider")
    void fromStackTypeTest(String type, CdpResourceType cdpResourceTypeExpected) {
        assertThat(CdpResourceType.fromStackType(type)).isEqualTo(cdpResourceTypeExpected);
    }

}