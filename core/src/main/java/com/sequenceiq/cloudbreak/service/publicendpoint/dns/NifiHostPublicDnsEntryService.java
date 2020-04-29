package com.sequenceiq.cloudbreak.service.publicendpoint.dns;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiConfigProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;

@Service
public class NifiHostPublicDnsEntryService extends BaseDnsEntryService {

    @Inject
    private ComponentLocatorService componentLocatorService;

    @Inject
    private NifiConfigProvider nifiConfigProvider;

    @Override
    protected Map<String, List<String>> getComponentLocation(Stack stack) {
        return componentLocatorService.getComponentLocation(stack.getCluster(), nifiConfigProvider.getRoleTypes());
    }

    @Override
    protected String logName() {
        return "NiFi nodes";
    }
}
