package com.sequenceiq.cloudbreak.sdx.paas;

import java.util.Optional;

public interface PaasRemoteDataContextSupplier {

    Optional<String> getPaasSdxRemoteDataContext(String sdxCrn);
}
