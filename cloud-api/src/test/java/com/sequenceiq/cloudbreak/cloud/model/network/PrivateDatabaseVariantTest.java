package com.sequenceiq.cloudbreak.cloud.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PrivateDatabaseVariantTest {

    @ParameterizedTest
    @MethodSource("privateEndpointSettingsProvider")
    void testFromPrivateEndpointSettings(
            boolean hasPrivateEndpointEnabled,
            boolean hasExistingDnsZone,
            boolean hasFlexibleServerSubnets,
            PrivateDatabaseVariant expected
    ) {
        // When
        PrivateDatabaseVariant result = PrivateDatabaseVariant.fromPrivateEndpointSettings(
                hasPrivateEndpointEnabled,
                hasExistingDnsZone,
                hasFlexibleServerSubnets
        );

        // Then
        assertEquals(expected, result);
    }

    private static Stream<Arguments> privateEndpointSettingsProvider() {
        return Stream.of(
                arguments(false, false, false, PrivateDatabaseVariant.NONE),
                arguments(false, true, false, PrivateDatabaseVariant.NONE),
                arguments(true, false, true, PrivateDatabaseVariant.NONE),
                arguments(true, true, true, PrivateDatabaseVariant.NONE),
                arguments(true, false, false, PrivateDatabaseVariant.FLEXIBLE_POSTGRES_WITH_PE_AND_NEW_DNS_ZONE),
                arguments(true, true, false, PrivateDatabaseVariant.FLEXIBLE_POSTGRES_WITH_PE_AND_EXISTING_DNS_ZONE),
                arguments(false, false, true, PrivateDatabaseVariant.FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_NEW_DNS_ZONE),
                arguments(false, true, true, PrivateDatabaseVariant.FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_EXISTING_DNS_ZONE)
        );
    }
}