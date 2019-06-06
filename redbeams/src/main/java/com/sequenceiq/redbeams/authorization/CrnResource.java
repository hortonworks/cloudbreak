package com.sequenceiq.redbeams.authorization;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

/**
 * A resource that can be referenced by CRN. This is important for authorization
 * checks on entities which can be represented by CRN.
 */
public interface CrnResource {

    String getAccountId();

    Crn getResourceCrn();

}
