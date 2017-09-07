package com.sequenceiq.cloudbreak.service.network;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
@Transactional
public class NetworkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);

    @Inject
    private NetworkRepository networkRepository;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Transactional(TxType.NEVER)
    public Network create(IdentityUser user, Network network) {
        LOGGER.info("Creating network: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        network.setOwner(user.getUserId());
        network.setAccount(user.getAccount());
        try {
            return networkRepository.save(network);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.NETWORK, network.getName(), ex);
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Network get(Long id) {
        return getById(id);
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

    @Transactional(TxType.NEVER)
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
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            return networkRepository.findAllInAccount(user.getAccount());
        } else {
            return networkRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    private void delete(Network network) {
        authorizationService.hasWritePermission(network);
        if (!stackRepository.countByNetwork(network).equals(0L)) {
            throw new BadRequestException(String.format(
                    "There are clusters associated with network '%s'(ID:'%s'). Please remove these before deleting the network.",
                    network.getName(), network.getId()));
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
