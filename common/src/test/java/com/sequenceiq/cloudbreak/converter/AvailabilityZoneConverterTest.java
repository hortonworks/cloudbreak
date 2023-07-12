package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;

public class AvailabilityZoneConverterTest {
    private static final Set<String> ZONES = Set.of("1", "2");

    private static final Set<String> UPDATED_ZONES = Set.of("1", "2", "3");

    private static final List<String> SUBNETS = List.of("subnet-1", "subnet-2");

    private static final Map<String, Object> ONLY_ZONES_MAP = Map.of(NetworkConstants.AVAILABILITY_ZONES, ZONES);

    private static final Map<String, Object> UPDATED_ZONES_MAP = Map.of(NetworkConstants.AVAILABILITY_ZONES, UPDATED_ZONES);

    private static final Map<String, Object> ONLY_SUBNETS_MAP = Map.of(NetworkConstants.SUBNET_IDS, SUBNETS);

    private static final Map<String, Object> SUBNETS_AND_ZONES_MAP = Map.of(NetworkConstants.AVAILABILITY_ZONES, ZONES,
            NetworkConstants.SUBNET_IDS, SUBNETS);

    private final AvailabilityZoneConverter availabilityZoneConverter = new AvailabilityZoneConverter();

    static Object [] [] dataForGetAvailabilityZonesFromJsonAttributes() {
        return new Object[] [] {
                new Object [] {null, Collections.emptySet()},
                new Object [] {new Json(null), Collections.emptySet()},
                new Object [] {new Json(""), Collections.emptySet()},
                new Object [] {new Json(ONLY_ZONES_MAP), ZONES},
                new Object [] {new Json(ONLY_SUBNETS_MAP), Collections.emptySet()},
                new Object [] {new Json(SUBNETS_AND_ZONES_MAP), ZONES},
        };
    }

    @ParameterizedTest
    @MethodSource("dataForGetAvailabilityZonesFromJsonAttributes")
    void testGetAvailabilityZonesFromJsonAttributes(Json attributes, Set<String> expectedZones) {
        Set<String> availabilityZones = availabilityZoneConverter.getAvailabilityZonesFromJsonAttributes(attributes);
        assertEquals(expectedZones, availabilityZones);
    }

    static Object [] [] dataForGetJsonAttributesWithAvailabilityZones() {
        return new Object[] [] {
                new Object [] {null, null, null},
                new Object [] {null, new Json(Collections.emptyMap()), new Json(Collections.emptyMap())},
                new Object [] {ZONES, new Json(null), new Json(ONLY_ZONES_MAP)},
                new Object [] {ZONES, new Json(""), new Json(ONLY_ZONES_MAP)},
                new Object [] {null, new Json(ONLY_SUBNETS_MAP), new Json(ONLY_SUBNETS_MAP)},
                new Object [] {ZONES, null, new Json(ONLY_ZONES_MAP)},
                new Object [] {UPDATED_ZONES, new Json(ONLY_ZONES_MAP), new Json(UPDATED_ZONES_MAP)},
                new Object [] {ZONES, new Json(ONLY_SUBNETS_MAP), new Json(SUBNETS_AND_ZONES_MAP)}
        };
    }

    @ParameterizedTest
    @MethodSource("dataForGetJsonAttributesWithAvailabilityZones")
    void testGetJsonAttributesWithAvailabilityZonesWithNull(Set<String> zones, Json existingAttributes, Json expectedAttribute) {
        Json attributes = availabilityZoneConverter.getJsonAttributesWithAvailabilityZones(zones, existingAttributes);
        assertEquals(expectedAttribute == null ? null : expectedAttribute.getMap(), attributes == null ? null : attributes.getMap());
    }

}
