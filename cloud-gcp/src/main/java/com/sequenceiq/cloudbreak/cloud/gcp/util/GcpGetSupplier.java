package com.sequenceiq.cloudbreak.cloud.gcp.util;

import java.io.IOException;

@FunctionalInterface
public interface GcpGetSupplier<T> {

    T get() throws IOException;
}
