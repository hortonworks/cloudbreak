package com.sequenceiq.freeipa.kerberos;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.freeipa.events.AbstractCustomCrnOrNameProvider;

@Component
public class KerberosCustomCrnOrNameProvider extends AbstractCustomCrnOrNameProvider {

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Override
    protected List<? extends AccountAwareResource> getResource(String environmentCrn, String accountId) {
        return kerberosConfigService.findAllInEnvironmentEvenIfArchived(environmentCrn, accountId);
    }
}
