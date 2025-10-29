package com.sequenceiq.cloudbreak.service.cluster;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.DatabaseSslDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class DatabaseSslService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSslService.class);

    private static final String ENABLED = "enabled";

    private static final String DISABLED = "disabled";

    @Value("${cb.externaldatabase.ssl.rootcerts.path:}")
    private String certsPath;

    @Inject
    private RedbeamsDbCertificateProvider dbCertificateProvider;

    @Inject
    private EmbeddedDatabaseService embeddedDatabaseService;

    @Inject
    private FreeipaClientService freeipaClientService;

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private ClusterService clusterService;

    public String getSslCertsFilePath() {
        return certsPath;
    }

    public boolean isDbSslEnabledByClusterView(StackView stackView, ClusterView clusterView) {
        return Optional.ofNullable(clusterView.getDbSslEnabled())
                .or(() -> {
                    LOGGER.warn("Cluster.dbSslEnabled is null for stack '{}' with CRN '{}'. Assuming false.", stackView.getName(), stackView.getResourceCrn());
                    return Optional.of(Boolean.FALSE);
                })
                .get();
    }

    public DatabaseSslDetails getDbSslDetailsForCreationAndUpdateInCluster(StackDto stackDto) {
        return getDbSslDetailsAndUpdateInClusterInternal(stackDto, true);
    }

    public DatabaseSslDetails getDbSslDetailsForRotationAndUpdateInCluster(StackDto stackDto) {
        return getDbSslDetailsAndUpdateInClusterInternal(stackDto, false);
    }

    public DatabaseSslDetails getDbSslDetailsForRotationAndUpdateInCluster(Long stackId) {
        return getDbSslDetailsAndUpdateInClusterInternal(stackDtoService.getById(stackId), false);
    }

    public DatabaseSslDetails setEmbeddedDbSslDetailsAndUpdateInClusterInternal(StackDto stackDto) {
        return setEmbeddedDbSslDetailsAndUpdateInCluster(stackDto);
    }

    private DatabaseSslDetails getDbSslDetailsAndUpdateInClusterInternal(StackDto stackDto, boolean creation) {
        LOGGER.info("Invocation mode: {}", creation ? "Creation" : "Rotation");
        DatabaseSslDetails sslDetails = dbCertificateProvider.getRelatedSslCerts(stackDto);
        LOGGER.info("SslDetails from RedbeamsDbCertificateProvider: {}", sslDetails);
        decorateSslDetailsWithEmbeddedDatabase(stackDto, sslDetails, creation);
        LOGGER.info("SslDetails after decorating with embedded DB: {}", sslDetails);
        clusterService.updateDbSslCert(stackDto.getCluster().getId(), sslDetails);
        return sslDetails;
    }

    private DatabaseSslDetails setEmbeddedDbSslDetailsAndUpdateInCluster(StackDto stackDto) {
        DatabaseSslDetails sslDetails = dbCertificateProvider.getRelatedSslCerts(stackDto);
        decorateSslDetailsWithEmbeddedDatabase(stackDto, sslDetails, false);
        clusterService.updateDbSslCert(stackDto.getCluster().getId(), sslDetails);
        return sslDetails;
    }

    private void decorateSslDetailsWithEmbeddedDatabase(StackDto stackDto, DatabaseSslDetails sslDetails, boolean creation) {
        StackView stackView = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        boolean sslEnforcementForStackEmbeddedDatabaseEnabled;
        if (isEmbeddedDatabase(cluster)) {
            // Update sslEnabledForStack; its value in the RedbeamsDbCertificateProvider response is always expected to be false when the stack uses an
            // embedded DB.
            if (sslDetails.isSslEnabledForStack()) {
                throw new IllegalStateException("Mismatching sslDetails.sslEnabledForStack in RedbeamsDbCertificateProvider response. " +
                        "Expecting false because the stack uses an embedded DB, but it was true.");
            }
            sslEnforcementForStackEmbeddedDatabaseEnabled = isSslEnforcementForEmbeddedDatabaseEnabled(stackView, cluster, stackDto.getDatabase(), creation);
            sslDetails.setSslEnabledForStack(sslEnforcementForStackEmbeddedDatabaseEnabled);
            LOGGER.info("SSL enforcement is {} for the stack embedded DB", sslEnforcementForStackEmbeddedDatabaseEnabled ? ENABLED : DISABLED);
        } else {
            sslEnforcementForStackEmbeddedDatabaseEnabled = false;
            LOGGER.info("SSL enforcement is {} for the stack external DB", sslDetails.isSslEnabledForStack() ? ENABLED : DISABLED);
        }

        if (sslEnforcementForStackEmbeddedDatabaseEnabled || isSslEnforcementForDatalakeEmbeddedDatabaseEnabled(stackView, creation)) {
            LOGGER.info("Including the SSL root cert of the embedded DB");
            Set<String> sslCertsNew = new HashSet<>(sslDetails.getSslCerts());
            sslCertsNew.add(getEmbeddedDatabaseRootCertificate(stackView));
            sslDetails.setSslCerts(sslCertsNew);
        } else {
            LOGGER.info("No need to include the SSL root cert of the embedded DB");
        }
    }

    private boolean isSslEnforcementForDatalakeEmbeddedDatabaseEnabled(StackView stackView, boolean creation) {
        if (StackType.WORKLOAD.equals(stackView.getType())) {
            Optional<SdxBasicView> sdxBasicViewOptional = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stackView.getEnvironmentCrn());
            if (sdxBasicViewOptional.isPresent()) {
                return switch (sdxBasicViewOptional.get().platform()) {
                    case PAAS -> isSslEnforcementForVMDatalakeEmbeddedDbEnabled(stackView, creation);
                    case PDL -> false;
                };
            }
        } else {
            LOGGER.debug("Stack is not a datahub cluster");
        }
        return false;
    }

    private boolean isSslEnforcementForVMDatalakeEmbeddedDbEnabled(StackView stackView, boolean creation) {
        boolean sslEnforcementForEmbeddedDbEnabled = false;
        Optional<Stack> datalakeStackOpt = Optional.ofNullable(stackService.getByCrnOrElseNull(stackView.getDatalakeCrn()));
        LOGGER.debug("Gathering datalake and its DB if exists for the datahub cluster");
        if (datalakeStackOpt.isPresent()) {
            Stack datalakeStack = datalakeStackOpt.get();
            Cluster datalakeCluster = datalakeStack.getCluster();
            if (isEmbeddedDatabase(datalakeCluster)) {
                sslEnforcementForEmbeddedDbEnabled =
                        isSslEnforcementForEmbeddedDatabaseEnabled(datalakeStack, datalakeCluster, datalakeStack.getDatabase(), creation);
                LOGGER.info("SSL enforcement is {} for the parent datalake stack embedded DB", sslEnforcementForEmbeddedDbEnabled ? ENABLED : DISABLED);
            } else {
                LOGGER.info("The parent datalake stack uses an external DB");
            }
        } else {
            LOGGER.warn("No datalake resource could be found for the datahub cluster");
        }
        return sslEnforcementForEmbeddedDbEnabled;
    }

    private boolean isEmbeddedDatabase(ClusterView clusterView) {
        return !RedbeamsDbServerConfigurer.isRemoteDatabaseRequested(clusterView.getDatabaseServerCrn());
    }

    private boolean isSslEnforcementForEmbeddedDatabaseEnabled(StackView stackView, ClusterView clusterView, Database database, boolean creation) {
        boolean response;
        String stackName = stackView.getName();
        String stackCrn = stackView.getResourceCrn();
        if (creation) {
            response = embeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled(stackView, clusterView, database);
            LOGGER.info("Retrieving dbSslEnabled from EmbeddedDatabaseService.isSslEnforcementForEmbeddedDatabaseEnabled() for stack '{}' with CRN '{}': {}",
                    stackName, stackCrn, response);
        } else {
            // The stack dbSslEnabled flag is not expected to change after cluster provisioning, so its recent value from Cluster shall be honored in Rotation
            // mode. The only potential exception would be the migration of a non-SSL DB to an SSL one, but this is not currently supported or considered.
            //
            // Furthermore, Cluster.dbSslEnabled is only reliable when the stack uses an embedded DB. When using an external DB, the sslEnabledForStack in the
            // RedbeamsDbCertificateProvider response is the one that matters. This is because legacy clusters with external DB & SSL enforcement
            // enabled may not have this flag initialized at all (i.e. left as null). On the other hand, if null and using an embedded DB, we can be sure it
            // was supposed to be false.
            response = isDbSslEnabledByClusterView(stackView, clusterView);
            LOGGER.info("Retrieving dbSslEnabled from Cluster for stack '{}' with CRN '{}': {}", stackName, stackCrn, response);
        }
        return response;
    }

    private String getEmbeddedDatabaseRootCertificate(StackView stackView) {
        String rootCertificate = freeipaClientService.getRootCertificateByEnvironmentCrn(stackView.getEnvironmentCrn());
        if (StringUtils.isBlank(rootCertificate)) {
            throw new IllegalStateException("Got a blank FreeIPA root certificate.");
        }
        return rootCertificate;
    }

}
