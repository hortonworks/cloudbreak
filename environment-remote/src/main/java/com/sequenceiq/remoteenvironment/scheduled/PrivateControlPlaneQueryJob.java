package com.sequenceiq.remoteenvironment.scheduled;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.remotecluster.RemoteClusterProto.PvcControlPlaneConfiguration;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequests;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;
import com.sequenceiq.remoteenvironment.service.PrivateControlPlaneService;

@Component
public class PrivateControlPlaneQueryJob extends MdcQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateControlPlaneQueryJob.class);

    @Inject
    private RemoteClusterServiceClient grpcRemoteClusterClient;

    @Inject
    private PrivateControlPlaneService privateControlPlaneService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        queryPrivateControlPlaneConfigs();
    }

    public void queryPrivateControlPlaneConfigs() throws JobExecutionException {
        LOGGER.debug("query all remote clusters");
        try {
            List<PvcControlPlaneConfiguration> remoteControlPlanes = grpcRemoteClusterClient.listAllPrivateControlPlanes().stream()
                    .filter(pcp -> StringUtils.isNotEmpty(pcp.getPvcCrn()) && Crn.isCrn(pcp.getPvcCrn()))
                    .toList();
            if (!remoteControlPlanes.isEmpty()) {
                List<PrivateControlPlane> controlPlanesInOurDatabase = privateControlPlaneService.findAll();

                Set<String> allOurCrn = controlPlanesInOurDatabase.stream()
                        .map(PrivateControlPlane::getResourceCrn)
                        .collect(toSet());
                Set<String> allCPCrn = remoteControlPlanes.stream()
                        .map(PvcControlPlaneConfiguration::getPvcCrn)
                        .collect(toSet());

                allOurCrn = cleanupRecordsWhichAreNotRegisteredAnymore(allOurCrn, allCPCrn);
                registerWhichAreNewRegisteredOnRemoteClusterSide(remoteControlPlanes, allOurCrn, allCPCrn);
                updateDataInOurDatabase(remoteControlPlanes, controlPlanesInOurDatabase);
            }
        } catch (Exception e) {
            LOGGER.error("Could not query remote clusters.", e);
            throw new JobExecutionException("Could not query and update private control planes.", e);
        }
    }

    private void registerWhichAreNewRegisteredOnRemoteClusterSide(List<PvcControlPlaneConfiguration> remoteControlPlanes,
        Set<String> allOurCrn, Set<String> allCPCrn) {
        Set<String> crnsOnlyOnRemoteClusterSide = new HashSet<>(CollectionUtils.removeAll(allCPCrn, allOurCrn));
        if (crnsOnlyOnRemoteClusterSide.size() > 0) {
            PrivateControlPlaneRegistrationRequests registrationRequests = new PrivateControlPlaneRegistrationRequests();
            for (String crn : crnsOnlyOnRemoteClusterSide) {
                Optional<PvcControlPlaneConfiguration> first = remoteControlPlanes.stream()
                        .filter(e -> e.getPvcCrn().equalsIgnoreCase(crn))
                        .findFirst();
                if (first.isPresent()) {
                    PvcControlPlaneConfiguration pvcControlPlaneConfiguration = first.get();
                    PrivateControlPlaneRegistrationRequest request = new PrivateControlPlaneRegistrationRequest();
                    request.setCrn(pvcControlPlaneConfiguration.getPvcCrn());
                    request.setName(pvcControlPlaneConfiguration.getName());
                    request.setUrl(pvcControlPlaneConfiguration.getBaseUrl());
                    registrationRequests.getItems().add(request);
                }
            }
            LOGGER.debug("adding {} items because the data not yet in our database.", registrationRequests);
            privateControlPlaneService.register(registrationRequests);
        }
    }

    private Set<String> cleanupRecordsWhichAreNotRegisteredAnymore(Set<String> allOurCrn, Set<String> allCPCrn) {
        Set<String> actualOnlyInOurDb = new HashSet<>(CollectionUtils.removeAll(allOurCrn, allCPCrn));
        if (!actualOnlyInOurDb.isEmpty()) {
            LOGGER.debug("delete {} crns because those are not presented in remote cluster service.", actualOnlyInOurDb);
            privateControlPlaneService.deleteByResourceCrns(actualOnlyInOurDb);
            List<PrivateControlPlane> controlPlanesInOurDatabase = privateControlPlaneService.findAll();
            return controlPlanesInOurDatabase.stream()
                    .map(PrivateControlPlane::getResourceCrn)
                    .collect(toSet());
        }
        return allOurCrn;
    }

    private void updateDataInOurDatabase(List<PvcControlPlaneConfiguration> controlPlaneConfigurations,
        List<PrivateControlPlane> controlPlanesInOurDatabase) {
        for (PrivateControlPlane item : controlPlanesInOurDatabase) {
            Optional<PvcControlPlaneConfiguration> registeredControlPlane = controlPlaneConfigurations.stream()
                    .filter(e -> e.getPvcCrn().equalsIgnoreCase(item.getResourceCrn()))
                    .findFirst();

            if (registeredControlPlane.isPresent()) {
                PvcControlPlaneConfiguration pvcControlPlaneConfiguration = registeredControlPlane.get();
                if (urlOrNameChangedOREmptyBaseUrl(item, pvcControlPlaneConfiguration) || emptyPvcAccountId(item)) {
                    Crn pvcCrn = Crn.fromString(pvcControlPlaneConfiguration.getPvcCrn());
                    item.setName(pvcControlPlaneConfiguration.getName());
                    item.setUrl(pvcControlPlaneConfiguration.getBaseUrl());
                    item.setPrivateCloudAccountId(pvcCrn.getResource());
                    LOGGER.debug("updating control plane configurations in our database with {} crn because " +
                                    "the data on our side is out of sync name: {} url: {}.",
                            item.getResourceCrn(), item.getName(), item.getUrl());
                    privateControlPlaneService.pureSave(item);
                }
            }
        }
    }

    private boolean urlOrNameChangedOREmptyBaseUrl(PrivateControlPlane item, PvcControlPlaneConfiguration pvcControlPlaneConfiguration) {
        return !pvcControlPlaneConfiguration.getName().equals(item.getName()) || !pvcControlPlaneConfiguration.getBaseUrl().equals(item.getUrl());
    }

    private boolean emptyPvcAccountId(PrivateControlPlane item) {
        return Strings.isNullOrEmpty(item.getPrivateCloudAccountId());
    }
}
