package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AmbariInfoJson;
import com.sequenceiq.cloudbreak.api.model.StackDescriptor;
import com.sequenceiq.cloudbreak.api.model.StackMatrix;
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
        for (Map.Entry<String, DefaultHDFInfo> defaultHDFInfoEntry : hdfEntries.entrySet()) {
            DefaultHDFInfo defaultHDFInfo = defaultHDFInfoEntry.getValue();
            StackDescriptor stackDescriptor = getStackDescriptor(defaultHDFInfo);
            hdfStackDescriptors.put(defaultHDFInfoEntry.getKey(), stackDescriptor);
        }

        Map<String, StackDescriptor> hdpStackDescriptors = new HashMap<>();
        for (Map.Entry<String, DefaultHDPInfo> defaultHDPInfoEntry : hdpEntries.entrySet()) {
            DefaultHDPInfo defaultHDPInfo = defaultHDPInfoEntry.getValue();
            StackDescriptor stackDescriptor = getStackDescriptor(defaultHDPInfo);
            hdpStackDescriptors.put(defaultHDPInfoEntry.getKey(), stackDescriptor);
        }

        stackMatrix.setHdf(hdfStackDescriptors);
        stackMatrix.setHdp(hdpStackDescriptors);
        return stackMatrix;
    }

    private StackDescriptor getStackDescriptor(StackInfo stackInfo) {
        Map<String, AmbariInfo> ambariInfoEntries = defaultAmbariRepoService.getEntries();
        StackDescriptor stackDescriptor = stackInfoMapper.mapStackInfoToStackDescriptor(stackInfo);
        AmbariInfo ambariInfo = ambariInfoEntries.getOrDefault(stackDescriptor.getMinAmbari(), new AmbariInfo());
        AmbariInfoJson ambariInfoJson = ambariInfoMapper.mapAmbariInfoToAmbariInfoJson(ambariInfo);
        stackDescriptor.setAmbari(ambariInfoJson);
        return stackDescriptor;
    }
}
