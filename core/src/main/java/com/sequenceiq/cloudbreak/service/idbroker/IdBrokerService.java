package com.sequenceiq.cloudbreak.service.idbroker;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
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

    public IdBroker getByCluster(Long clusterId) {
        return repository.findByClusterId(clusterId);
    }

    public void generateIdBrokerSignKey(Long stackId) {
        Cluster cluster = clusterService.findOneByStackIdOrNotFoundError(stackId);
        IdBroker idBroker = repository.findByClusterId(cluster.getId());
        if (idBroker == null) {
            LOGGER.debug("Generate IdBroker sign keys for the cluster");
            idBroker = idBrokerConverterUtil.generateIdBrokerSignKeys(cluster.getId(), cluster.getWorkspace());
            repository.save(idBroker);
        } else {
            LOGGER.debug("IdBroker sign keysh have already been created");
        }
    }

    public IdBroker putLegacyFieldsIntoVaultIfNecessary(Long idBrokerId) {
        IdBroker idBroker = repository.findById(idBrokerId)
                .orElseThrow(NotFoundException.notFound(String.format("IdBroker should exist, id: %d", idBrokerId)));
        if (idBroker.getSignCertSecret() == null || idBroker.getSignCertSecret().getRaw() == null) {
            idBroker.setSignCert(idBroker.getSignCertDeprecated());
        }
        if (idBroker.getSignPubSecret() == null || idBroker.getSignPubSecret().getRaw() == null) {
            idBroker.setSignPub(idBroker.getSignPubDeprecated());
        }
        return save(idBroker);
    }

    public void setLegacyFieldsForServiceRollback(Long idBrokerId) {
        IdBroker idBroker = repository.findById(idBrokerId)
                .orElseThrow(NotFoundException.notFound(String.format("IdBroker should exist, id: %d", idBrokerId)));
        idBroker.setSignCertDeprecated(idBroker.getSignCert());
        idBroker.setSignPubDeprecated(idBroker.getSignPub());
        save(idBroker);
    }
}
