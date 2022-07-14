package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;

@Service
public class RedbeamsDbCertificateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDbCertificateProvider.class);

    private final RedbeamsDbServerConfigurer dbServerConfigurer;

    private final StackService stackService;

    private final DatalakeService datalakeService;

    private final String certsPath;

    public RedbeamsDbCertificateProvider(RedbeamsDbServerConfigurer dbServerConfigurer,
            StackService stackService, DatalakeService datalakeService, @Value("${cb.externaldatabase.ssl.rootcerts.path:}") String certsPath) {
        this.dbServerConfigurer = dbServerConfigurer;
        this.stackService = stackService;
        this.datalakeService = datalakeService;
        this.certsPath = certsPath;
    }

    public String getSslCertsFilePath() {
        return certsPath;
    }

    public Set<String> getRelatedSslCerts(StackDto stackDto) {
        Set<String> result = new HashSet<>();
        getDatalakeDatabaseRootCerts(stackDto.getStack(), result);
        ClusterView cluster = stackDto.getCluster();
        result.addAll(getDatabaseRootCerts(cluster.getName(), cluster.getDatabaseServerCrn(), stackDto.getStack().getResourceCrn()));
        //TODO persist the gathered certs for the cluster to support cert rotation in the future
        return result;
    }

    private void getDatalakeDatabaseRootCerts(StackView stack, Set<String> result) {
        if (StackType.WORKLOAD.equals(stack.getType())) {
            Optional<Stack> datalakeStack = datalakeService.getDatalakeStackByDatahubStack(stack);
            LOGGER.debug("Gathering datalake and its database if exists for the cluster");
            if (datalakeStack.isPresent()) {
                Cluster dataLakeCluster = datalakeStack.get().getCluster();
                result.addAll(getDatabaseRootCerts(dataLakeCluster.getName(), dataLakeCluster.getDatabaseServerCrn(), datalakeStack.get().getResourceCrn()));
            } else {
                LOGGER.info("There is no datalake resource could be found for the cluster.");
            }
        }
    }

    private Set<String> getDatabaseRootCerts(String clusterName, String dbServerCrn, String stackResourceCrn) {
        Set<String> result = new HashSet<>();
        if (dbServerConfigurer.isRemoteDatabaseNeeded(dbServerCrn)) {
            LOGGER.info("Gathering cluster's(crn:'{}', name: '{}') remote database root certificates", stackResourceCrn, clusterName);
            DatabaseServerV4Response databaseServer = dbServerConfigurer.getDatabaseServer(dbServerCrn);
            SslConfigV4Response sslConfig = databaseServer.getSslConfig();
            if (sslConfig != null) {
                if (SslMode.isEnabled(sslConfig.getSslMode())) {
                    Set<String> sslCertificates = sslConfig.getSslCertificates();
                    result.addAll(sslCertificates);
                    LOGGER.info("Number of certificates found:'{}' for cluster(crn:'{}', name: '{}')", sslCertificates.size(), stackResourceCrn, clusterName);
                }
            } else {
                LOGGER.info("There no SSL config could be found for the remote database('{}').", dbServerCrn);
            }
        }
        return result;
    }
}
