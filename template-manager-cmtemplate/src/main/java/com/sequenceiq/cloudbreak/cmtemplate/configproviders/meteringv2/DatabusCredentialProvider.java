package com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2;

import java.io.IOException;

import com.sequenceiq.cloudbreak.template.views.DatabusCredentialView;

public interface DatabusCredentialProvider {

    DatabusCredentialView getOrCreateDatabusCredential(String crn) throws IOException;
}
