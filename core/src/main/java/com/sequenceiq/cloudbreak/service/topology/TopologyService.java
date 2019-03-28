package com.sequenceiq.cloudbreak.service.topology;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.TopologyRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractArchivistService;

@Service
public class TopologyService extends AbstractArchivistService<Topology> {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyService.class);

    private static final String DELIMITER = "_";

    @Inject
    private TopologyRepository topologyRepository;

    public Topology get(Long id) {
        return topologyRepository.findById(id).orElseThrow(notFound("Topology", id));
    }

    public Topology create(User user, Topology topology, Workspace workspace) {
        LOGGER.debug("Creating topology: [User: '{}']", user.getUserId());
        Topology savedTopology;
        topology.setWorkspace(workspace);
        try {
            savedTopology = topologyRepository.save(topology);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.TOPOLOGY, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return savedTopology;
    }

    @Override
    public WorkspaceResourceRepository<Topology, Long> repository() {
        return topologyRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.STACK;
    }

    @Override
    protected void prepareDeletion(Topology resource) {

    }

    @Override
    protected void prepareCreation(Topology resource) {

    }
}
