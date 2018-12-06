package com.sequenceiq.cloudbreak.structuredevent.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;

@Component
public class LdapConfigToLdapDetailsConverter extends AbstractConversionServiceAwareConverter<LdapConfig, LdapDetails> {

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public LdapDetails convert(LdapConfig source) {
        LdapDetails ldapDetails = new LdapDetails();
        ldapDetails.setId(source.getId());
        ldapDetails.setAdminGroup(source.getAdminGroup());
        ldapDetails.setCertificate(source.getCertificate());
        ldapDetails.setDescription(source.getDescription());
        ldapDetails.setDirectoryType(source.getDirectoryType().name());
        ldapDetails.setDomain(source.getDomain());
        ldapDetails.setGroupMemberAttribute(source.getGroupMemberAttribute());
        ldapDetails.setGroupObjectClass(source.getGroupObjectClass());
        ldapDetails.setGroupNameAttribute(source.getGroupNameAttribute());
        ldapDetails.setGroupSearchBase(source.getGroupSearchBase());
        ldapDetails.setName(source.getName());
        ldapDetails.setProtocol(source.getProtocol());
        ldapDetails.setServerHost(source.getServerHost());
        ldapDetails.setServerPort(source.getServerPort());
        ldapDetails.setUserDnPattern(source.getUserDnPattern());
        ldapDetails.setUserNameAttribute(source.getUserNameAttribute());
        ldapDetails.setUserObjectClass(source.getUserObjectClass());
        ldapDetails.setUserSearchBase(source.getUserSearchBase());
        if (source.getWorkspace() != null) {
            ldapDetails.setWorkspaceId(source.getWorkspace().getId());
        } else {
            ldapDetails.setWorkspaceId(restRequestThreadLocalService.getRequestedWorkspaceId());
        }
        ldapDetails.setUserName(restRequestThreadLocalService.getCloudbreakUser().getUsername());
        ldapDetails.setUserId(restRequestThreadLocalService.getCloudbreakUser().getUserId());
        ldapDetails.setTenantName(restRequestThreadLocalService.getCloudbreakUser().getTenant());
        return ldapDetails;
    }
}
