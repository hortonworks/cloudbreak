package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.io.BaseEncoding;

public class DefaultCcmV2Parameters implements CcmV2Parameters, Serializable {

    private static final long serialVersionUID = 1L;

    private String invertingProxyHost;

    private String invertingProxyCertificate;

    private String agentCrn;

    private String agentKeyId;

    private String agentCertificate;

    private String agentEncipheredPrivateKey;

    private String clusterGatewayDomain;

    public DefaultCcmV2Parameters(@Nonnull String invertingProxyHost, @Nonnull String invertingProxyCertificate, @Nonnull String agentCrn,
            @Nonnull String agentKeyId, @Nonnull String agentEncipheredPrivateKey, @Nonnull String agentCertificate, @Nonnull String clusterGatewayDomain) {
        this.invertingProxyHost = invertingProxyHost;
        this.invertingProxyCertificate = invertingProxyCertificate;
        this.agentCrn = agentCrn;
        this.agentKeyId = agentKeyId;
        this.agentEncipheredPrivateKey = agentEncipheredPrivateKey;
        this.agentCertificate = agentCertificate;
        this.clusterGatewayDomain = clusterGatewayDomain;
    }

    public String getInvertingProxyCertificate() {
        return invertingProxyCertificate;
    }

    public String getInvertingProxyHost() {
        return invertingProxyHost;
    }

    public String getAgentCertificate() {
        return agentCertificate;
    }

    public String getAgentKeyId() {
        return agentKeyId;
    }

    public String getAgentCrn() {
        return agentCrn;
    }

    public String getAgentEncipheredPrivateKey() {
        return agentEncipheredPrivateKey;
    }

    public String getClusterGatewayDomain() {
        return clusterGatewayDomain;
    }

    public void addToTemplateModel(Map<String, Object> model) {
        model.put(CcmV2ParameterConstants.CCMV2_INVERTING_PROXY_HOST, getInvertingProxyHost());
        model.put(CcmV2ParameterConstants.CCMV2_INVERTING_PROXY_CERTIFICATE,
                BaseEncoding.base64().encode(getInvertingProxyCertificate().getBytes(StandardCharsets.UTF_8)));

        model.put(CcmV2ParameterConstants.CCMV2_AGENT_KEY_ID, getAgentKeyId());
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_CRN, getAgentCrn());
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_ENCIPHERED_KEY,
                BaseEncoding.base64().encode(getAgentEncipheredPrivateKey().getBytes(StandardCharsets.UTF_8)));
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_CERTIFICATE,
                BaseEncoding.base64().encode(getAgentCertificate().getBytes(StandardCharsets.UTF_8)));

        model.put(CcmV2ParameterConstants.CCMV2_CLUSTER_DOMAIN, getClusterGatewayDomain());
    }
}
