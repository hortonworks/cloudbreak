package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.RegionList;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;

@Component
public class GcpCloudSubnetProvider {

    public List<CreatedSubnet> provide(NetworkCreationRequest request, List<String> subnetCidrs) throws IOException {
        Compute compute = GcpStackUtil.buildCompute(request.getCloudCredential());
        String projectId = GcpStackUtil.getProjectId(request.getCloudCredential());

        List<String> az = getAvailabilityZones(compute, projectId, request.getRegion());
        List<CreatedSubnet> subnets = new ArrayList<>(subnetCidrs.size());
        for (int i = 0; i < subnetCidrs.size(); i++) {
            CreatedSubnet createdSubnet = new CreatedSubnet();
            createdSubnet.setCidr(subnetCidrs.get(i));
            if (i < az.size()) {
                createdSubnet.setAvailabilityZone(az.get(i));
            } else {
                createdSubnet.setAvailabilityZone(az.get(az.size() - 1));
            }
            subnets.add(createdSubnet);
        }
        return subnets;
    }

    private List<String> getAvailabilityZones(Compute compute, String projectId, Region region) throws IOException {
        List<String> availabilityZones = new ArrayList<>();
        RegionList regionList = compute.regions().list(projectId).execute();
        for (com.google.api.services.compute.model.Region gcpRegion : regionList.getItems()) {
            if (region == null || Strings.isNullOrEmpty(region.value()) || gcpRegion.getName().equals(region.value())) {
                for (String s : gcpRegion.getZones()) {
                    String[] split = s.split("/");
                    if (split.length > 0) {
                        availabilityZones.add(split[split.length - 1]);
                    }
                }
            }
        }
        return availabilityZones;
    }
}
