package com.sequenceiq.cloudbreak.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;

@Service
public class StackMatrixService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackMatrixService.class);

    @Inject
    private DefaultHDFEntries defaultHDFEntries;

    @Inject
    private DefaultHDPEntries defaultHDPEntries;

    @Inject
    private DefaultCDHEntries defaultCDHEntries;

    @Inject
    private DefaultAmbariRepoService defaultAmbariRepoService;

    @Inject
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Inject
    private ConverterUtil converterUtil;

    public StackMatrixV4Response getStackMatrix() {
        Map<String, DefaultHDFInfo> hdfEntries = defaultHDFEntries.getEntries();
        Map<String, DefaultHDPInfo> hdpEntries = defaultHDPEntries.getEntries();
        Map<String, DefaultCDHInfo> cdhEntries = defaultCDHEntries.getEntries();
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();

        Map<String, AmbariStackDescriptorV4Response> hdfStackDescriptors = new HashMap<>();
        for (Entry<String, DefaultHDFInfo> defaultHDFInfoEntry : hdfEntries.entrySet()) {
            DefaultHDFInfo defaultHDFInfo = defaultHDFInfoEntry.getValue();
            AmbariStackDescriptorV4Response stackDescriptorV4 = getAmbariStackDescriptor(defaultHDFInfo);
            hdfStackDescriptors.put(defaultHDFInfoEntry.getKey(), stackDescriptorV4);
        }

        Map<String, AmbariStackDescriptorV4Response> hdpStackDescriptors = new HashMap<>();
        for (Entry<String, DefaultHDPInfo> defaultHDPInfoEntry : hdpEntries.entrySet()) {
            DefaultHDPInfo defaultHDPInfo = defaultHDPInfoEntry.getValue();
            AmbariStackDescriptorV4Response stackDescriptorV4 = getAmbariStackDescriptor(defaultHDPInfo);
            hdpStackDescriptors.put(defaultHDPInfoEntry.getKey(), stackDescriptorV4);
        }

        Map<String, ClouderaManagerStackDescriptorV4Response> cdhStackDescriptors = new HashMap<>();
        for (Entry<String, DefaultCDHInfo> defaultCDHInfoEntry : cdhEntries.entrySet()) {
            DefaultCDHInfo defaultCDHInfo = defaultCDHInfoEntry.getValue();
            ClouderaManagerStackDescriptorV4Response stackDescriptorV4 = getCMStackDescriptor(defaultCDHInfo);
            cdhStackDescriptors.put(defaultCDHInfoEntry.getKey(), stackDescriptorV4);
        }

        stackMatrixV4Response.setHdf(hdfStackDescriptors);
        stackMatrixV4Response.setHdp(hdpStackDescriptors);
        stackMatrixV4Response.setCdh(cdhStackDescriptors);
        return stackMatrixV4Response;
    }

    public Set<String> getSupportedOperatingSystems(String clusterType, String clusterVersion) {
        StackMatrixV4Response stackMatrix = getStackMatrix();
        if ("HDP".equalsIgnoreCase(clusterType) || "HDF".equalsIgnoreCase(clusterType)) {
            LOGGER.debug("Get Ambari stack info for determining the supported OS types for type: {} and version: {}", clusterType, clusterVersion);
            AmbariStackDescriptorV4Response stackDescriptor = getAmbariStackDescriptor(stackMatrix, clusterType, clusterVersion);
            return stackDescriptor != null ? stackDescriptor.getAmbari().getRepository().keySet() : Collections.emptySet();
        }
        LOGGER.debug("Get Cloudera Manager stack info for determining the supported OS types for type: {} and version: {}", clusterType, clusterVersion);
        ClouderaManagerStackDescriptorV4Response cmStackDescriptor = stackMatrix.getCdh().get(clusterVersion);
        return cmStackDescriptor != null ? cmStackDescriptor.getClouderaManager().getRepository().keySet() : Collections.emptySet();
    }

    public AmbariStackDescriptorV4Response getAmbariStackDescriptor(StackMatrixV4Response stackMatrix, String clusterType, String clusterVersion) {
        Map<String, AmbariStackDescriptorV4Response> stackDescriptorMap = getStackDescriptorMap(clusterType, stackMatrix, true);
        return stackDescriptorMap.get(clusterVersion);
    }

    public Map<String, AmbariStackDescriptorV4Response> getStackDescriptorMap(String clusterType, StackMatrixV4Response stackMatrix, boolean fallbackToHDP) {
        Map<String, AmbariStackDescriptorV4Response> stackDescriptorMap = null;
        switch (clusterType) {
            case "HDP":
                stackDescriptorMap = stackMatrix.getHdp();
                break;
            case "HDF":
                stackDescriptorMap = stackMatrix.getHdf();
                break;
            default:
                LOGGER.debug("No stack descriptor map found for clusterType {}", clusterType);
                if (fallbackToHDP) {
                    LOGGER.debug("Fallback to HDP");
                    stackDescriptorMap = stackMatrix.getHdp();
                }
        }
        return stackDescriptorMap;
    }

    private AmbariStackDescriptorV4Response getAmbariStackDescriptor(StackInfo stackInfo) {
        Map<String, RepositoryInfo> ambariInfoEntries = defaultAmbariRepoService.getEntries();
        AmbariStackDescriptorV4Response stackDescriptorV4 = converterUtil.convert(stackInfo, AmbariStackDescriptorV4Response.class);
        RepositoryInfo ambariInfo = ambariInfoEntries.getOrDefault(stackDescriptorV4.getMinAmbari(), new RepositoryInfo());
        AmbariInfoV4Response ambariInfoJson = converterUtil.convert(ambariInfo, AmbariInfoV4Response.class);
        stackDescriptorV4.setAmbari(ambariInfoJson);
        return stackDescriptorV4;
    }

    private ClouderaManagerStackDescriptorV4Response getCMStackDescriptor(DefaultCDHInfo stackInfo) {
        Map<String, RepositoryInfo> clouderaManagerRepoInfoEntries = defaultClouderaManagerRepoService.getEntries();
        ClouderaManagerStackDescriptorV4Response stackDescriptorV4 = converterUtil.convert(stackInfo, ClouderaManagerStackDescriptorV4Response.class);
        RepositoryInfo cmInfo = clouderaManagerRepoInfoEntries.getOrDefault(stackDescriptorV4.getMinCM(), new RepositoryInfo());
        ClouderaManagerInfoV4Response cmInfoJson = converterUtil.convert(cmInfo, ClouderaManagerInfoV4Response.class);
        stackDescriptorV4.setClouderaManager(cmInfoJson);
        return stackDescriptorV4;
    }
}
