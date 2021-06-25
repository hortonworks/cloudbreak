package com.sequenceiq.cloudbreak.ccm.cloudinit;

import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_BACKEND_ID_FORMAT;

import java.io.Serializable;
import java.util.Map;

import javax.annotation.Nonnull;

public class DefaultCcmV2Parameters implements CcmV2JumpgateParameters, Serializable {

    private static final long serialVersionUID = 1L;

    private final String invertingProxyHost;

    private final String invertingProxyCertificate;

    private final String agentCrn;

    private final String agentKeyId;

    private final String agentCertificate;

    private final String agentEncipheredPrivateKey;

    public DefaultCcmV2Parameters(@Nonnull String invertingProxyHost, @Nonnull String invertingProxyCertificate, @Nonnull String agentCrn,
            @Nonnull String agentKeyId, @Nonnull String agentEncipheredPrivateKey, @Nonnull String agentCertificate) {
        this.invertingProxyHost = invertingProxyHost;
        this.invertingProxyCertificate = invertingProxyCertificate;
        this.agentCrn = agentCrn;
        this.agentKeyId = agentKeyId;
        this.agentEncipheredPrivateKey = agentEncipheredPrivateKey;
        this.agentCertificate = agentCertificate;
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

    public void addToTemplateModel(Map<String, Object> model) {
        model.put(CcmV2ParameterConstants.CCMV2_INVERTING_PROXY_HOST, getInvertingProxyHost());
        model.put(CcmV2ParameterConstants.CCMV2_INVERTING_PROXY_CERTIFICATE, getInvertingProxyCertificate());

        model.put(CcmV2ParameterConstants.CCMV2_AGENT_KEY_ID, getAgentKeyId());
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_CRN, getAgentCrn());
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_ENCIPHERED_KEY, getAgentEncipheredPrivateKey());
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_CERTIFICATE, getAgentCertificate());
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_BACKEND_ID_PREFIX,
                String.format(CCMV2_BACKEND_ID_FORMAT, getAgentCrn(), ""));
    }
}
