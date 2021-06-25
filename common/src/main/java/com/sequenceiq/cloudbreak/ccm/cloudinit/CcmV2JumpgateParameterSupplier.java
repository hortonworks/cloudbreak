package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.util.Optional;

public interface CcmV2JumpgateParameterSupplier {

    CcmV2JumpgateParameters getCcmV2JumpgateParameters(String accountId, Optional<String> environmentCrnOpt, String domainName, String agentKeyId);
}
