package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Service
public class RedbeamsDbCertificateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDbCertificateProvider.class);

    private final RedbeamsDbServerConfigurer dbServerConfigurer;

    private final DatalakeResourcesService datalakeResourcesService;

    private final StackService stackService;

    public RedbeamsDbCertificateProvider(RedbeamsDbServerConfigurer dbServerConfigurer, DatalakeResourcesService datalakeResourcesService,
            StackService stackService) {
        this.dbServerConfigurer = dbServerConfigurer;
        this.datalakeResourcesService = datalakeResourcesService;
        this.stackService = stackService;
    }

    public Set<String> getRelatedSslCerts(Stack stack, Cluster cluster) {
        Set<String> result = new HashSet<>();
        getDatalakeDatabaseRootCerts(stack, result);
        result.addAll(getDatabaseRootCerts(cluster));
        //TODO persist the gathered certs for the cluster to support cert rotation in the future
        return result;
    }

    private void getDatalakeDatabaseRootCerts(Stack stack, Set<String> result) {
        if (StackType.WORKLOAD.equals(stack.getType())) {
            if (stack.getDatalakeResourceId() != null) {
                LOGGER.debug("Gathering datalake and it's database if exists for the cluster");
                Optional<DatalakeResources> datalakeResource = datalakeResourcesService.findById(stack.getDatalakeResourceId());
                if (datalakeResource.isPresent()) {
                    Long datalakeStackId = datalakeResource.get().getDatalakeStackId();
                    Stack dataLakeStack = stackService.getByIdWithListsInTransaction(datalakeStackId);
                    Cluster dataLakeCluster = dataLakeStack.getCluster();
                    result.addAll(getDatabaseRootCerts(dataLakeCluster));
                } else {
                    LOGGER.info("There is no datalake resource could be found for the cluster.");
                }
            } else {
                LOGGER.info("There is no datalake resource id has set for the cluster.");
            }

        }
    }

    private Set<String> getDatabaseRootCerts(Cluster cluster) {
        Set<String> result = new HashSet<>();
        if (dbServerConfigurer.isRemoteDatabaseNeeded(cluster)) {
            String stackResourceCrn = cluster.getStack().getResourceCrn();
            String clusterName = cluster.getName();
            LOGGER.info("Gathering cluster's(crn:'{}', name: '{}') remote database root certificates", stackResourceCrn, clusterName);
            DatabaseServerV4Response databaseServer = dbServerConfigurer.getDatabaseServer(cluster.getDatabaseServerCrn());
            SslMode sslMode = databaseServer.getSslConfig().getSslMode();
            if (SslMode.ENABLED.equals(sslMode)) {
                Set<String> sslCertificates = databaseServer.getSslConfig().getSslCertificates();
                result.addAll(sslCertificates);
                LOGGER.info("Number of certificates found:'{}' for cluster(crn:'{}', name: '{}')", sslCertificates.size(), stackResourceCrn, clusterName);
            }
        }
        return result;
    }
}
