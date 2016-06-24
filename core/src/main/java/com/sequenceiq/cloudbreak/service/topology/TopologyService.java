package com.sequenceiq.cloudbreak.service.topology;

import java.util.Date;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.repository.TopologyRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
@Transactional
public class TopologyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyService.class);
    private static final String DELIMITER = "_";
    private static final String TOPOLOGY_NOT_FOUND_MSG = "Topology '%s' not found.";

    @Inject
    private TopologyRepository topologyRepository;

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private TemplateRepository templateRepository;

    @Inject
    private NetworkRepository networkRepository;

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Topology get(Long id) {
        Topology topology = topologyRepository.findOne(id);
        if (topology == null) {
            throw new NotFoundException(String.format(TOPOLOGY_NOT_FOUND_MSG, id));
        } else {
            return topology;
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    public Topology create(CbUser user, Topology topology) {
        LOGGER.debug("Creating topology: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Topology savedTopology;
        topology.setOwner(user.getUserId());
        topology.setAccount(user.getAccount());
        try {
            savedTopology = topologyRepository.save(topology);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.TOPOLOGY, topology.getName(), ex);
        }
        return savedTopology;
    }

    public void delete(Long topologyId, CbUser user) {
        Topology topology = topologyRepository.findByIdInAccount(topologyId, user.getAccount());
        if (topology == null) {
            throw new NotFoundException(String.format(TOPOLOGY_NOT_FOUND_MSG, topologyId));
        }
        delete(topology, user);
    }

    private void delete(Topology topology, CbUser user) {
        LOGGER.debug("Deleting topology. {} - {}", new Object[]{topology.getId(), topology.getName()});
        Set<Credential> credentials = credentialRepository.findByTopology(topology.getId());
        Set<Template> templates = templateRepository.findByTopology(topology.getId());
        Set<Network> networks = networkRepository.findByTopology(topology.getId());
        if (credentials.isEmpty() && templates.isEmpty() && networks.isEmpty()) {
            Date now = new Date();
            String terminatedName = topology.getName() + DELIMITER + now.getTime();
            topology.setName(terminatedName);
            topology.setDeleted(true);
            topologyRepository.save(topology);
        } else {
            throw new BadRequestException(String.format("Topology '%d' is in use, cannot be deleted.", topology.getId()));
        }
    }
}
