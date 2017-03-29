package com.sequenceiq.cloudbreak.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayJson {

    @ApiModelProperty(value = ModelDescriptions.GatewayModelDescription.ENABLE_KNOX_GATEWAY)
    private Boolean enableGateway;

    @ApiModelProperty(value = ModelDescriptions.GatewayModelDescription.KNOX_PATH)
    private String path;

    @ApiModelProperty(value = ModelDescriptions.GatewayModelDescription.KNOX_TOPOLOGY_NAME)
    private String topologyName;

    @ApiModelProperty(value = ModelDescriptions.GatewayModelDescription.EXPOSED_KNOX_SERVICES)
    private List<String> exposedServices;

    @ApiModelProperty(value = ModelDescriptions.GatewayModelDescription.KNOX_SSO_PROVIDER)
    private String ssoProvider;

    @ApiModelProperty(value = ModelDescriptions.GatewayModelDescription.KNOX_SSO_CERT)
    private String signCert;

    @ApiModelProperty(value = ModelDescriptions.GatewayModelDescription.KNOX_SSO_PUB_KEY)
    private String signPub;

    @ApiModelProperty(value = ModelDescriptions.GatewayModelDescription.KNOX_GATEWAY_TYPE)
    private GatewayType gatewayType;

    @ApiModelProperty(value = ModelDescriptions.GatewayModelDescription.KNOX_SSO_TYPE)
    private SSOType ssoType;

    public Boolean getEnableGateway() {
        return enableGateway;
    }

    public void setEnableGateway(Boolean enableGateway) {
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

    public String getSignCert() {
        return signCert;
    }

    public void setSignCert(String signCert) {
        this.signCert = signCert;
    }

    public String getSignPub() {
        return signPub;
    }

    public void setSignPub(String signPub) {
        this.signPub = signPub;
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
}
