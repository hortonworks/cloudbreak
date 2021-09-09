package com.sequenceiq.cloudbreak.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.RepositoryInfoToClouderaManagerInfoV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.StackInfoToClouderaManagerStackDescriptorV4ResponseConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm.ClouderaManagerProductToClouderaManagerProductV4Response;

@Service
public class StackMatrixService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackMatrixService.class);

    @Inject
    private ImageBasedDefaultCDHEntries imageBasedDefaultCDHEntries;

    @Inject
    private RepositoryInfoToClouderaManagerInfoV4ResponseConverter repositoryInfoToClouderaManagerInfoV4ResponseConverter;

    @Inject
    private StackInfoToClouderaManagerStackDescriptorV4ResponseConverter stackInfoToClouderaManagerStackDescriptorV4ResponseConverter;

    public StackMatrixV4Response getStackMatrix(String platform) throws CloudbreakImageCatalogException {
        return getStackMatrix(0L, platform, null);
    }

    public StackMatrixV4Response getStackMatrix(Long workspaceId, String platform, String imageCatalogName) throws CloudbreakImageCatalogException {
        LOGGER.debug("Generate stack matrix from images using '{}' image catalog and '{}' platform.", imageCatalogName, platform);
        return getImageBasedStackMatrix(workspaceId, platform, imageCatalogName);
    }

    private StackMatrixV4Response getImageBasedStackMatrix(Long workspaceId, String platform, String imageCatalogName) throws CloudbreakImageCatalogException {
        Map<String, ImageBasedDefaultCDHInfo> cdhEntries = imageBasedDefaultCDHEntries.getEntries(workspaceId, platform, imageCatalogName);
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();

        Map<String, ClouderaManagerStackDescriptorV4Response> cdhStackDescriptors = new HashMap<>();
        for (Entry<String, ImageBasedDefaultCDHInfo> defaultCDHInfoEntry : cdhEntries.entrySet()) {
            DefaultCDHInfo defaultCDHInfo = defaultCDHInfoEntry.getValue().getDefaultCDHInfo();
            Image image = defaultCDHInfoEntry.getValue().getImage();
            ClouderaManagerStackDescriptorV4Response stackDescriptorV4 = getImageBasedCMStackDescriptor(defaultCDHInfo, image);
            cdhStackDescriptors.put(defaultCDHInfoEntry.getKey(), stackDescriptorV4);
        }

        stackMatrixV4Response.setCdh(cdhStackDescriptors);
        return stackMatrixV4Response;
    }

    public Set<String> getSupportedOperatingSystems(Long workspaceId, String clusterVersion, String platform, String imageCatalogName) throws Exception {
        StackMatrixV4Response stackMatrix = getStackMatrix(workspaceId, platform, imageCatalogName);
        LOGGER.debug("Get Cloudera Manager stack info for determinigetSupportedOperationSystemsng the supported OS types for version: {}", clusterVersion);
        ClouderaManagerStackDescriptorV4Response cmStackDescriptor = stackMatrix.getCdh().get(clusterVersion);
        return cmStackDescriptor != null ? cmStackDescriptor.getClouderaManager().getRepository().keySet() : Collections.emptySet();
    }

    private ClouderaManagerStackDescriptorV4Response getImageBasedCMStackDescriptor(DefaultCDHInfo stackInfo, Image image) {
        ClouderaManagerStackDescriptorV4Response stackDescriptorV4 = stackInfoToClouderaManagerStackDescriptorV4ResponseConverter.convert(stackInfo);
        RepositoryInfo cmInfo = new RepositoryInfo();
        cmInfo.setVersion(image.getPackageVersion(ImagePackageVersion.CM));
        cmInfo.setRepo(image.getRepo().entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> {
            RepositoryDetails repo = new RepositoryDetails();
            repo.setBaseurl(e.getValue());

            return repo;
        })));

        ClouderaManagerInfoV4Response cmInfoJson = repositoryInfoToClouderaManagerInfoV4ResponseConverter.convert(cmInfo);
        stackDescriptorV4.setClouderaManager(cmInfoJson);
        for (ClouderaManagerProduct parcel : stackInfo.getParcels()) {
            stackDescriptorV4.getProducts().add(ClouderaManagerProductToClouderaManagerProductV4Response.convert(parcel));
        }
        stackDescriptorV4.setProductDefinitions(stackInfo.getCsd());
        return stackDescriptorV4;
    }
}
