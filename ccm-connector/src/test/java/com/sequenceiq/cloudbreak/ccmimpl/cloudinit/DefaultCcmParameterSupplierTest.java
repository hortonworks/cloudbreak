package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultInstanceParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultServerParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultTunnelParameters;
import com.sequenceiq.cloudbreak.ccm.endpoint.HostEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.HttpsServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccmimpl.altus.GrpcMinaSshdManagementClient;

@ExtendWith(MockitoExtension.class)
class DefaultCcmParameterSupplierTest {

    @InjectMocks
    private DefaultCcmParameterSupplier ccmParameterSupplier;

    @Mock
    private GrpcMinaSshdManagementClient grpcMinaSshdManagementClient;

    @Test
    void testGetCcmParametersWhenBaseCcmParametersIsNull() {
        CcmParameters baseCcmParameters = null;
        Map<KnownServiceIdentifier, Integer> tunneledServicePorts = new HashMap<>();

        Optional<CcmParameters> result = ccmParameterSupplier.getCcmParameters(baseCcmParameters, tunneledServicePorts);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetCcmParametersWhenTunneledServicePortsIsNull() {
        CcmParameters baseCcmParameters = new DefaultCcmParameters(
                new DefaultServerParameters(new HttpsServiceEndpoint(new HostEndpoint("address")), "ccmPublicKey", "minaSshdServiceId"),
                new DefaultInstanceParameters("tunnel", "key", "privateKey"),
                List.of(new DefaultTunnelParameters(KnownServiceIdentifier.KNOX, 123)));
        Map<KnownServiceIdentifier, Integer> tunneledServicePorts = new HashMap<>();

        Optional<CcmParameters> result = ccmParameterSupplier.getCcmParameters(baseCcmParameters, tunneledServicePorts);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetCcmParametersWhenTunneledServicePortsIsEmpty() {
        CcmParameters baseCcmParameters = new DefaultCcmParameters(
                new DefaultServerParameters(new HttpsServiceEndpoint(new HostEndpoint("address")), "ccmPublicKey", "minaSshdServiceId"),
                new DefaultInstanceParameters("tunnel", "key", "privateKey"),
                new ArrayList<>());
        Map<KnownServiceIdentifier, Integer> tunneledServicePorts = Collections.emptyMap();

        Optional<CcmParameters> result = ccmParameterSupplier.getCcmParameters(baseCcmParameters, tunneledServicePorts);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetCcmParametersWhenBothBaseAndTunneledParametersAreValid() {
        CcmParameters baseCcmParameters = new DefaultCcmParameters(
                new DefaultServerParameters(new HttpsServiceEndpoint(new HostEndpoint("address")), "ccmPublicKey", "minaSshdServiceId"),
                new DefaultInstanceParameters("tunnel", "key", "privateKey"),
                new ArrayList<>());
        Map<KnownServiceIdentifier, Integer> tunneledServicePorts = new HashMap<>();
        tunneledServicePorts.put(KnownServiceIdentifier.GATEWAY, 123);
        tunneledServicePorts.put(KnownServiceIdentifier.KNOX, 456);

        Optional<CcmParameters> result = ccmParameterSupplier.getCcmParameters(baseCcmParameters, tunneledServicePorts);

        assertTrue(result.isPresent());
    }
}