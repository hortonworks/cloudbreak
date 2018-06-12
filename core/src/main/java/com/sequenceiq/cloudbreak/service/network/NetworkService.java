package com.sequenceiq.cloudbreak.service.network;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.util.NameUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

@Service
public class NetworkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private AuthorizationService authorizationService;

    public Network create(IdentityUser user, Network network) {
        LOGGER.info("Creating network: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        network.setOwner(user.getUserId());
        network.setAccount(user.getAccount());
        try {
            return networkRepository.save(network);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.NETWORK, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public Network get(Long id) {
        Network network = getById(id);
        authorizationService.hasReadPermission(network);
        return network;
    }

    public Network getById(Long id) {
        Network network = networkRepository.findOneById(id);
        if (network == null) {
            throw new NotFoundException(String.format("Network '%s' not found", id));
        }
        return network;
    }

    public Network getPrivateNetwork(String name, IdentityUser user) {
        Network network = networkRepository.findByNameForUser(name, user.getUserId());
        if (network == null) {
            throw new NotFoundException(String.format("Network '%s' not found", name));
        }
        return network;
    }

    public Network getPublicNetwork(String name, IdentityUser user) {
        Network network = networkRepository.findByNameInAccount(name, user.getAccount());
        if (network == null) {
            throw new NotFoundException(String.format("Network '%s' not found", name));
        }
        return network;
    }

    public void delete(Long id, IdentityUser user) {
        LOGGER.info("Deleting network with id: {}", id);
        delete(get(id));
    }

    public void delete(String name, IdentityUser user) {
        LOGGER.info("Deleting network with name: {}", name);
        Network network = networkRepository.findByNameInAccount(name, user.getAccount());
        if (network == null) {
            throw new NotFoundException(String.format("Network '%s' not found.", name));
        }
        delete(network);
    }

    public Set<Network> retrievePrivateNetworks(IdentityUser user) {
        return networkRepository.findForUser(user.getUserId());
    }

    public Set<Network> retrieveAccountNetworks(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? networkRepository.findAllInAccount(user.getAccount())
                : networkRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    private void delete(Network network) {
        authorizationService.hasWritePermission(network);
        List<Stack> stacksWithThisNetwork = new ArrayList<>(stackRepository.findByNetwork(network));
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
        if (ResourceStatus.USER_MANAGED.equals(network.getStatus())) {
            networkRepository.delete(network);
        } else {
            network.setName(NameUtil.postfixWithTimestamp(network.getName()));
            network.setStatus(ResourceStatus.DEFAULT_DELETED);
            networkRepository.save(network);
        }
    }
}
