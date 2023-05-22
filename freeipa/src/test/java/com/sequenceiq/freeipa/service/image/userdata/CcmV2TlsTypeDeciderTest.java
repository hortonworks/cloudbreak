package com.sequenceiq.freeipa.service.image.userdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class CcmV2TlsTypeDeciderTest {

    private final CcmV2TlsTypeDecider underTest = new CcmV2TlsTypeDecider();

    @ParameterizedTest(name = "CcmV2TlsType = {0}")
    @MethodSource("scenarios")
    void testScenarios(CcmV2TlsType tlsType, CcmV2TlsType expected) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCcmV2TlsType(tlsType);
        CcmV2TlsType result = underTest.decide(environment);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> scenarios() {
        return Stream.of(
                Arguments.of(null, CcmV2TlsType.ONE_WAY_TLS),
                Arguments.of(CcmV2TlsType.ONE_WAY_TLS, CcmV2TlsType.ONE_WAY_TLS),
                Arguments.of(CcmV2TlsType.TWO_WAY_TLS, CcmV2TlsType.TWO_WAY_TLS)
        );
    }

}
