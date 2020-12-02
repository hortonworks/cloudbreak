package com.sequenceiq.cloudbreak.ccm.cloudinit;

public interface CcmV2ParameterSupplier {

    CcmV2Parameters getCcmV2Parameters(String accountId, String domainName, String agentKeyId);
}