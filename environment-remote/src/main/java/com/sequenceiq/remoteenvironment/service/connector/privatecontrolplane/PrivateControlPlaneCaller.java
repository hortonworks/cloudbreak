package com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane;

import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;

@FunctionalInterface
public interface PrivateControlPlaneCaller<T> {

    T call(PrivateControlPlane controlPlane, String environmentCrn, String actorCrn);
}
