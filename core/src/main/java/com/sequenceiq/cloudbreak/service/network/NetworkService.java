package com.sequenceiq.cloudbreak.service.network;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class NetworkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private StackService stackService;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private OrganizationService organizationService;

    public Network create(IdentityUser user, Network network, Organization organization) {
        LOGGER.info("Creating network: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        network.setOwner(user.getUserId());
        network.setAccount(user.getAccount());
        if (organization != null) {
            network.setOrganization(organization);
        } else {
            network.setOrganization(organizationService.getDefaultOrganizationForCurrentUser());
        }
        try {
            return networkRepository.save(network);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.NETWORK, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public Network get(Long id) {
        return networkRepository.findOneById(id);
    }

    public Network getPrivateNetwork(String name, IdentityUser user) {
        return networkRepository.findByNameForUser(name, user.getUserId());
    }

    public Network getPublicNetwork(String name, IdentityUser user) {
        return networkRepository.findByNameInAccount(name, user.getAccount());
    }

    public void delete(Long id, IdentityUser user) {
        deleteImpl(get(id));
    }

    public void delete(String name, IdentityUser user) {
        deleteImpl(networkRepository.findByNameInAccount(name, user.getAccount()));
    }

    public void delete(Network network) {
        deleteImpl(network);
    }

    public Set<Network> retrievePrivateNetworks(IdentityUser user) {
        return networkRepository.findForUser(user.getUserId());
    }

    public Set<Network> retrieveAccountNetworks(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? networkRepository.findAllInAccount(user.getAccount())
                : networkRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    private void deleteImpl(Network network) {
        LOGGER.info("Deleting network with name: {}", network.getName());
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
        if (ResourceStatus.USER_MANAGED.equals(network.getStatus())) {
            networkRepository.delete(network);
        } else {
            network.setName(NameUtil.postfixWithTimestamp(network.getName()));
            network.setStatus(ResourceStatus.DEFAULT_DELETED);
            networkRepository.save(network);
        }
    }

    public Set<Network> findByTopology(Topology topology) {
        return networkRepository.findByTopology(topology);
    }
}
