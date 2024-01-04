package com.sequenceiq.cloudbreak.ccm.cloudinit;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Nonnull;

public class DefaultCcmV2JumpgateParameters extends DefaultCcmV2Parameters implements CcmV2JumpgateParameters {

    private final String environmentCrn;

    private final String agentMachineUserAccessKey;

    private final String agentMachineUserEncipheredAccessKey;

    private final String hmacKey;

    private final String initialisationVector;

    private final String hmacForPrivateKey;

    public DefaultCcmV2JumpgateParameters(@Nonnull String invertingProxyHost, @Nonnull String invertingProxyCertificate, @Nonnull String agentCrn,
            @Nonnull String agentKeyId, String agentEncipheredPrivateKey, String agentCertificate, String environmentCrn,
            String agentMachineUserAccessKey, String agentMachineUserEncipheredAccessKey,
            String hmacKey, String initialisationVector, String hmacForPrivateKey) {
        super(invertingProxyHost, invertingProxyCertificate, agentCrn, agentKeyId, agentEncipheredPrivateKey, agentCertificate);
        this.environmentCrn = Objects.requireNonNullElse(environmentCrn, EMPTY);
        this.agentMachineUserAccessKey = Objects.requireNonNullElse(agentMachineUserAccessKey, EMPTY);
        this.agentMachineUserEncipheredAccessKey = Objects.requireNonNullElse(agentMachineUserEncipheredAccessKey, EMPTY);
        this.hmacKey = Objects.requireNonNullElse(hmacKey, EMPTY);
        this.initialisationVector = Objects.requireNonNullElse(initialisationVector, EMPTY);
        this.hmacForPrivateKey = Objects.requireNonNullElse(hmacForPrivateKey, EMPTY);
    }

    public DefaultCcmV2JumpgateParameters() {
        this(EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY);
    }

    @Override
    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    @Override
    public String getAgentMachineUserAccessKey() {
        return agentMachineUserAccessKey;
    }

    @Override
    public String getAgentMachineUserEncipheredAccessKey() {
        return agentMachineUserEncipheredAccessKey;
    }

    @Override
    public String getHmacKey() {
        return hmacKey;
    }

    @Override
    public String getInitialisationVector() {
        return initialisationVector;
    }

    @Override
    public String getHmacForPrivateKey() {
        return hmacForPrivateKey;
    }

    @Override
    public void addToTemplateModel(@Nonnull Map<String, Object> model) {
        super.addToTemplateModel(model);
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_MACHINE_USER_ACCESS_KEY_ID, getAgentMachineUserAccessKey());
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_MACHINE_USER_ENCIPHERED_ACCESS_KEY, getAgentMachineUserEncipheredAccessKey());
        model.put(CcmV2JumpgateParameterConstants.CCMV2_AGENT_HMAC_KEY, getHmacKey());
        model.put(CcmV2JumpgateParameterConstants.CCMV2_AGENT_IV, getInitialisationVector());
        model.put(CcmV2JumpgateParameterConstants.CCMV2_AGENT_HMAC_FOR_PRIVATE_KEY, getHmacForPrivateKey());
    }
}
