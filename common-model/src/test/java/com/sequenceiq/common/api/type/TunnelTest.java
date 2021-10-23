package com.sequenceiq.common.api.type;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class TunnelTest {

    @Test
    void testLatestUpgradeTarget() {
        assertThat(Tunnel.latestUpgradeTarget()).isEqualTo(Tunnel.CCMV2_JUMPGATE);
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "CCM", "CCMV2", "CCMV2_JUMPGATE"}, mode = Mode.INCLUDE)
    void testUseCCM(Tunnel underTest) {
        assertThat(underTest.useCcm()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "CCM", "CCMV2", "CCMV2_JUMPGATE"}, mode = Mode.EXCLUDE)
    void testNotUseCCM(Tunnel underTest) {
        assertThat(underTest.useCcm()).isFalse();
    }

    @Test
    void testUseCCMv1() {
        assertThat(Tunnel.CCM.useCcmV1()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = "CCM", mode = Mode.EXCLUDE)
    void testNotUseCCMv1(Tunnel underTest) {
        assertThat(underTest.useCcmV1()).isFalse();
    }

    @Test
    void testUseCCMv2() {
        assertThat(Tunnel.CCMV2.useCcmV2()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = "CCMV2", mode = Mode.EXCLUDE)
    void testNotUseCCMV2(Tunnel underTest) {
        assertThat(underTest.useCcmV2()).isFalse();
    }

    @Test
    void testUseCCMv2Jumpgate() {
        assertThat(Tunnel.CCMV2_JUMPGATE.useCcmV2Jumpgate()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = "CCMV2_JUMPGATE", mode = Mode.EXCLUDE)
    void testNotUseCCMv2Jumpgate(Tunnel underTest) {
        assertThat(underTest.useCcmV2Jumpgate()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "CCMV2", "CCMV2_JUMPGATE"}, mode = Mode.INCLUDE)
    void testUseCCMv2OrJumpgate(Tunnel underTest) {
        assertThat(underTest.useCcmV2OrJumpgate()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "CCMV2", "CCMV2_JUMPGATE"}, mode = Mode.EXCLUDE)
    void testNotUseCCMv2OrJumpgate(Tunnel underTest) {
        assertThat(underTest.useCcmV2OrJumpgate()).isFalse();
    }

    @Test
    void testUseClusterProxy() {
        assertThat(Tunnel.DIRECT.useClusterProxy()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = "DIRECT", mode = Mode.EXCLUDE)
    void testNotUseClusterProxy(Tunnel underTest) {
        assertThat(underTest.useClusterProxy()).isTrue();
    }

}
