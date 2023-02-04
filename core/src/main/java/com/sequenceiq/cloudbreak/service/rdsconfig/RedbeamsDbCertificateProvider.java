package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.DatabaseSslDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;

@Service
public class RedbeamsDbCertificateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDbCertificateProvider.class);

    private final RedbeamsDbServerConfigurer dbServerConfigurer;

    private final DatalakeService datalakeService;

    public RedbeamsDbCertificateProvider(RedbeamsDbServerConfigurer dbServerConfigurer, DatalakeService datalakeService) {
        this.dbServerConfigurer = dbServerConfigurer;
        this.datalakeService = datalakeService;
    }

    public DatabaseSslDetails getRelatedSslCerts(StackDto stackDto) {
        Set<String> relatedSslCerts = new HashSet<>();
        StackView stack = stackDto.getStack();
        getDatalakeDatabaseRootCerts(stack, relatedSslCerts);
        ClusterView cluster = stackDto.getCluster();
        Set<String> sslCertsForStack = getDatabaseRootCerts(cluster.getName(), cluster.getDatabaseServerCrn(), stack.getResourceCrn());
        relatedSslCerts.addAll(sslCertsForStack);
        // Note: When stackDto is a DH, relatedSslCerts is the union of DL + DH certs,
        // and sslEnabledForStack is purely determined by the presence of DH certs.
        return new DatabaseSslDetails(relatedSslCerts, !sslCertsForStack.isEmpty());
    }

    private void getDatalakeDatabaseRootCerts(StackView stack, Set<String> result) {
        if (StackType.WORKLOAD.equals(stack.getType())) {
            Optional<Stack> datalakeStack = datalakeService.getDatalakeStackByDatahubStack(stack);
            LOGGER.debug("Gathering datalake and its database if exists for the datahub cluster");
            if (datalakeStack.isPresent()) {
                Cluster dataLakeCluster = datalakeStack.get().getCluster();
                result.addAll(getDatabaseRootCerts(dataLakeCluster.getName(), dataLakeCluster.getDatabaseServerCrn(), datalakeStack.get().getResourceCrn()));
            } else {
                LOGGER.warn("No datalake resource could be found for the datahub cluster");
            }
        } else {
            LOGGER.debug("Stack is not a datahub cluster");
        }
    }

    private Set<String> getDatabaseRootCerts(String clusterName, String dbServerCrn, String stackResourceCrn) {
        Set<String> result = new HashSet<>();
        if (dbServerConfigurer.isRemoteDatabaseRequested(dbServerCrn)) {
            LOGGER.info("Gathering cluster's(crn:'{}', name: '{}') remote database('{}') root certificates", stackResourceCrn, clusterName, dbServerCrn);
            DatabaseServerV4Response databaseServer = dbServerConfigurer.getDatabaseServer(dbServerCrn);
            SslConfigV4Response sslConfig = databaseServer.getSslConfig();
            if (sslConfig != null) {
                if (SslMode.isEnabled(sslConfig.getSslMode())) {
                    Set<String> sslCertificates = sslConfig.getSslCertificates();
                    if (CollectionUtils.isEmpty(sslCertificates)) {
                        throw new IllegalStateException(
                                String.format("External DB SSL enforcement is enabled for cluster(crn:'%s', name: '%s') and remote database('%s')," +
                                                "but no certificates have been returned!", stackResourceCrn, clusterName, dbServerCrn));
                    }
                    result.addAll(sslCertificates);
                    LOGGER.info("Number of certificates found:'{}' for cluster(crn:'{}', name: '{}') and remote database('{}')", sslCertificates.size(),
                            stackResourceCrn, clusterName, dbServerCrn);
                } else {
                    LOGGER.info("External DB SSL enforcement is disabled for the cluster's(crn:'{}', name: '{}') remote database('{}').", stackResourceCrn,
                            clusterName, dbServerCrn);
                }
            } else {
                LOGGER.info("No SSL config could be found for the cluster's(crn:'{}', name: '{}') remote database('{}').", stackResourceCrn, clusterName,
                        dbServerCrn);
            }
        } else {
            LOGGER.info("No remote database is configured for cluster(crn:'{}', name: '{}')", stackResourceCrn, clusterName);
        }
        return result;
    }
}
