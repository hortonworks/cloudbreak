package com.sequenceiq.cloudbreak.cloud.azure.view;

import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView.SUBNETS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.cloud.model.Network;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class AzureNetworkViewTest {

    @Mock
    private Network network;

    private AzureNetworkView underTest;

    @Before
    public void setUp() {
        initMocks(this);

        underTest = new AzureNetworkView(network);
    }

    @Test
    public void testSubnets() {
        when(network.getStringParameter(SUBNETS)).thenReturn("subnet-a,subnet-b");
        assertThat(underTest.getSubnets()).isEqualTo("subnet-a,subnet-b");
    }
}
