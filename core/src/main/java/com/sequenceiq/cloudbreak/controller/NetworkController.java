package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import com.sequenceiq.cloudbreak.api.endpoint.NetworkEndpoint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.NetworkJson;
import com.sequenceiq.cloudbreak.service.network.DefaultNetworkCreator;
import com.sequenceiq.cloudbreak.service.network.NetworkService;

@Component
public class NetworkController implements NetworkEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private DefaultNetworkCreator networkCreator;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public IdJson postPrivate(NetworkJson networkJson) {
        CbUser user = authenticatedUserService.getCbUser();
        return createNetwork(user, networkJson, false);
    }

    @Override
    public IdJson postPublic(NetworkJson networkJson) {
        CbUser user = authenticatedUserService.getCbUser();
        return createNetwork(user, networkJson, true);
    }

    @Override
    public Set<NetworkJson> getPrivates() {
        CbUser user = authenticatedUserService.getCbUser();
        networkCreator.createDefaultNetworks(user);
        Set<Network> networks = networkService.retrievePrivateNetworks(user);
        return convert(networks);
    }

    @Override
    public Set<NetworkJson> getPublics() {
        CbUser user = authenticatedUserService.getCbUser();
        networkCreator.createDefaultNetworks(user);
        Set<Network> networks = networkService.retrieveAccountNetworks(user);
        return convert(networks);
    }

    @Override
    public NetworkJson get(Long id) {
        Network network = networkService.getById(id);
        return convert(network);
    }

    @Override
    public NetworkJson getPrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        Network network = networkService.getPrivateNetwork(name, user);
        return convert(network);
    }

    @Override
    public NetworkJson getPublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        Network network = networkService.getPublicNetwork(name, user);
        return convert(network);
    }

    @Override
    public void delete(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        networkService.delete(id, user);
    }

    @Override
    public void deletePublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        networkService.delete(name, user);
    }

    @Override
    public void deletePrivate(@PathVariable String name) {
        CbUser user = authenticatedUserService.getCbUser();
        networkService.delete(name, user);
    }

    private IdJson createNetwork(CbUser user, NetworkJson networkRequest, boolean publicInAccount) {
        Network network = convert(networkRequest, publicInAccount);
        network = networkService.create(user, network);
        return new IdJson(network.getId());
    }

    private Network convert(NetworkJson networkRequest, boolean publicInAccount) {
        Network network = conversionService.convert(networkRequest, Network.class);
        network.setPublicInAccount(publicInAccount);
        return network;
    }

    private NetworkJson convert(Network network) {
        return conversionService.convert(network, NetworkJson.class);
    }

    private Set<NetworkJson> convert(Set<Network> networks) {
        Set<NetworkJson> jsons = new HashSet<>();
        for (Network network : networks) {
            jsons.add(convert(network));
        }
        return jsons;
    }
}
