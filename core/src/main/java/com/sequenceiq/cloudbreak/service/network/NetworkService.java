package com.sequenceiq.cloudbreak.service.network;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractArchivistService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class NetworkService extends AbstractArchivistService<Network> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private StackService stackService;

    public Network create(Network network, Workspace workspace) {
        network.setWorkspace(workspace);
        try {
            return networkRepository.save(network);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.NETWORK, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public Network get(Long id) {
        return repository().findById(id).orElseThrow(notFound("Network", id));
    }

    public void delete(Long id) {
        Optional.ofNullable(networkRepository.findById(id)).orElseThrow(notFound("Network", id))
                .map(this::delete);
    }

    @Override
    protected void prepareDeletion(Network network) {
        List<Stack> stacksWithThisNetwork = new ArrayList<>(stackService.getByNetwork(network));
        if (!stacksWithThisNetwork.isEmpty()) {
            if (stacksWithThisNetwork.size() > 1) {
                String clusters = stacksWithThisNetwork
                        .stream()
                        .map(stack -> stack.getCluster().getName())
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with network '%s'(ID:'%s'). Please remove these before deleting the network. "
                                + "The following clusters are using this network: [%s]", network.getName(), network.getId(), clusters));
            } else {
                throw new BadRequestException(String.format("There is a cluster ['%s'] which uses network '%s'(ID:'%s'). Please remove this "
                        + "cluster before deleting the network", stacksWithThisNetwork.get(0).getCluster().getName(), network.getName(), network.getId()));
            }
        }
    }

    @Override
    protected void prepareCreation(Network resource) {

    }

    @Override
    public WorkspaceResourceRepository<Network, Long> repository() {
        return networkRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.STACK;
    }

}
