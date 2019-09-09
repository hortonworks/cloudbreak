package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.GenerateAndRegisterSshTunnelingKeyPairResponse;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.MinaSshdService;
import com.google.common.base.Throwables;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameterSupplier;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultInstanceParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultServerParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultTunnelParameters;
import com.sequenceiq.cloudbreak.ccm.endpoint.BaseServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.HostEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccmimpl.altus.GrpcMinaSshdManagementClient;

/**
 * Default supplier of CCM parameters, which:
 * <ol>
 * <li>contacts the minasshd management server via GRPC to acquire a minasshd instance;</li>
 * <li>creates corresponding server parameters;</li>
 * <li>contacts the minasshd instance via GRPC to register an instance and generate a keypair;</li>
 * <li>creates the corresponding instance parameters; and</li>
 * <li>creates the tunnel parameters based on the passed-in arguments.</li>
 * </ol>
 */
@Component
public class DefaultCcmParameterSupplier implements CcmParameterSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCcmParameterSupplier.class);

    @Inject
    private GrpcMinaSshdManagementClient grpcMinaSshdManagementClient;

    @Override
    public Optional<CcmParameters> getBaseCcmParameters(@Nonnull String actorCrn, @Nonnull String accountId, @Nonnull String keyId) {
        if (grpcMinaSshdManagementClient == null) {
            return Optional.empty();
        }
        // JSA TODO get request ID from somewhere?
        String requestId = UUID.randomUUID().toString();
        try {
            MinaSshdService minaSshdService = grpcMinaSshdManagementClient.acquireMinaSshdServiceAndWaitUntilReady(
                    requestId,
                    Objects.requireNonNull(actorCrn, "actorCrn is null"),
                    Objects.requireNonNull(accountId, "accountId is null"));

            GenerateAndRegisterSshTunnelingKeyPairResponse keyPairResponse =
                    grpcMinaSshdManagementClient.generateAndRegisterSshTunnelingKeyPair(
                            requestId, actorCrn, accountId, minaSshdService.getMinaSshdServiceId(), keyId);

            return Optional.of(
                    new DefaultCcmParameters(
                            createServerParameters(minaSshdService),
                            createInstanceParameters(keyId, keyPairResponse.getEncipheredPrivateKey()),
                            Collections.emptyList()));
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<CcmParameters> getCcmParameters(
            @Nullable CcmParameters baseCcmParameters,
            @Nullable Map<KnownServiceIdentifier, Integer> tunneledServicePorts) {
        return ((baseCcmParameters == null) || (tunneledServicePorts == null) || tunneledServicePorts.isEmpty()) ? Optional.empty() : Optional.of(
                new DefaultCcmParameters(
                        baseCcmParameters.getServerParameters(),
                        baseCcmParameters.getInstanceParameters(),
                        tunneledServicePorts.entrySet().stream()
                                .map(entry -> new DefaultTunnelParameters(entry.getKey(), entry.getValue()))
                                .collect(Collectors.toList())));
    }

    /**
     * Creates instance parameters.
     *
     * @param keyId    the key ID under which the private key was registered with CCM
     * @param encipheredPrivateKey the enciphered private key
     * @return the instance parameters
     */
    private DefaultInstanceParameters createInstanceParameters(String keyId, String encipheredPrivateKey) {
        return new DefaultInstanceParameters(null, keyId, encipheredPrivateKey);
    }

    /**
     * Creates server parameters based on the specified minasshd service.
     *
     * @param minaSshdService the minasshd service
     * @return the server parameters
     */
    private DefaultServerParameters createServerParameters(MinaSshdService minaSshdService) {
        MinaSshdManagementProto.SshTunnelingConfiguration sshTunnelingConfiguration = minaSshdService.getSshTunnelingConfiguration();
        MinaSshdManagementProto.NlbPort nlbPort = sshTunnelingConfiguration.getNlbPort();
        String ccmHostAddressString = nlbPort.getNlbFqdn();
        int ccmPort = nlbPort.getPort();
        String ccmPublicKey = sshTunnelingConfiguration.getSshdPublicKey().toStringUtf8();
        return new DefaultServerParameters(new BaseServiceEndpoint(new HostEndpoint(ccmHostAddressString), ccmPort, null),
                "ssh-rsa " + ccmPublicKey + "\n");
    }
}
