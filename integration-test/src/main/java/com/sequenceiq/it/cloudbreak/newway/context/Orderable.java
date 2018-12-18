package com.sequenceiq.it.cloudbreak.newway.context;

public interface Orderable {

    default int order() {
        return 0;
    }
}
