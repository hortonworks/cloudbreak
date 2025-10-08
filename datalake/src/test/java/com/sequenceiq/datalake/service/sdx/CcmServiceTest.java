package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.CCMV2_JUMPGATE_REQUIRED_VERSION;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.type.Tunnel;

class CcmServiceTest {

    private CcmService underTest;

    static Object[][] ccmV2Scenarios() {
        return new Object[][]{
                // runtime  compatible
                {null, true},
                {"7.2.0", false},
                {"7.2.1", true},
                {"7.2.5", true},
                {"7.2.6", true},
                {"7.2.7", true},
        };
    }

    static Object[][] ccmV2JumpgateScenarios() {
        return new Object[][]{
                // runtime  compatible
                {null, true},
                {"7.2.0", false},
                {"7.2.1", false},
                {"7.2.5", false},
                {"7.2.6", true},
                {"7.2.7", true},
        };
    }

    @BeforeEach
    void setUp() {
        underTest = new CcmService();
    }

    @ParameterizedTest(name = "Runtime {0} is compatible with CCMv2 = {1}")
    @MethodSource("ccmV2Scenarios")
    void testCcmV2VersionChecker(String runtime, boolean compatible) throws IOException {
        if (!compatible) {
            assertThatThrownBy(() -> underTest.validateCcmV2Requirement(Tunnel.CCMV2, runtime))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage(String.format("Runtime version %s does not support Cluster Connectivity Manager. "
                            + "Please try creating a datalake with runtime version at least %s.", runtime, SdxVersionRuleEnforcer.CCMV2_REQUIRED_VERSION));
        } else {
            underTest.validateCcmV2Requirement(Tunnel.CCMV2, runtime);
        }
    }

    @ParameterizedTest(name = "Runtime {0} is compatible with CCMv2JumpGate = {1}")
    @MethodSource("ccmV2JumpgateScenarios")
    void testCcmV2JumpgateVersionChecker(String runtime, boolean compatible) throws IOException {
        if (!compatible) {
            assertThatThrownBy(() -> underTest.validateCcmV2Requirement(Tunnel.CCMV2_JUMPGATE, runtime)).isInstanceOf(BadRequestException.class)
                    .hasMessage(String.format("Runtime version %s does not support Cluster Connectivity Manager. "
                            + "Please try creating a datalake with runtime version at least %s.", runtime, CCMV2_JUMPGATE_REQUIRED_VERSION));
        } else {
            underTest.validateCcmV2Requirement(Tunnel.CCMV2_JUMPGATE, runtime);
        }
    }
}