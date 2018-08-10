package com.sequenceiq.cloudbreak.service.ldapconfig;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
import com.sequenceiq.cloudbreak.repository.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Service
public class LdapConfigService extends AbstractOrganizationAwareResourceService<LdapConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigService.class);

    @Inject
    private LdapConfigRepository ldapConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private OrganizationService organizationService;

    public LdapConfig create(IdentityUser identityUser, LdapConfig ldapConfig) {
        ldapConfig.setOwner(identityUser.getUserId());
        ldapConfig.setAccount(identityUser.getAccount());
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        ldapConfig.setOrganization(organization);
        try {
            return ldapConfigRepository.save(ldapConfig);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.LDAP_CONFIG, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public LdapConfig get(Long id) {
        return ldapConfigRepository.findById(id).orElseThrow(notFound("LdapConfig", id));
    }

    public void delete(Long id) {
        delete(get(id));
    }

    @Override
    protected OrganizationResourceRepository<LdapConfig, Long> repository() {
        return ldapConfigRepository;
    }

    @Override
    protected OrganizationResource resource() {
        return OrganizationResource.LDAP;
    }

    @Override
    protected boolean canDelete(LdapConfig ldapConfig) {
        LOGGER.info("Deleting ldap configuration with name: {}", ldapConfig.getName());
        List<Cluster> clustersWithLdap = clusterService.findByLdapConfig(ldapConfig);
        if (!clustersWithLdap.isEmpty()) {
            if (clustersWithLdap.size() > 1) {
                String clusters = clustersWithLdap
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with LDAP config '%s'. Please remove these before deleting the LDAP config. "
                                + "The following clusters are using this LDAP: [%s]", ldapConfig.getName(), clusters));
            } else {
                throw new BadRequestException(String.format("There is a cluster ['%s'] which uses LDAP config '%s'. Please remove this "
                        + "cluster before deleting the LDAP config", clustersWithLdap.get(0).getName(), ldapConfig.getName()));
            }
        }
        return true;
    }

    @Override
    protected void prepareCreation(LdapConfig resource) {

    }

}
