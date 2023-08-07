package com.sequenceiq.cloudbreak.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Service
public class DefaultClouderaManagerRepoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClouderaManagerRepoService.class);

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private StackTypeResolver stackTypeResolver;

    @Inject
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    private Map<String, RepositoryInfo> entries = new HashMap<>();

    public ClouderaManagerRepo getDefault(String osType, String os, String clusterType, String clusterVersion,
        ImageCatalogPlatform platform) throws CloudbreakImageCatalogException {
        if (StackType.CDH.name().equals(clusterType)) {
            StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix(platform, os);
            Map<String, ClouderaManagerStackDescriptorV4Response> stackDescriptorMap = stackMatrixV4Response.getCdh();

            if (stackDescriptorMap != null) {
                Optional<Entry<String, ClouderaManagerStackDescriptorV4Response>> descriptorEntry = stackDescriptorMap.entrySet().stream()
                        .filter(stackDescriptorEntry ->
                                clusterVersion == null || clusterVersion.equals(stackDescriptorEntry.getKey()))
                        .max(Comparator.comparing(Entry::getKey));
                if (descriptorEntry.isPresent()) {
                    Entry<String, ClouderaManagerStackDescriptorV4Response> stackDescriptorEntry = descriptorEntry.get();
                    ClouderaManagerInfoV4Response clouderaManagerInfoJson = stackDescriptorEntry.getValue().getClouderaManager();
                    if (clouderaManagerInfoJson.getRepository().get(osType) != null) {
                        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
                        clouderaManagerRepo.setPredefined(false);
                        clouderaManagerRepo.setVersion(clouderaManagerInfoJson.getVersion());
                        clouderaManagerRepo.setBaseUrl(clouderaManagerInfoJson.getRepository().get(osType).getBaseUrl());
                        clouderaManagerRepo.setGpgKeyUrl(clouderaManagerInfoJson.getRepository().get(osType).getGpgKeyUrl());
                        return clouderaManagerRepo;
                    }
                }
            }
        }

        LOGGER.info("Missing Cloudera Manager Repo information for os: {} clusterType: {} clusterVersion: {}", osType, clusterType, clusterVersion);
        return null;
    }

    public Optional<ClouderaManagerRepo> getClouderaManagerRepo(Image imgFromCatalog)
            throws CloudbreakImageCatalogException {
        return imgFromCatalog.getStackDetails() != null
                ? Optional.of(getClouderaManagerRepo(imgFromCatalog, stackTypeResolver.determineStackType(imgFromCatalog.getStackDetails())))
                : Optional.empty();
    }

    public ClouderaManagerRepo getClouderaManagerRepo(Image imgFromCatalog, StackType stackType)
            throws CloudbreakImageCatalogException {
        if (imgFromCatalog.getRepo() != null) {
            if (StackType.CDH.equals(stackType)) {
                ClouderaManagerRepo clouderaManagerRepo = imageToClouderaManagerRepoConverter.convert(imgFromCatalog);
                if (Objects.isNull(clouderaManagerRepo) || clouderaManagerRepo.getBaseUrl() == null) {
                    throw new CloudbreakImageCatalogException(
                            String.format("Cloudera Manager repo was not found in image for os: '%s'.", imgFromCatalog.getOsType()));
                }
                return clouderaManagerRepo;
            } else {
                throw new CloudbreakImageCatalogException(String.format("Invalid repo present in image catalog: '%s'.", imgFromCatalog.getRepo()));
            }
        } else {
            throw new CloudbreakImageCatalogException(String.format("Invalid repo present in image catalog: '%s'.", imgFromCatalog.getRepo()));
        }
    }

}
