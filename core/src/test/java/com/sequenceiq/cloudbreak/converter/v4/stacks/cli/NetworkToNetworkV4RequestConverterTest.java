package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.domain.Network;

class NetworkToNetworkV4RequestConverterTest {

    private static final Map<String, Object> ATTRIBUTES_MAP = Map.of("paprikas", "krumpli");

    @Mock
    private ProviderParameterCalculator mockProviderParameterCalculator;

    @Mock
    private Json attributes;

    @InjectMocks
    private NetworkToNetworkV4RequestConverter underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(attributes.getMap()).thenReturn(ATTRIBUTES_MAP);
    }

    @Test
    void testConvertWhenInputIsNullThenIllegalStateExceptionShouldCome() {
        IllegalStateException expectedException = assertThrows(IllegalStateException.class, () -> underTest.convert(null));

        assertEquals(Network.class.getSimpleName() + " should not be null!", expectedException.getMessage());
    }

    @Test
    void testConvertWhenAttributesIsEmptyThenNorProviderParameterCalculatorShouldNotBeCalled() {
        underTest.convert(new Network());

        verify(mockProviderParameterCalculator, never()).parse(any(), any());
        verify(mockProviderParameterCalculator, never()).parse(any(Map.class), any(NetworkV4Request.class));
    }

    @Test
    void testConvertWhenAttributesIsNotEmptyThenNorProviderParameterCalculatorShouldNotBeCalled() {
        Network input = new Network();
        input.setAttributes(attributes);

        underTest.convert(input);

        verify(mockProviderParameterCalculator, times(1)).parse(any(), any());
        verify(mockProviderParameterCalculator, times(1)).parse(any(Map.class), any(NetworkV4Request.class));
    }

    @Test
    void testConvertWhenEmptyNetworkInstancePassedThenNullShouldReturn() {
        assertNull(underTest.convert(new Network()));
    }

    @Test
    void testConvertWhenSubnetCidrIsNotNullThenResultShouldContainSubnetCidr() {
        Network input = new Network();
        input.setSubnetCIDR("someCidr");

        NetworkV4Request result = underTest.convert(input);

        assertNotNull(result);
        assertEquals(input.getSubnetCIDR(), result.getSubnetCIDR());
    }

}