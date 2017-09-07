package com.sequenceiq.cloudbreak.service.topology;

import java.util.Date;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.repository.TopologyRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
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

    @Inject
    private AuthorizationService authorizationService;

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Topology get(Long id) {
        return getById(id);
    }

    public Topology getById(Long id) {
        Topology topology = topologyRepository.findOne(id);
        if (topology == null) {
            throw new NotFoundException(String.format(TOPOLOGY_NOT_FOUND_MSG, id));
        } else {
            return topology;
        }
    }

    @Transactional(TxType.NEVER)
    public Topology create(IdentityUser user, Topology topology) {
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

    public void delete(Long topologyId, IdentityUser user) {
        Topology topology = topologyRepository.findByIdInAccount(topologyId, user.getAccount());
        if (topology == null) {
            throw new NotFoundException(String.format(TOPOLOGY_NOT_FOUND_MSG, topologyId));
        }
        delete(topology);
    }

    private void delete(Topology topology) {
        authorizationService.hasWritePermission(topology);
        if (0L != credentialRepository.countByTopology(topology) + templateRepository.countByTopology(topology) + networkRepository.countByTopology(topology)) {
            throw new BadRequestException(String.format("Topology '%d' is in use, cannot be deleted.", topology.getId()));
        }
        LOGGER.debug("Deleting topology. {} - {}", new Object[]{topology.getId(), topology.getName()});
        Date now = new Date();
        String terminatedName = topology.getName() + DELIMITER + now.getTime();
        topology.setName(terminatedName);
        topology.setDeleted(true);
        topologyRepository.save(topology);
    }
}
