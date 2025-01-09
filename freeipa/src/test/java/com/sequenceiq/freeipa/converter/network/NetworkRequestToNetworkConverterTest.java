package com.sequenceiq.freeipa.converter.network;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.entity.Network;

@ExtendWith(MockitoExtension.class)
public class NetworkRequestToNetworkConverterTest {

    @Mock
    private ResourceNameGenerator resourceNameGenerator;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @InjectMocks
    private NetworkRequestToNetworkConverter underTest;

    // Convert valid NetworkRequest to Network with all fields populated correctly
    @Test
    public void convertValidNetworkRequest() {
        NetworkRequest request = new NetworkRequest();
        request.setNetworkCidrs(List.of("10.0.0.0/16"));
        request.setOutboundInternetTraffic(OutboundInternetTraffic.DISABLED);

        Map<String, Object> params = Map.of("key", "value");
        Mappable mappable = mock(Mappable.class);
        when(mappable.asMap()).thenReturn(params);
        when(providerParameterCalculator.get(request)).thenReturn(mappable);
        when(resourceNameGenerator.generateName(APIResourceType.NETWORK)).thenReturn("n-123");

        Network result = underTest.convert(request);

        assertThat(result.getName()).isEqualTo("n-123");
        assertThat(result.getOutboundInternetTraffic()).isEqualTo(OutboundInternetTraffic.DISABLED);
        assertThat(result.getNetworkCidrs()).containsExactly("10.0.0.0/16");
        assertThat(result.getAttributes().getValue()).contains("key", "value");
    }

    // Generate unique network name using ResourceNameGenerator
    @ParameterizedTest
    @ValueSource(strings = {"n-123", "n-456", "n-789"})
    public void generateUniqueNetworkNames(String networkName) {
        NetworkRequest request = new NetworkRequest();
        when(resourceNameGenerator.generateName(APIResourceType.NETWORK)).thenReturn(networkName);
        when(providerParameterCalculator.get(request)).thenReturn(mock(Mappable.class));

        Network result = underTest.convert(request);

        assertThat(result.getName()).isEqualTo(networkName);
        verify(resourceNameGenerator).generateName(APIResourceType.NETWORK);
    }

    // Handle empty or null network CIDR list
    @ParameterizedTest
    @NullAndEmptySource
    public void handleEmptyNetworkCidrs(List<String> cidrs) {
        NetworkRequest request = new NetworkRequest();
        request.setNetworkCidrs(cidrs);
        when(providerParameterCalculator.get(request)).thenReturn(mock(Mappable.class));
        when(resourceNameGenerator.generateName(APIResourceType.NETWORK)).thenReturn("n-123");

        Network result = underTest.convert(request);

        assertThat(result.getNetworkCidrs()).isEmpty();
    }

    // Handle null provider parameters map
    @Test
    public void handleNullProviderParameters() {
        NetworkRequest request = new NetworkRequest();
        Mappable mappable = mock(Mappable.class);
        when(mappable.asMap()).thenReturn(null);
        when(providerParameterCalculator.get(request)).thenReturn(mappable);
        when(resourceNameGenerator.generateName(APIResourceType.NETWORK)).thenReturn("n-123");

        Network result = underTest.convert(request);

        assertThat(result.getAttributes()).isNull();
    }
}