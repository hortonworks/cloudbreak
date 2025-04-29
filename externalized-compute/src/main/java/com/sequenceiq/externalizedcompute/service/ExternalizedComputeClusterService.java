package com.sequenceiq.externalizedcompute.service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.DeleteClusterResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ListClusterItem;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidateCredentialRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidateCredentialResponse;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidationResult;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterCredentialValidationResponse;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatus;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterFlowManager;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterRepository;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.liftie.client.LiftieGrpcClient;

@Service
public class ExternalizedComputeClusterService implements ResourceIdProvider, PayloadContextProvider {

    public static final String DELETING_LIFTIE_STATUS = "DELETING";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterService.class);

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private ExternalizedComputeClusterRepository externalizedComputeClusterRepository;

    @Inject
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @Inject
    private ExternalizedComputeClusterFlowManager externalizedComputeClusterFlowManager;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private LiftieGrpcClient liftieGrpcClient;

    @Inject
    private TransactionService transactionService;

    @Inject
    private Clock clock;

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        return externalizedComputeClusterRepository.findByResourceCrnAndAccountIdAndDeletedIsNull(resourceCrn, ThreadBasedUserCrnProvider.getAccountId())
                .map(ExternalizedComputeCluster::getId)
                .orElseThrow(() -> new NotFoundException("Resource id not found for resource crn"));
    }

    @Override
    public String getResourceCrnByResourceId(Long resourceId) {
        return externalizedComputeClusterRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found for resource id: " + resourceId))
                .getResourceCrn();
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        return externalizedComputeClusterRepository.findByNameAndAccountIdAndDeletedIsNull(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .map(ExternalizedComputeCluster::getId)
                .orElseThrow(() -> new NotFoundException("Resource id not found for resource name"));
    }

    @Override
    public List<Long> getResourceIdsByResourceCrn(String resourceName) {
        return externalizedComputeClusterRepository.findByResourceCrnAndAccountIdAndDeletedIsNull(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .stream()
                .map(ExternalizedComputeCluster::getId)
                .collect(Collectors.toList());
    }

    public Set<Long> findByResourceIdsAndStatuses(Set<Long> resourceIds, Set<ExternalizedComputeClusterStatusEnum> statuses) {
        LOGGER.debug("Find by resource ids ({}) and statuses: {}", resourceIds, statuses);
        return externalizedComputeClusterStatusService.findLatestClusterStatusesFilteredByStatusesAndClusterIds(statuses, resourceIds)
                .stream().map(statusEntity -> statusEntity.getExternalizedComputeCluster().getId()).collect(Collectors.toSet());
    }

    public FlowIdentifier reInitializeComputeCluster(ExternalizedComputeClusterRequest externalizedComputeClusterRequest, boolean force) {
        ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(externalizedComputeClusterRequest.getEnvironmentCrn(),
                externalizedComputeClusterRequest.getName());
        ExternalizedComputeClusterStatus actualStatus = externalizedComputeClusterStatusService.getActualStatus(externalizedComputeCluster);
        if (actualStatus.getStatus().isInProgress()) {
            throw new BadRequestException("Compute cluster is under operation.");
        } else if (!actualStatus.getStatus().isFailed() && !force) {
            throw new BadRequestException("Compute cluster is not in failed state.");
        } else {
            return externalizedComputeClusterFlowManager.triggerExternalizedComputeClusterReInitialization(externalizedComputeCluster);
        }
    }

    public FlowIdentifier prepareComputeClusterCreation(ExternalizedComputeClusterRequest externalizedComputeClusterRequest,
            boolean defaultCluster, Crn userCrn) {
        ExternalizedComputeCluster externalizedComputeCluster = new ExternalizedComputeCluster();
        externalizedComputeCluster.setName(externalizedComputeClusterRequest.getName());
        DetailedEnvironmentResponse environment = environmentEndpoint.getByCrn(externalizedComputeClusterRequest.getEnvironmentCrn());
        externalizedComputeCluster.setEnvironmentCrn(environment.getCrn());
        externalizedComputeCluster.setCreated(new Date().getTime());
        externalizedComputeCluster.setAccountId(userCrn.getAccountId());
        String crn = regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.EXTERNALIZED_COMPUTE, userCrn.getAccountId());
        externalizedComputeCluster.setResourceCrn(crn);
        externalizedComputeCluster.setDefaultCluster(defaultCluster);
        // TODO: add check if exists
        try {
            ExternalizedComputeCluster initiatedExternalizedComputeCluster = transactionService.required(() -> {
                ExternalizedComputeCluster savedExternalizedComputeCluster = externalizedComputeClusterRepository.save(externalizedComputeCluster);
                MDCBuilder.buildMdcContext(savedExternalizedComputeCluster);
                externalizedComputeClusterStatusService.setStatus(savedExternalizedComputeCluster, ExternalizedComputeClusterStatusEnum.CREATE_IN_PROGRESS,
                        "Cluster provision initiated");
                return savedExternalizedComputeCluster;
            });
            LOGGER.info("Saved ExternalizedComputeCluster entity: {}", initiatedExternalizedComputeCluster);
            return externalizedComputeClusterFlowManager.triggerExternalizedComputeClusterCreation(initiatedExternalizedComputeCluster);
        } catch (TransactionExecutionException e) {
            LOGGER.error("Externalized compute cluster save failed", e);
            throw new RuntimeException("Externalized compute cluster initiation failed");
        }
    }

    public void initiateAuxClusterDelete(Long externalizedComputeClusterId, String actorCrn, boolean force) {
        ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(externalizedComputeClusterId);
        LOGGER.info("Initiate auxiliary cluster delete for: {}", externalizedComputeCluster.getName());

        String environmentCrn = externalizedComputeCluster.getEnvironmentCrn();
        List<ListClusterItem> auxClusters = liftieGrpcClient.listAuxClusters(environmentCrn, actorCrn);
        for (ListClusterItem listClusterItem : auxClusters) {
            try {
                liftieGrpcClient.deleteCluster(listClusterItem.getClusterCrn(),
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        environmentCrn, force);
            } catch (Exception e) {
                if (!e.getMessage().contains("already deleted") && !e.getMessage().contains("not found in database")
                        && !e.getMessage().contains("existing operation 'Delete'")) {
                    LOGGER.error("Auxiliary compute cluster deletion failed", e);
                    throw new RuntimeException("Auxiliary compute cluster deletion failed. Cause: " + e.getMessage(), e);
                }
            }
        }
    }

    public void initiateDelete(Long id, boolean force) {
        ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(id);
        LOGGER.info("Initiate delete for: {}", externalizedComputeCluster.getName());
        if (externalizedComputeCluster.getLiftieName() != null) {
            String liftieClusterCrn = getLiftieClusterCrn(externalizedComputeCluster);
            String internalCrn = regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString();
            try {
                DeleteClusterResponse deleteClusterResponse =
                        liftieGrpcClient.deleteCluster(liftieClusterCrn,
                                internalCrn,
                                externalizedComputeCluster.getEnvironmentCrn(), force);
                LOGGER.info("Liftie delete response: {}", deleteClusterResponse);
            } catch (Exception e) {
                if (e.getMessage().contains("existing operation 'Delete'")) {
                    LiftieSharedProto.DescribeClusterResponse describeClusterResponse = liftieGrpcClient.describeCluster(liftieClusterCrn, internalCrn);
                    if (!DELETING_LIFTIE_STATUS.equals(describeClusterResponse.getStatus())) {
                        LOGGER.error("Liftie cluster delete failed, there is a delete operation, but not in "
                                + DELETING_LIFTIE_STATUS + "status: {}", describeClusterResponse);
                        throw new RuntimeException(e.getMessage());
                    }
                } else if (!e.getMessage().contains("already deleted") && !e.getMessage().contains("not found in database")) {
                    LOGGER.error("Externalized compute cluster deletion failed", e);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    public ExternalizedComputeCluster getExternalizedComputeCluster(String environmentCrn, String name) {
        return externalizedComputeClusterRepository.findByEnvironmentCrnAndNameAndDeletedIsNull(environmentCrn, name)
                .orElseThrow(() -> new NotFoundException(String.format("Can't find externalized compute cluster by environmentCrn: %s and name: %s",
                        environmentCrn, name)));
    }

    public ExternalizedComputeCluster getExternalizedComputeCluster(Long id) {
        return externalizedComputeClusterRepository.findByIdAndDeletedIsNull(id)
                .orElseThrow(() -> new NotFoundException("Can't find externalized compute cluster by id: " + id));
    }

    public void deleteLiftieClusterNameForCluster(Long id) {
        externalizedComputeClusterRepository.findById(id).ifPresent(externalizedComputeCluster -> {
            externalizedComputeCluster.setLiftieName(null);
            externalizedComputeClusterRepository.save(externalizedComputeCluster);
        });
    }

    public void deleteExternalizedComputeCluster(Long id) {
        try {
            transactionService.required(() -> {
                externalizedComputeClusterStatusService.setStatus(id, ExternalizedComputeClusterStatusEnum.DELETED, "Successfully deleted");
                externalizedComputeClusterRepository.findById(id).ifPresent(externalizedComputeCluster -> {
                    externalizedComputeCluster.setDeleted(clock.getCurrentTimeMillis());
                    externalizedComputeClusterRepository.save(externalizedComputeCluster);
                });
            });
        } catch (TransactionExecutionException e) {
            throw new RuntimeException("Could not delete cluster", e);
        }
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        try {
            ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(resourceId);
            DetailedEnvironmentResponse envResp = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> environmentEndpoint.getByCrn(externalizedComputeCluster.getEnvironmentCrn()));
            return PayloadContext.create(externalizedComputeCluster.getResourceCrn(), envResp.getCrn(), envResp.getCloudPlatform());
        } catch (NotFoundException exception) {
            LOGGER.info("Exception happened when trying to get payload context {}", exception.getMessage(), exception);
        } catch (Exception e) {
            LOGGER.warn("Error happened during fetching payload context for externalized compute", e);
        }
        return null;
    }

    public List<ExternalizedComputeCluster> getAllByEnvironmentCrn(String environmentCrn, String accountId) {
        return externalizedComputeClusterRepository.findAllByEnvironmentCrnAndAccountIdAndDeletedIsNull(environmentCrn, accountId);
    }

    public String getLiftieClusterCrn(ExternalizedComputeCluster externalizedComputeCluster) {
        return regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.COMPUTE_CLUSTER,
                externalizedComputeCluster.getLiftieName(), externalizedComputeCluster.getAccountId());
    }

    public ExternalizedComputeClusterCredentialValidationResponse validateCredential(String credentialName, String region, String actorCrn) {
        ValidateCredentialResponse validateCredentialResponse = liftieGrpcClient.validateCredential(
                ValidateCredentialRequest.newBuilder().setCredential(credentialName).setRegion(region).build(), actorCrn);
        List<String> validationResults = validateCredentialResponse.getValidationsList().stream().map(ValidationResult::getDetailedMessage).toList();
        boolean successful = "PASSED".equals(validateCredentialResponse.getResult());
        return new ExternalizedComputeClusterCredentialValidationResponse(successful, validateCredentialResponse.getMessage(), validationResults);
    }
}
