package com.sequenceiq.remoteenvironment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@ExtendWith(MockitoExtension.class)
class RemoteEnvironmentConnectorProviderTest {

    private Set<RemoteEnvironmentConnector> connectors;

    private RemoteEnvironmentConnectorProvider underTest;

    @BeforeEach
    void setUp() {
        connectors = Arrays.stream(RemoteEnvironmentConnectorType.values())
                .map(this::mockConnector)
                .collect(Collectors.toSet());
        underTest = new RemoteEnvironmentConnectorProvider(connectors);
    }

    @Test
    void testEmptyConnectors() {
        Assertions.assertThatThrownBy(() -> new RemoteEnvironmentConnectorProvider(Set.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not all RemoteEnvironmentTypes have RemoteEnvironmentConnector implementations.");
    }

    @Test
    void all() {
        assertThat(underTest.all()).containsAll(connectors);
    }

    @ParameterizedTest
    @EnumSource(RemoteEnvironmentConnectorType.class)
    void getForType(RemoteEnvironmentConnectorType type) {
        assertThat(underTest.getForType(type).type()).isEqualTo(type);
    }

    @Test
    void getForCrn() {
        String crn = "crn:cdp:classicclusters:us-west-1:cloudera:cluster:c1";
        assertThat(underTest.getForCrn(crn).type()).isEqualTo(RemoteEnvironmentConnectorType.CLASSIC_CLUSTER);
    }

    private RemoteEnvironmentConnector mockConnector(RemoteEnvironmentConnectorType type) {
        RemoteEnvironmentConnector connector = mock(RemoteEnvironmentConnector.class);
        when(connector.type()).thenReturn(type);
        return connector;
    }

}
