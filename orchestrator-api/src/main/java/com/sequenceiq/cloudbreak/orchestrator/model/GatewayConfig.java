package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.ccm.endpoint.HostEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.HttpsServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointLookupException;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointRequest;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;

public class GatewayConfig {

    // Used by cloudbreak to connect the cluster
    private final String connectionAddress;

    private final String publicAddress;

    private final String privateAddress;

    private final String hostname;

    private final String serverCert;

    private final String clientCert;

    private final String clientKey;

    private final Integer gatewayPort;

    private final String instanceId;

    private final String saltPassword;

    private final String saltBootPassword;

    private final String signatureKey;

    private final Boolean knoxGatewayEnabled;

    private final boolean primary;

    private final String saltSignPrivateKey;

    private final String saltSignPublicKey;

    private final String userFacingCert;

    private final String userFacingKey;

    private Boolean useCcm;

    public GatewayConfig(String connectionAddress, String publicAddress, String privateAddress,
            Integer gatewayPort, String instanceId, Boolean knoxGatewayEnabled) {
        this(connectionAddress, publicAddress, privateAddress, null, gatewayPort,
                instanceId, null, null, null, null, null, null, knoxGatewayEnabled, true, null, null, null, null, null);
    }

    public GatewayConfig(String connectionAddress, String publicAddress, String privateAddress, String hostname,
            Integer gatewayPort, String instanceId, String serverCert, String clientCert, String clientKey, String saltPassword, String saltBootPassword,
            String signatureKey, Boolean knoxGatewayEnabled, boolean primary, String saltSignPrivateKey, String saltSignPublicKey,
            String userFacingCert, String userFacingKey, Boolean useCcm) {
        this.connectionAddress = connectionAddress;
        this.publicAddress = publicAddress;
        this.privateAddress = privateAddress;
        this.hostname = hostname;
        this.gatewayPort = gatewayPort;
        this.instanceId = instanceId;
        this.serverCert = serverCert;
        this.clientCert = clientCert;
        this.clientKey = clientKey;
        this.saltPassword = saltPassword;
        this.saltBootPassword = saltBootPassword;
        this.signatureKey = signatureKey;
        this.knoxGatewayEnabled = knoxGatewayEnabled;
        this.primary = primary;
        this.saltSignPrivateKey = saltSignPrivateKey;
        this.saltSignPublicKey = saltSignPublicKey;
        this.userFacingCert = userFacingCert;
        this.userFacingKey = userFacingKey;
        this.useCcm = useCcm;
    }

    private GatewayConfig(GatewayConfig gatewayConfig, @Nonnull ServiceEndpoint serviceEndpoint) {
        this(serviceEndpoint.getHostEndpoint().getHostAddressString(), gatewayConfig.publicAddress, gatewayConfig.privateAddress,
                gatewayConfig.hostname, Objects.requireNonNull(serviceEndpoint.getPort().orElse(null), "serviceEndpoint port unspecified"),
                gatewayConfig.instanceId,
                gatewayConfig.serverCert, gatewayConfig.clientCert, gatewayConfig.clientKey,
                gatewayConfig.saltPassword, gatewayConfig.saltBootPassword, gatewayConfig.signatureKey,
                gatewayConfig.knoxGatewayEnabled, gatewayConfig.primary,
                gatewayConfig.saltSignPrivateKey, gatewayConfig.saltSignPublicKey,
                gatewayConfig.getUserFacingCert(), gatewayConfig.getUserFacingKey(), gatewayConfig.getUseCcm());
    }

    public String getConnectionAddress() {
        return connectionAddress;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public String getPrivateAddress() {
        return privateAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public String getGatewayUrl() {
        return String.format("https://%s:%d", connectionAddress, gatewayPort);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getServerCert() {
        return serverCert;
    }

    public String getClientCert() {
        return clientCert;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getSaltPassword() {
        return saltPassword;
    }

    public String getSaltBootPassword() {
        return saltBootPassword;
    }

    public String getSignatureKey() {
        return signatureKey;
    }

    public Boolean getKnoxGatewayEnabled() {
        return knoxGatewayEnabled;
    }

    public boolean isPrimary() {
        return primary;
    }

    public String getSaltSignPrivateKey() {
        return saltSignPrivateKey;
    }

    public String getSaltSignPublicKey() {
        return saltSignPublicKey;
    }

    public String getUserFacingCert() {
        return userFacingCert;
    }

    public String getUserFacingKey() {
        return userFacingKey;
    }

    public Boolean getUseCcm() {
        return useCcm;
    }

    /**
     * Returns a gateway config for the nginx service on the gateway.
     * The returned gateway config is identical to this one, except that
     * its connection address and gateway port (and hence gateway URL)
     * are determined using the specified service endpoint finder.
     *
     * @param serviceEndpointFinder  the service endpoint finder
     * @return a gateway config for the nginx service on the gateway
     * @throws ServiceEndpointLookupException if an exception occurs looking up the endpoint
     * @throws InterruptedException           if the lookup is interrupted
     */
    public GatewayConfig getGatewayConfig(ServiceEndpointFinder serviceEndpointFinder)
            throws ServiceEndpointLookupException, InterruptedException {
        return new GatewayConfig(this, getServiceEndpoint(serviceEndpointFinder, !Boolean.TRUE.equals(getUseCcm())));
    }

    /**
     * Returns a service endpoint for the nginx service on the gateway.
     *
     * @param serviceEndpointFinder  the service endpoint finder
     * @param directAccessRequired whether the lookup should only allow direct access to the target service
     * @return a service endpoint for the nginx service on the gateway
     * @throws ServiceEndpointLookupException if an exception occurs looking up the endpoint
     * @throws InterruptedException           if the lookup is interrupted
     */
    private ServiceEndpoint getServiceEndpoint(ServiceEndpointFinder serviceEndpointFinder, boolean directAccessRequired)
            throws ServiceEndpointLookupException, InterruptedException {
        ServiceEndpointRequest<HttpsServiceEndpoint> serviceEndpointRequest =
                createServiceEndpointRequest(directAccessRequired);
        return serviceEndpointFinder.getServiceEndpoint(serviceEndpointRequest);
    }

    /**
     * Creates a service endpoint request for connecting to the nginx service on the gateway.
     *
     * @param directAccessRequired whether the lookup should only allow direct access to the target service
     * @return a service endpoint request for connecting to the specified service on the gateway
     */
    private ServiceEndpointRequest<HttpsServiceEndpoint> createServiceEndpointRequest(boolean directAccessRequired) {
        return ServiceEndpointRequest.createDefaultServiceEndpointRequest(
                instanceId, new HostEndpoint(connectionAddress), gatewayPort, ServiceFamilies.GATEWAY, directAccessRequired);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("GatewayConfig{");
        sb.append("connectionAddress='").append(connectionAddress).append('\'');
        sb.append(", publicAddress='").append(publicAddress).append('\'');
        sb.append(", privateAddress='").append(privateAddress).append('\'');
        sb.append(", hostname='").append(hostname).append('\'');
        sb.append(", gatewayPort=").append(gatewayPort);
        sb.append(", instanceId=").append(instanceId);
        sb.append(", knoxGatewayEnabled=").append(knoxGatewayEnabled);
        sb.append(", primary=").append(primary);
        sb.append('}');
        return sb.toString();
    }
}
