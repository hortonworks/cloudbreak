package com.sequenceiq.remoteenvironment.service;

import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;

@FunctionalInterface
public interface RemoteEnvironmentCaller<T> {

    T call(PrivateControlPlane controlPlane, String environmentCrn, String actorCrn);
}
