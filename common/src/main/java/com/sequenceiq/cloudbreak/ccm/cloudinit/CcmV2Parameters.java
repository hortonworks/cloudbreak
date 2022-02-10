package com.sequenceiq.cloudbreak.ccm.cloudinit;

import static com.sequenceiq.common.api.type.InstanceGroupType.isGateway;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.sequenceiq.common.api.type.InstanceGroupType;

public interface CcmV2Parameters {

    static void addToTemplateModel(InstanceGroupType type, @Nullable CcmV2Parameters ccmV2Parameters, @Nonnull Map<String, Object> model) {
        if (ccmV2Parameters == null || !isGateway(type)) {
            model.put(CcmV2ParameterConstants.CCM_V2_ENABLED_KEY, Boolean.FALSE);
        } else {
            model.put(CcmV2ParameterConstants.CCM_V2_ENABLED_KEY, Boolean.TRUE);
            ccmV2Parameters.addToTemplateModel(model);
        }
    }

    String getInvertingProxyCertificate();

    String getInvertingProxyHost();

    String getAgentCertificate();

    String getAgentKeyId();

    String getAgentEncipheredPrivateKey();

    String getAgentCrn();

    String getAgentBackendIdPrefix();

    void addToTemplateModel(@Nonnull Map<String, Object> model);
}
