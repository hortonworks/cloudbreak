package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.model.CloudIdentityType;

class CloudIdentityTypeDeciderTest {

    private final CloudIdentityTypeDecider underTest = new CloudIdentityTypeDecider();

    static Set<String>[] scenariosForLog() {
        return new Set[] {
                null,
                Set.of(),
                Set.of("test1", "test2")
        };
    }

    @ParameterizedTest
    @MethodSource("scenariosForLog")
    void testGetIdentityTypeWhenLog(Set<String> components) {
        assertEquals(CloudIdentityType.LOG, underTest.getIdentityType(components));
    }

    static Set<String>[] scenariosForIdBroker() {
        return new Set[] {
                Set.of(KnoxRoles.IDBROKER),
                Set.of("test1", KnoxRoles.IDBROKER, "test2")
        };
    }

    @ParameterizedTest
    @MethodSource("scenariosForIdBroker")
    void testGetIdentityTypeWhenIdBroker(Set<String> components) {
        assertEquals(CloudIdentityType.ID_BROKER, underTest.getIdentityType(components));
    }

    @Test
    void testGetIdentityTypeForInstanceGroupWhenLog() {
        Map<String, Set<String>> componentsByHostGroup = Map.of("master", Set.of("test"), "idbroker", Set.of("test1", KnoxRoles.IDBROKER, "test2"));
        assertEquals(CloudIdentityType.LOG, underTest.getIdentityTypeForInstanceGroup("master", componentsByHostGroup));
    }

    @Test
    void testGetIdentityTypeForInstanceGroupWhenIdBroker() {
        Map<String, Set<String>> componentsByHostGroup = Map.of("master", Set.of("test"), "idbroker", Set.of("test1", KnoxRoles.IDBROKER, "test2"));
        assertEquals(CloudIdentityType.ID_BROKER, underTest.getIdentityTypeForInstanceGroup("idbroker", componentsByHostGroup));
    }

    @Test
    void testGetIdentityTypeForInstanceGroupWhenComponentsNotFound() {
        assertThrows(CloudbreakServiceException.class, () -> underTest.getIdentityTypeForInstanceGroup("master", Map.of()));
    }
}
