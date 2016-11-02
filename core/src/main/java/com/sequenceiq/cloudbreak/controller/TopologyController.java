package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.TopologyEndpoint;
import com.sequenceiq.cloudbreak.api.model.TopologyRequest;
import com.sequenceiq.cloudbreak.api.model.TopologyResponse;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.TopologyRepository;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class TopologyController implements TopologyEndpoint {

    @Autowired
    private TopologyService topologyService;

    @Autowired
    private TopologyRepository topologyRepository;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public Set<TopologyResponse> getPublics() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Set<Topology> stacks = topologyRepository.findAllInAccount(user.getAccount());

        return convert(stacks);
    }

    private TopologyResponse convert(Topology topology) {
        return conversionService.convert(topology, TopologyResponse.class);
    }

    private Set<TopologyResponse> convert(Set<Topology> topologies) {
        Set<TopologyResponse> jsons = new HashSet<>();
        for (Topology topology : topologies) {
            jsons.add(convert(topology));
        }
        return jsons;
    }

    @Override
    public TopologyResponse postPublic(TopologyRequest topologyRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Topology topology = conversionService.convert(topologyRequest, Topology.class);
        topology = topologyService.create(user, topology);
        return conversionService.convert(topology, TopologyResponse.class);
    }

    @Override
    public void delete(Long id, Boolean forced) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        topologyService.delete(id, user);
    }

    @Override
    public TopologyResponse get(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Topology topology = topologyService.get(id);
        return convert(topology);
    }
}
