package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.util.Map;

import javax.annotation.Nonnull;

public class DefaultCcmV2JumpgateParameters extends DefaultCcmV2Parameters implements CcmV2JumpgateParameters {

    private final String agentMachineUserAccessKey;

    private final String agentMachineUserEncipheredAccessKey;

    public DefaultCcmV2JumpgateParameters(@Nonnull String invertingProxyHost, @Nonnull String invertingProxyCertificate, @Nonnull String agentCrn,
            @Nonnull String agentKeyId, String agentEncipheredPrivateKey, String agentCertificate,
            String agentMachineUserAccessKey, String agentMachineUserEncipheredAccessKey) {
        super(invertingProxyHost, invertingProxyCertificate, agentCrn, agentKeyId, agentEncipheredPrivateKey, agentCertificate);
        this.agentMachineUserAccessKey = agentMachineUserAccessKey;
        this.agentMachineUserEncipheredAccessKey = agentMachineUserEncipheredAccessKey;
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
    public void addToTemplateModel(@Nonnull Map<String, Object> model) {
        super.addToTemplateModel(model);
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_MACHINE_USER_ACCESS_KEY_ID, getAgentMachineUserAccessKey());
        model.put(CcmV2ParameterConstants.CCMV2_AGENT_MACHINE_USER_ENCIPHERED_ACCESS_KEY, getAgentMachineUserEncipheredAccessKey());
    }
}
