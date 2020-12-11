package com.sequenceiq.it.cloudbreak.context;

public interface Orderable {

    default int order() {
        return 200;
    }
}
