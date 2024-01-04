package com.sequenceiq.cloudbreak.ccm.cloudinit;

import static com.sequenceiq.common.api.type.InstanceGroupType.isGateway;

import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.common.api.type.InstanceGroupType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({ @Type(value = DefaultCcmV2JumpgateParameters.class, name = "ccmV2JumpgateParameters"),
        @Type(value = DefaultCcmV2Parameters.class, name = "ccmV2Parameters") })
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

    @JsonIgnore
    String getAgentBackendIdPrefix();

    void addToTemplateModel(@Nonnull Map<String, Object> model);
}
