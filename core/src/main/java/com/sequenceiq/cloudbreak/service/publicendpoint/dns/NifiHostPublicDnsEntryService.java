package com.sequenceiq.cloudbreak.service.publicendpoint.dns;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiConfigProvider;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;

public class NifiHostPublicDnsEntryService extends BaseDnsEntryService {

    @Inject
    private ComponentLocatorService componentLocatorService;

    @Inject
    private NifiConfigProvider nifiConfigProvider;

    @Override
    protected Map<String, List<String>> getComponentLocation(StackDtoDelegate stack) {
        return componentLocatorService.getComponentLocation(stack, nifiConfigProvider.getRoleTypes());
    }

    @Override
    protected String logName() {
        return "NiFi nodes";
    }
}
