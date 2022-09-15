package com.sequenceiq.freeipa.service.image.userdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class CcmV2TlsTypeDeciderTest {

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private CcmV2TlsTypeDecider underTest;

    @ParameterizedTest(name = "CcmV2TlsType = {0}, EntitlementEnabled = {1}")
    @MethodSource("scenarios")
    void testScenarios(CcmV2TlsType tlsType, boolean entitlementEnabled, CcmV2TlsType expected) {
        lenient().when(entitlementService.ccmV2UseOneWayTls(any())).thenReturn(entitlementEnabled);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCcmV2TlsType(tlsType);
        CcmV2TlsType result = underTest.decide(environment);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> scenarios() {
        return Stream.of(
                Arguments.of(null, false, CcmV2TlsType.TWO_WAY_TLS),
                Arguments.of(null, true, CcmV2TlsType.ONE_WAY_TLS),
                Arguments.of(CcmV2TlsType.ONE_WAY_TLS, false, CcmV2TlsType.ONE_WAY_TLS),
                Arguments.of(CcmV2TlsType.ONE_WAY_TLS, true, CcmV2TlsType.ONE_WAY_TLS),
                Arguments.of(CcmV2TlsType.TWO_WAY_TLS, false, CcmV2TlsType.TWO_WAY_TLS),
                Arguments.of(CcmV2TlsType.TWO_WAY_TLS, true, CcmV2TlsType.TWO_WAY_TLS)
        );
    }

}
