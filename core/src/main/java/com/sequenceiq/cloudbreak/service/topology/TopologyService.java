package com.sequenceiq.cloudbreak.service.topology;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.repository.TopologyRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
public class TopologyService {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyService.class);

    private static final String DELIMITER = "_";

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

    public Topology get(Long id) {
        return topologyRepository.findById(id).orElseThrow(notFound("Topology", id));
    }

    public Set<Topology> findAllInAccount(String account) {
        return topologyRepository.findAllInAccount(account);
    }

    public Topology create(IdentityUser user, Topology topology) {
        LOGGER.debug("Creating topology: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Topology savedTopology;
        topology.setOwner(user.getUserId());
        topology.setAccount(user.getAccount());
        try {
            savedTopology = topologyRepository.save(topology);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.TOPOLOGY, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return savedTopology;
    }

    public void delete(Long topologyId, IdentityUser user) {
        Topology topology = Optional.ofNullable(topologyRepository.findByIdInAccount(topologyId, user.getAccount()))
                .orElseThrow(notFound("Topology", topologyId));
        delete(topology);
    }

    private void delete(Topology topology) {
        Set<Credential> credentialsByTopology = credentialRepository.findByTopology(topology);
        Set<Template> templatesByTopology = templateRepository.findByTopology(topology);
        Set<Network> networksByTopology = networkRepository.findByTopology(topology);
        if (!credentialsByTopology.isEmpty() || !templatesByTopology.isEmpty() || !networksByTopology.isEmpty()) {
            String conflicts = String.format("%s%s%s",
                    getDetailedExceptionMessage("Credential", credentialsByTopology, credentialsByTopology.stream().map(Credential::getName)),
                    getDetailedExceptionMessage("Template", templatesByTopology, templatesByTopology.stream().map(Template::getName)),
                    getDetailedExceptionMessage("Network", networksByTopology, networksByTopology.stream().map(Network::getName)));
            String message = String.format("The following topology ['%s'] is used by the following resource(s): %s", topology.getName(), conflicts);
            throw new BadRequestException(message);
        }
        LOGGER.info("Deleting topology. {} - {}", topology.getId(), topology.getName());
        Date now = new Date();
        String terminatedName = topology.getName() + DELIMITER + now.getTime();
        topology.setName(terminatedName);
        topology.setDeleted(true);
        topologyRepository.save(topology);
    }

    private String getDetailedExceptionMessage(String type, Set<?> repositoryContent, Stream<String> values) {
        if (repositoryContent.isEmpty()) {
            return "";
        }
        String message = repositoryContent.size() > 1 ? "%ss: %s" : "%s: %s";
        return String.join(LINE_SEPARATOR, String.format(message, type, values.collect(Collectors.joining(", "))));
    }
}
