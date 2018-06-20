package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.TopologyEndpoint;
import com.sequenceiq.cloudbreak.api.model.TopologyRequest;
import com.sequenceiq.cloudbreak.api.model.TopologyResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
@Transactional(TxType.NEVER)
public class TopologyController extends NotificationController implements TopologyEndpoint {

    @Autowired
    private TopologyService topologyService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public Set<TopologyResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<Topology> stacks = topologyService.findAllInAccount(user.getAccount());

        return convert(stacks);
    }

    private TopologyResponse convert(Topology topology) {
        return conversionService.convert(topology, TopologyResponse.class);
    }

    private Set<TopologyResponse> convert(Iterable<Topology> topologies) {
        Set<TopologyResponse> jsons = new HashSet<>();
        for (Topology topology : topologies) {
            jsons.add(convert(topology));
        }
        return jsons;
    }

    @Override
    public TopologyResponse postPublic(TopologyRequest topologyRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Topology topology = conversionService.convert(topologyRequest, Topology.class);
        topology = topologyService.create(user, topology);
        notify(user, ResourceEvent.TOPOLOGY_CREATED);
        return conversionService.convert(topology, TopologyResponse.class);
    }

    @Override
    public void delete(Long id, Boolean forced) {
        executeAndNotify(user -> topologyService.delete(id, user), ResourceEvent.TOPOLOGY_DELETED);
    }

    @Override
    public TopologyResponse get(Long id) {
        Topology topology = topologyService.get(id);
        return convert(topology);
    }
}
