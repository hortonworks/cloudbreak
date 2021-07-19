package com.sequenceiq.redbeams.domain;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public interface RedbeamsResourceCrnAndNameView {

    String getName();

    Crn getResourceCrn();
}
