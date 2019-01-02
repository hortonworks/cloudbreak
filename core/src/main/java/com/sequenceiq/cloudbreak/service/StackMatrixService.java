package com.sequenceiq.cloudbreak.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;

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
    private ConverterUtil converterUtil;

    public StackMatrixV4Response getStackMatrix() {
        Map<String, DefaultHDFInfo> hdfEntries = defaultHDFEntries.getEntries();
        Map<String, DefaultHDPInfo> hdpEntries = defaultHDPEntries.getEntries();
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();

        Map<String, StackDescriptorV4Response> hdfStackDescriptors = new HashMap<>();
        for (Map.Entry<String, DefaultHDFInfo> defaultHDFInfoEntry : hdfEntries.entrySet()) {
            DefaultHDFInfo defaultHDFInfo = defaultHDFInfoEntry.getValue();
            StackDescriptorV4Response stackDescriptorV4 = getStackDescriptor(defaultHDFInfo);
            hdfStackDescriptors.put(defaultHDFInfoEntry.getKey(), stackDescriptorV4);
        }

        Map<String, StackDescriptorV4Response> hdpStackDescriptors = new HashMap<>();
        for (Map.Entry<String, DefaultHDPInfo> defaultHDPInfoEntry : hdpEntries.entrySet()) {
            DefaultHDPInfo defaultHDPInfo = defaultHDPInfoEntry.getValue();
            StackDescriptorV4Response stackDescriptorV4 = getStackDescriptor(defaultHDPInfo);
            hdpStackDescriptors.put(defaultHDPInfoEntry.getKey(), stackDescriptorV4);
        }

        stackMatrixV4Response.setHdf(hdfStackDescriptors);
        stackMatrixV4Response.setHdp(hdpStackDescriptors);
        return stackMatrixV4Response;
    }

    public Set<String> getSupportedOperatingSystems(String clusterType, String clusterVersion) {
        StackDescriptorV4Response stackDescriptor = getStackDescriptor(clusterType, clusterVersion);
        if (stackDescriptor != null) {
            return stackDescriptor.getAmbari().getRepository().keySet();
        } else {
            return Collections.emptySet();
        }
    }

    public StackDescriptorV4Response getStackDescriptor(String clusterType, String clusterVersion) {
        StackMatrixV4Response stackMatrix = getStackMatrix();
        Map<String, StackDescriptorV4Response> stackDescriptorMap = getStackDescriptorMap(clusterType, stackMatrix, true);
        return stackDescriptorMap.get(clusterVersion);
    }

    public Map<String, StackDescriptorV4Response> getStackDescriptorMap(String clusterType, StackMatrixV4Response stackMatrix, boolean fallbackToHDP) {
        Map<String, StackDescriptorV4Response> stackDescriptorMap = null;
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

    private StackDescriptorV4Response getStackDescriptor(StackInfo stackInfo) {
        Map<String, AmbariInfo> ambariInfoEntries = defaultAmbariRepoService.getEntries();
        StackDescriptorV4Response stackDescriptorV4 = converterUtil.convert(stackInfo, StackDescriptorV4Response.class);
        AmbariInfo ambariInfo = ambariInfoEntries.getOrDefault(stackDescriptorV4.getMinAmbari(), new AmbariInfo());
        AmbariInfoV4Response ambariInfoJson = converterUtil.convert(ambariInfo, AmbariInfoV4Response.class);
        stackDescriptorV4.setAmbari(ambariInfoJson);
        return stackDescriptorV4;
    }
}
