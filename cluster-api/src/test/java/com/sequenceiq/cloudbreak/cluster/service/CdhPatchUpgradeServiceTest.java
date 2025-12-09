package com.sequenceiq.cloudbreak.cluster.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.dto.StackDto;

@ExtendWith(MockitoExtension.class)
class CdhPatchUpgradeServiceTest {

    private static final String STACK_NAME = "stack-name";

    @Mock
    private ClusterApi connector;

    @Mock
    private StackDto stackDto;

    @Spy
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @InjectMocks
    private CdhPatchUpgradeService underTest;

    static Stream<Arguments> testIsCdhPatchUpgradeArguments() {
        return Stream.of(
                Arguments.of(null, null, false),
                Arguments.of(null, "7.2.18-1.cdh7.2.18.p1101.68994679", false),
                Arguments.of("7.2.18-1.cdh7.2.18.p1101.68994679", null, false),
                Arguments.of("7.2.18-1.cdh7.2.18.p1101.68994679", "7.2.18", false),
                Arguments.of("7.2.18-1.cdh7.2.18.p1101.68994679", "7.2.18-1.cdh7.2.18.p1101", false),
                Arguments.of("7.2.18-1.cdh7.2.18.p1101.68994679", "7.3.1-1.cdh7.3.1.p1101.68994679", false),
                Arguments.of("7.2.18-1.cdh7.2.18.p1101.68994679", "7.2.18-1.cdh7.2.18.p1101.68994679", false),
                Arguments.of("7.2.18-1.cdh7.2.18.p1101.68994679", "7.2.18-1.cdh7.2.18.p1102.68994679", true),
                Arguments.of("7.2.18-1.cdh7.2.18.p1101.68994679", "7.2.18-1.cdh7.2.18.p1101.68994680", true),
                Arguments.of("7.2.18-1.cdh7.2.18.p1101.68994679", "7.2.18-1.cdh7.2.18.p1102.68994680", true)
        );
    }

    @MethodSource("testIsCdhPatchUpgradeArguments")
    @ParameterizedTest
    void testIsCdhPatchUpgrade(String currentCdhVersion, String targetCdhVersion, boolean expected) throws Exception {
        Set<ClouderaManagerProduct> products = Set.of();
        if (targetCdhVersion != null) {
            products = Set.of(new ClouderaManagerProduct().withName(StackType.CDH.name()).withVersion(targetCdhVersion));
            when(stackDto.getName()).thenReturn(STACK_NAME);
            when(connector.getStackCdhVersion(STACK_NAME)).thenReturn(currentCdhVersion);
        }

        assertEquals(expected, underTest.isCdhPatchUpgrade(products, connector, stackDto));
    }
}
