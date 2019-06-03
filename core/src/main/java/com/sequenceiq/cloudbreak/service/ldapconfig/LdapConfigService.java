package com.sequenceiq.cloudbreak.service.ldapconfig;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.Set;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapMinimalV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
import com.sequenceiq.cloudbreak.service.AbstractArchivistService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

public class LdapConfigService extends AbstractArchivistService<LdapConfig> {

    @Inject
    private LdapConfigRepository ldapConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    public LdapConfig get(Long id) {
        return ldapConfigRepository.findById(id).orElseThrow(notFound("LdapConfig", id));
    }

    public void delete(Long id) {
        delete(get(id));
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.LDAP;
    }

    public String testConnection(Long workspaceId, String existingName, LdapMinimalV4Request existingLdapConfig) {
        if (existingName == null && existingLdapConfig == null) {
            throw new BadRequestException("Either an existing resource 'name' or an LDAP 'validationRequest' needs to be specified in the request. ");
        }
        try {
            if (existingName != null) {
                LdapConfig ldapConfig = getByNameForWorkspaceId(existingName, workspaceId);
                ldapConfigValidator.validateLdapConnection(ldapConfig);
            } else {
                ldapConfigValidator.validateLdapConnection(existingLdapConfig);
            }
            return "connected";
        } catch (BadRequestException e) {
            return e.getMessage();
        }
    }

    public Set<LdapConfig> findAllInWorkspace(Long workspaceId) {
        return ldapConfigRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    protected WorkspaceResourceRepository<LdapConfig, Long> repository() {
        return ldapConfigRepository;
    }

    @Override
    protected void prepareDeletion(LdapConfig resource) {
//        Set<Cluster> clustersWithThisProxy = clusterService.findByLdapConfig(resource);
//        if (!clustersWithThisProxy.isEmpty()) {
//            String clusters = clustersWithThisProxy
//                    .stream()
//                    .map(Cluster::getName)
//                    .collect(Collectors.joining(", "));
//            throw new BadRequestException(String.format(resource().getReadableName() + " '%s' cannot be deleted"
//                    + " because there are clusters associated with it: [%s].", resource.getName(), clusters));
//        }
    }

    @Override
    protected void prepareCreation(LdapConfig resource) {

    }
}
