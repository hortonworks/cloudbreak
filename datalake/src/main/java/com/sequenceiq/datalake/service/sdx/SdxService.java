package com.sequenceiq.datalake.service.sdx;


import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxClusterRequest;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@Service
public class SdxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    public SdxCluster getById(Long id) {
        Optional<SdxCluster> sdxClusters = sdxClusterRepository.findById(id);
        if (sdxClusters.isPresent()) {
            return sdxClusters.get();
        } else {
            throw new NotFoundException("Sdx cluster not found by ID");
        }
    }

    public SdxCluster getByAccountIdAndSdxName(String userCrn, String sdxName) {
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, sdxName);
        if (sdxCluster.isPresent()) {
            return sdxCluster.get();
        } else {
            throw new NotFoundException("Sdx cluster not found");
        }
    }

    public void updateSdxStatus(Long id, SdxClusterStatus sdxClusterStatus) {
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findById(id);
        sdxCluster.ifPresentOrElse(sdx -> {
            sdx.setStatus(sdxClusterStatus);
            sdxClusterRepository.save(sdx);
        }, () -> LOGGER.info("Can not update sdx {} to {} status", id, sdxClusterStatus));
    }

    public SdxCluster createSdx(final String userCrn, final String sdxName, final SdxClusterRequest sdxClusterRequest) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(sdxName);
        sdxCluster.setAccountId(getAccountIdFromCrn(userCrn));
        sdxCluster.setClusterShape(sdxClusterRequest.getClusterShape());
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setAccessCidr(sdxClusterRequest.getAccessCidr());
        sdxCluster.setClusterShape(sdxClusterRequest.getClusterShape());
        try {
            sdxCluster.setTags(new Json(sdxClusterRequest.getTags()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Can not convert tags", e);
        }
        sdxCluster.setInitiatorUserCrn(userCrn);
        sdxCluster.setEnvName(sdxClusterRequest.getEnvironment());

        sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(sdxCluster.getAccountId(), sdxCluster.getClusterName())
                .ifPresent(foundSdx -> {
                    throw new BadRequestException("SDX cluster exists with this name: " + sdxName);
                });

        sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(sdxCluster.getAccountId(), sdxCluster.getEnvName()).stream().findFirst()
                .ifPresent(existedSdx -> {
            throw new BadRequestException("SDX cluster exists for environment name: " + existedSdx.getEnvName());
        });

        sdxCluster = sdxClusterRepository.save(sdxCluster);

        LOGGER.info("trigger SDX creation: {}", sdxCluster);
        sdxReactorFlowManager.triggerSdxCreation(sdxCluster.getId());

        return sdxCluster;
    }

    public List<SdxCluster> listSdx(String userCrn, String envName) {
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        return sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(accountIdFromCrn, envName);
    }

    public void deleteSdx(String userCrn, String sdxName) {
        LOGGER.info("Delete sdx");
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, sdxName).ifPresentOrElse(sdxCluster -> {
            sdxCluster.setStatus(SdxClusterStatus.DELETE_REQUESTED);
            sdxClusterRepository.save(sdxCluster);
            sdxReactorFlowManager.triggerSdxDeletion(sdxCluster.getId());
            LOGGER.info("sdx delete triggered: {}", sdxCluster.getClusterName());
        }, () -> {
            LOGGER.info("Can not find sdx cluster");
            throw new BadRequestException("Can not find sdx cluster");
        });
    }

    private String getAccountIdFromCrn(String userCrn) {
        Crn crn = Crn.fromString(userCrn);
        if (crn != null) {
            return crn.getAccountId();
        } else {
            throw new BadRequestException("Can not guess account ID from CRN");
        }
    }
}
