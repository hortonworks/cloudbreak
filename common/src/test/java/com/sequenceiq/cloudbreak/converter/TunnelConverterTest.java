package com.sequenceiq.cloudbreak.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.type.Tunnel;

class TunnelConverterTest {

    private final TunnelConverter underTest = new TunnelConverter();

    @Test
    void ccmv2JumpgateEnumToDb() {
        assertThat(underTest.convertToDatabaseColumn(Tunnel.CCMV2_JUMPGATE)).isEqualTo("CCMV2_JUMPGATE");
    }

    @Test
    void dbToccmV2JumpgateEnum() {
        assertThat(underTest.convertToEntityAttribute("CCMV2_JUMPGATE")).isEqualTo(Tunnel.CCMV2_JUMPGATE);
    }

}
