package com.sequenceiq.cloudbreak.ccm.cloudinit;

import static com.sequenceiq.common.api.type.InstanceGroupType.isGateway;

import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.type.InstanceGroupType;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface CcmV2JumpgateParameters extends CcmV2Parameters {
    static void addToTemplateModel(InstanceGroupType type, @Nullable CcmV2JumpgateParameters ccmV2JumpgateParameters, @Nonnull Map<String, Object> model) {
        if (ccmV2JumpgateParameters == null || !isGateway(type)) {
            model.put(CcmV2ParameterConstants.CCM_V2_ENABLED_KEY, Boolean.FALSE);
            model.put(CcmV2JumpgateParameterConstants.CCMV2_JUMPGATE_ENABLED_KEY, Boolean.FALSE);
        } else {
            model.put(CcmV2ParameterConstants.CCM_V2_ENABLED_KEY, Boolean.TRUE);
            model.put(CcmV2JumpgateParameterConstants.CCMV2_JUMPGATE_ENABLED_KEY, Boolean.TRUE);
            ccmV2JumpgateParameters.addToTemplateModel(model);
        }
    }

    String getEnvironmentCrn();

    String getAgentMachineUserAccessKey();

    String getAgentMachineUserEncipheredAccessKey();

    String getHmacKey();

    String getInitialisationVector();

    String getHmacForPrivateKey();
}
