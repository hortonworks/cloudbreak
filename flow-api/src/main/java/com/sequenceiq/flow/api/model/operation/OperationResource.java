package com.sequenceiq.flow.api.model.operation;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public enum OperationResource {
    UNKNOWN, ENVIRONMENT, FREEIPA, DATALAKE, DATAHUB, REMOTEDB;

    public static OperationResource fromCrn(Crn crn) {
        Crn.Service service = crn.getService();
        if (Crn.Service.DATALAKE.equals(service)) {
            return DATALAKE;
        } else if (Crn.Service.DATAHUB.equals(service)) {
            return DATAHUB;
        } else if (Crn.Service.ENVIRONMENTS.equals(service)) {
            return ENVIRONMENT;
        } else if (Crn.Service.FREEIPA.equals(service)) {
            return FREEIPA;
        } else if (Crn.Service.REDBEAMS.equals(service)) {
            return REMOTEDB;
        } else {
            return UNKNOWN;
        }
    }

}
