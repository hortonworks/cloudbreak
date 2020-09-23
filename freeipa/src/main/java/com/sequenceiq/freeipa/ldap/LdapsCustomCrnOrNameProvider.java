package com.sequenceiq.freeipa.ldap;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResource;
import com.sequenceiq.freeipa.events.AbstractCustomCrnOrNameProvider;

@Component
public class LdapsCustomCrnOrNameProvider extends AbstractCustomCrnOrNameProvider {

    @Inject
    private LdapConfigService ldapConfigService;

    @Override
    protected List<? extends AccountAwareResource> getResource(String environmentCrn, String accountId) {
        return ldapConfigService.findAllByEnvironmentAndAccountIdEvenIfArchived(environmentCrn, accountId);
    }
}
