package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

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

    private StackDescriptorV4Response getStackDescriptor(StackInfo stackInfo) {
        Map<String, AmbariInfo> ambariInfoEntries = defaultAmbariRepoService.getEntries();
        StackDescriptorV4Response stackDescriptorV4 = converterUtil.convert(stackInfo, StackDescriptorV4Response.class);
        AmbariInfo ambariInfo = ambariInfoEntries.getOrDefault(stackDescriptorV4.getMinAmbari(), new AmbariInfo());
        AmbariInfoV4Response ambariInfoJson = converterUtil.convert(ambariInfo, AmbariInfoV4Response.class);
        stackDescriptorV4.setAmbari(ambariInfoJson);
        return stackDescriptorV4;
    }
}
