package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AmbariInfoJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackDescriptor;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariInfoMapper;
import com.sequenceiq.cloudbreak.converter.mapper.StackInfoMapper;

@Service
public class StackMatrixService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackMatrixService.class);

    @Inject
    private DefaultHDFEntries defaultHDFEntries;

    @Inject
    private DefaultHDPEntries defaultHDPEntries;

    @Inject
    private DefaultAmbariRepoService defaultAmbariRepoService;

    @Inject
    private StackInfoMapper stackInfoMapper;

    @Inject
    private AmbariInfoMapper ambariInfoMapper;

    public StackMatrix getStackMatrix() {
        Map<String, DefaultHDFInfo> hdfEntries = defaultHDFEntries.getEntries();
        Map<String, DefaultHDPInfo> hdpEntries = defaultHDPEntries.getEntries();
        StackMatrix stackMatrix = new StackMatrix();

        Map<String, StackDescriptor> hdfStackDescriptors = new HashMap<>();
        for (Entry<String, DefaultHDFInfo> defaultHDFInfoEntry : hdfEntries.entrySet()) {
            DefaultHDFInfo defaultHDFInfo = defaultHDFInfoEntry.getValue();
            StackDescriptor stackDescriptor = getStackDescriptor(defaultHDFInfo);
            hdfStackDescriptors.put(defaultHDFInfoEntry.getKey(), stackDescriptor);
        }

        Map<String, StackDescriptor> hdpStackDescriptors = new HashMap<>();
        for (Entry<String, DefaultHDPInfo> defaultHDPInfoEntry : hdpEntries.entrySet()) {
            DefaultHDPInfo defaultHDPInfo = defaultHDPInfoEntry.getValue();
            StackDescriptor stackDescriptor = getStackDescriptor(defaultHDPInfo);
            hdpStackDescriptors.put(defaultHDPInfoEntry.getKey(), stackDescriptor);
        }

        stackMatrix.setHdf(hdfStackDescriptors);
        stackMatrix.setHdp(hdpStackDescriptors);
        return stackMatrix;
    }

    public Set<String> getSupportedOperatingSystems(String clusterType, String clusterVersion) {
        StackDescriptor stackDescriptor = getStackDescriptor(clusterType, clusterVersion);
        return stackDescriptor.getAmbari().getRepo().keySet();
    }

    public StackDescriptor getStackDescriptor(String clusterType, String clusterVersion) {
        StackMatrix stackMatrix = getStackMatrix();
        Map<String, StackDescriptor> stackDescriptorMap = getStackDescriptorMap(clusterType, stackMatrix, true);
        return stackDescriptorMap.get(clusterVersion);
    }

    public Map<String, StackDescriptor> getStackDescriptorMap(String clusterType, StackMatrix stackMatrix, boolean fallbackToHDP) {
        Map<String, StackDescriptor> stackDescriptorMap = null;
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

    private StackDescriptor getStackDescriptor(StackInfo stackInfo) {
        Map<String, AmbariInfo> ambariInfoEntries = defaultAmbariRepoService.getEntries();
        StackDescriptor stackDescriptor = stackInfoMapper.mapStackInfoToStackDescriptor(stackInfo, stackInfo.getRepo().getMpacks());
        AmbariInfo ambariInfo = ambariInfoEntries.getOrDefault(stackDescriptor.getMinAmbari(), new AmbariInfo());
        AmbariInfoJson ambariInfoJson = ambariInfoMapper.mapAmbariInfoToAmbariInfoJson(ambariInfo);
        stackDescriptor.setAmbari(ambariInfoJson);
        return stackDescriptor;
    }
}
