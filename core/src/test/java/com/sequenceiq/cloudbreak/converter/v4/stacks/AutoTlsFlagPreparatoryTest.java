package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
class AutoTlsFlagPreparatoryTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @InjectMocks
    private AutoTlsFlagPreparatory underTest;

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void testAutoTlsSettingFromRequest(String value) {
        ClusterV4Request request = new ClusterV4Request();
        request.setCm(new ClouderaManagerV4Request());
        request.getCm().setEnableAutoTls(Boolean.valueOf(value));

        boolean result = underTest.provideAutoTlsFlag(request, new Stack(), Optional.empty());

        verifyNoInteractions(cloudPlatformConnectors);
        assertEquals(Boolean.valueOf(value), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void testAutoTlsSettingFromPlatformParameterWithParent(String value) {
        ClusterV4Request request = new ClusterV4Request();
        Stack stack = new Stack();
        stack.setPlatformVariant("var");
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(Platform.platform("magic"), Variant.variant("var"))).thenReturn(connector);
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        when(connector.parameters()).thenReturn(platformParameters);
        when(platformParameters.isAutoTlsSupported()).thenReturn(Boolean.valueOf(value));

        boolean result = underTest.provideAutoTlsFlag(request, stack, Optional.of("magic"));

        assertEquals(Boolean.valueOf(value), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void testAutoTlsSettingFromPlatformParameterWithoutParent(String value) {
        ClusterV4Request request = new ClusterV4Request();
        Stack stack = new Stack();
        stack.setPlatformVariant("var");
        stack.setCloudPlatform("magic");
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(Platform.platform("magic"), Variant.variant("var"))).thenReturn(connector);
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        when(connector.parameters()).thenReturn(platformParameters);
        when(platformParameters.isAutoTlsSupported()).thenReturn(Boolean.valueOf(value));

        boolean result = underTest.provideAutoTlsFlag(request, stack, Optional.empty());

        assertEquals(Boolean.valueOf(value), result);
    }

}
