package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.util.Optional;

public interface CcmV2ParameterSupplier {

    CcmV2Parameters getCcmV2Parameters(String accountId, Optional<String> environmentCrnOpt, String domainName, String agentKeyId);
}