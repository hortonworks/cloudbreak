package com.sequenceiq.remoteenvironment.api.v1.controlplane.endpoint;

public class PrivateControlPlaneOpDescription {

    public static final String REGISTER = "Register a private control plane.";

    public static final String CONTROL_PLANE_NOTES = "CDP Private Cloud Base supports a variety of hybrid solutions where " +
            "compute tasks are separated from data storage and where data can be accessed from remote clusters, " +
            "including workloads created using CDP Private Cloud Data Services. This hybrid approach provides a " +
            "foundation for containerized applications by managing storage, table schema, authentication, " +
            "authorization and governance.";

    public static final String DEREGISTER = "Deregister a private control plane.";

    private PrivateControlPlaneOpDescription() {

    }
}
