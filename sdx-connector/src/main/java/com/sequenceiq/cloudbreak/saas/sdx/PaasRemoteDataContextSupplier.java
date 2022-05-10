package com.sequenceiq.cloudbreak.saas.sdx;

import java.util.Optional;

public interface PaasRemoteDataContextSupplier {

    Optional<String> getPaasSdxRemoteDataContext(String sdxCrn);
}
