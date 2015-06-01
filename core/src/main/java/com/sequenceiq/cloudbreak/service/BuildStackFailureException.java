package com.sequenceiq.cloudbreak.service;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Resource;

public class BuildStackFailureException extends CloudbreakServiceException {

    private final Set<Resource> resourceSet;

    public BuildStackFailureException(Exception ex) {
        super(ex);
        this.resourceSet = new HashSet<>();
    }

    public BuildStackFailureException(String message, Throwable cause) {
        super(message, cause);
        this.resourceSet = new HashSet<>();
    }

    public BuildStackFailureException(String message, Throwable cause, Set<Resource> resourceSet) {
        super(message, cause);
        this.resourceSet = resourceSet;
    }

    public Set<Resource> getResourceSet() {
        return resourceSet;
    }
}
