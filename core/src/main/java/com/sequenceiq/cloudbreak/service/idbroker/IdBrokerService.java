package com.sequenceiq.cloudbreak.service.idbroker;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.converter.IdBrokerConverterUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.repository.IdBrokerRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Service
public class IdBrokerService {

    private static final Logger LOGGER = getLogger(IdBrokerService.class);

    @Inject
    private IdBrokerRepository repository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private IdBrokerConverterUtil idBrokerConverterUtil;

    public IdBroker save(IdBroker idBroker) {
        return repository.save(idBroker);
    }

    public IdBroker getByCluster(Cluster cluster) {
        return repository.findByClusterId(cluster.getId());
    }

    public void generateIdBrokerSignKey(Long stackId) {
        LOGGER.debug("Generate IdBroker sign keys for the cluster");
        Cluster cluster = clusterService.findOneByStackIdOrNotFoundError(stackId);
        IdBroker idBroker = idBrokerConverterUtil.generateIdBrokerSignKeys(cluster);
        repository.save(idBroker);
    }
}
