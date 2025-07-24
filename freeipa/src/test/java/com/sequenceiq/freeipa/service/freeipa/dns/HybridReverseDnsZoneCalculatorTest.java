package com.sequenceiq.freeipa.service.freeipa.dns;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HybridReverseDnsZoneCalculatorTest {

    private HybridReverseDnsZoneCalculator underTest = new HybridReverseDnsZoneCalculator();

    @ParameterizedTest(name = "{0}: With CIDR={1}")
    @MethodSource("cidrSource")
    void testABit(String name, String cidr, Set<String> expectedResult) {
        Set<String> result = underTest.reverseDnsZoneForCidr(cidr);
        assertEquals(expectedResult, result);
    }

    public static Stream<Arguments> cidrSource() {
        return Stream.of(
                Arguments.of("Classful A", "10.1.2.3/8", Set.of("10.in-addr.arpa.")),
                Arguments.of("Classful B", "10.1.2.3/16", Set.of("1.10.in-addr.arpa.")),
                Arguments.of("Classful C", "10.1.2.3/24", Set.of("2.1.10.in-addr.arpa.")),
                Arguments.of("Classless A", "172.1.2.3/6", Set.of("172.in-addr.arpa.", "173.in-addr.arpa.", "174.in-addr.arpa.", "175.in-addr.arpa.")),
                Arguments.of("Classless B", "10.168.2.3/14", Set.of("168.10.in-addr.arpa.", "169.10.in-addr.arpa.", "170.10.in-addr.arpa.",
                        "171.10.in-addr.arpa.")),
                Arguments.of("Classless C", "10.5.144.3/22", Set.of("144.5.10.in-addr.arpa.", "145.5.10.in-addr.arpa.", "146.5.10.in-addr.arpa.",
                        "147.5.10.in-addr.arpa.")),
                Arguments.of("Classless D", "10.1.2.3/26", Set.of("2.1.10.in-addr.arpa."))
        );
    }

}