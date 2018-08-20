package com.sequenceiq.cloudbreak.service.topology;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.repository.TopologyRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Service
public class TopologyService extends AbstractOrganizationAwareResourceService<Topology> {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyService.class);

    private static final String DELIMITER = "_";

    @Inject
    private TopologyRepository topologyRepository;

    @Inject
    private CredentialService credentialService;

    @Inject
    private TemplateService templateService;

    @Inject
    private NetworkService networkService;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private OrganizationService organizationService;

    public Topology get(Long id) {
        return topologyRepository.findById(id).orElseThrow(notFound("Topology", id));
    }

    public Topology create(IdentityUser user, Topology topology, Organization organization) {
        LOGGER.debug("Creating topology: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Topology savedTopology;
        if (organization != null) {
            topology.setOrganization(organization);
        } else {
            topology.setOrganization(organizationService.getDefaultOrganizationForCurrentUser());
        }
        try {
            savedTopology = topologyRepository.save(topology);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.TOPOLOGY, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return savedTopology;
    }

    @Override
    protected OrganizationResourceRepository<Topology, Long> repository() {
        return topologyRepository;
    }

    @Override
    protected OrganizationResource resource() {
        return OrganizationResource.TOPOLOGY;
    }

    @Override
    protected void prepareDeletion(Topology resource) {

    }

    @Override
    protected void prepareCreation(Topology resource) {

    }
}
