package com.sequenceiq.cloudbreak.sdx.paas;

import java.util.Optional;

public interface LocalPaasRemoteDataContextSupplier {

    Optional<String> getPaasSdxRemoteDataContext(String sdxCrn);
}
