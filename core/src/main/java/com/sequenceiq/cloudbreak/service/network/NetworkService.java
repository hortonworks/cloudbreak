package com.sequenceiq.cloudbreak.service.network;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Network;

public interface NetworkService {

    Network create(CbUser user, Network network);

    Network get(Long id);

    Network getById(Long id);

    Network getPrivateNetwork(String name, CbUser user);

    Network getPublicNetwork(String name, CbUser user);

    void delete(Long id, CbUser user);

    void delete(String name, CbUser user);

    Set<Network> retrievePrivateNetworks(CbUser user);

    Set<Network> retrieveAccountNetworks(CbUser user);

}
