package com.sequenceiq.cloudbreak.cmtemplate;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;

public interface EntitledForServiceScale {

    Entitlement getEntitledFor();
}
