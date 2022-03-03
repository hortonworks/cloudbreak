package com.sequenceiq.cloudbreak.ccm.cloudinit;

import static com.sequenceiq.common.api.type.InstanceGroupType.isGateway;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sequenceiq.common.api.type.InstanceGroupType;

public interface CcmV2JumpgateParameters extends CcmV2Parameters {
    static void addToTemplateModel(InstanceGroupType type, @Nullable CcmV2JumpgateParameters ccmV2JumpgateParameters, @Nonnull Map<String, Object> model) {
        if (ccmV2JumpgateParameters == null || !isGateway(type)) {
            model.put(CcmV2ParameterConstants.CCM_V2_ENABLED_KEY, Boolean.FALSE);
            model.put(CcmV2JumpgateParameterConstants.CCM_V2_JUMPGATE_ENABLED_KEY, Boolean.FALSE);
        } else {
            model.put(CcmV2ParameterConstants.CCM_V2_ENABLED_KEY, Boolean.TRUE);
            model.put(CcmV2JumpgateParameterConstants.CCM_V2_JUMPGATE_ENABLED_KEY, Boolean.TRUE);
            ccmV2JumpgateParameters.addToTemplateModel(model);
        }
    }

    String getEnvironmentCrn();

    String getAgentMachineUserAccessKey();

    String getAgentMachineUserEncipheredAccessKey();
}
