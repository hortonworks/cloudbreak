package com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.SSOType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.GatewayModelDescription;
import com.sequenceiq.cloudbreak.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.json.Base64Serializer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GatewayJson implements JsonEntity {

    /**
     * @deprecated enableGateway is no longer needed to determine if gateway needs to be launched or not.
     * Presence of gateway definition in request is suffucicient.
     */
    @Deprecated
    @ApiModelProperty(GatewayModelDescription.ENABLE_KNOX_GATEWAY)
    private boolean enableGateway;

    @ApiModelProperty(GatewayModelDescription.KNOX_PATH)
    private String path;

    /**
     * @deprecated Use the Knox topology name inside the 'gateway' part of the request.
     */
    @Deprecated
    @ApiModelProperty(GatewayModelDescription.DEPRECATED_KNOX_TOPOLOGY_NAME)
    private String topologyName;

    @ApiModelProperty(GatewayModelDescription.GATEWAY_TOPOLOGIES)
    private List<GatewayTopologyJson> topologies;

    /**
     * @deprecated Use the 'exposed Knox services' inside the 'gateway' part of the request.
     */
    @Deprecated
    @ApiModelProperty(GatewayModelDescription.DEPRECATED_EXPOSED_KNOX_SERVICES)
    private List<String> exposedServices;

    @ApiModelProperty(GatewayModelDescription.KNOX_SSO_PROVIDER)
    private String ssoProvider;

    @ApiModelProperty(GatewayModelDescription.KNOX_SSO_CERT)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String tokenCert;

    @ApiModelProperty(GatewayModelDescription.KNOX_GATEWAY_TYPE)
    private GatewayType gatewayType;

    @ApiModelProperty(GatewayModelDescription.KNOX_SSO_TYPE)
    private SSOType ssoType;

    public boolean getEnableGateway() {
        return enableGateway;
    }

    public void setEnableGateway(boolean enableGateway) {
        this.enableGateway = enableGateway;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTopologyName() {
        return topologyName;
    }

    public void setTopologyName(String topologyName) {
        this.topologyName = topologyName;
    }

    public List<String> getExposedServices() {
        return exposedServices;
    }

    public void setExposedServices(List<String> exposedServices) {
        this.exposedServices = exposedServices;
    }

    public String getSsoProvider() {
        return ssoProvider;
    }

    public void setSsoProvider(String ssoProvider) {
        this.ssoProvider = ssoProvider;
    }

    public String getTokenCert() {
        return tokenCert;
    }

    public void setTokenCert(String tokenCert) {
        this.tokenCert = tokenCert;
    }

    public GatewayType getGatewayType() {
        return gatewayType;
    }

    public void setGatewayType(GatewayType gatewayType) {
        this.gatewayType = gatewayType;
    }

    public SSOType getSsoType() {
        return ssoType;
    }

    public void setSsoType(SSOType ssoType) {
        this.ssoType = ssoType;
    }

    public List<GatewayTopologyJson> getTopologies() {
        return topologies;
    }

    public void setTopologies(List<GatewayTopologyJson> topologies) {
        this.topologies = topologies;
    }
}