package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AmbariInfoJson;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackDescriptorV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
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

    public StackMatrixV4Response getStackMatrix() {
        Map<String, DefaultHDFInfo> hdfEntries = defaultHDFEntries.getEntries();
        Map<String, DefaultHDPInfo> hdpEntries = defaultHDPEntries.getEntries();
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();

        Map<String, StackDescriptorV4> hdfStackDescriptors = new HashMap<>();
        for (Map.Entry<String, DefaultHDFInfo> defaultHDFInfoEntry : hdfEntries.entrySet()) {
            DefaultHDFInfo defaultHDFInfo = defaultHDFInfoEntry.getValue();
            StackDescriptorV4 stackDescriptorV4 = getStackDescriptor(defaultHDFInfo);
            hdfStackDescriptors.put(defaultHDFInfoEntry.getKey(), stackDescriptorV4);
        }

        Map<String, StackDescriptorV4> hdpStackDescriptors = new HashMap<>();
        for (Map.Entry<String, DefaultHDPInfo> defaultHDPInfoEntry : hdpEntries.entrySet()) {
            DefaultHDPInfo defaultHDPInfo = defaultHDPInfoEntry.getValue();
            StackDescriptorV4 stackDescriptorV4 = getStackDescriptor(defaultHDPInfo);
            hdpStackDescriptors.put(defaultHDPInfoEntry.getKey(), stackDescriptorV4);
        }

        stackMatrixV4Response.setHdf(hdfStackDescriptors);
        stackMatrixV4Response.setHdp(hdpStackDescriptors);
        return stackMatrixV4Response;
    }

    private StackDescriptorV4 getStackDescriptor(StackInfo stackInfo) {
        Map<String, AmbariInfo> ambariInfoEntries = defaultAmbariRepoService.getEntries();
        StackDescriptorV4 stackDescriptorV4 = stackInfoMapper.mapStackInfoToStackDescriptor(stackInfo, stackInfo.getRepo().getMpacks());
        AmbariInfo ambariInfo = ambariInfoEntries.getOrDefault(stackDescriptorV4.getMinAmbari(), new AmbariInfo());
        AmbariInfoJson ambariInfoJson = ambariInfoMapper.mapAmbariInfoToAmbariInfoJson(ambariInfo);
        stackDescriptorV4.setAmbari(ambariInfoJson);
        return stackDescriptorV4;
    }
}
