package com.sequenceiq.cloudbreak.cloud.gcp.cost;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.common.api.type.CdpResourceType;

@Service("gcpPricingCache")
public class GcpPricingCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpPricingCache.class);

    private final Table<String, String, VmTypeMeta> vmCache = HashBasedTable.create();

    @Inject
    private CloudParameterService cloudParameterService;

    public double getPriceForInstanceType(String region, String instanceType) {
//        try (CloudCatalogClient cloudCatalogClient = CloudCatalogClient.create()) {
//            CloudCatalogClient.ListServicesPagedResponse response = cloudCatalogClient.listServices();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return 0.0;
    }

    public int getCpuCountForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        VmTypeMeta instanceTypeMetadata = getVmMetadata(region, instanceType, extendedCloudCredential);
        return instanceTypeMetadata.getCPU();
    }

    public int getMemoryForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        VmTypeMeta instanceTypeMetadata = getVmMetadata(region, instanceType, extendedCloudCredential);
        return instanceTypeMetadata.getMemoryInGb().intValue();
    }

    private VmTypeMeta getVmMetadata(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        if (vmCache.contains(region, instanceType)) {
            LOGGER.info("VM metadata for region [{}] and instance type [{}] found in cache.", region, instanceType);
            return vmCache.get(region, instanceType);
        } else {
            CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(extendedCloudCredential, region, "GCP", CdpResourceType.DEFAULT, Map.of());
            Set<VmType> vmTypes = cloudVmTypes.getCloudVmResponses().get(region);
            VmTypeMeta instanceTypeMetadata = vmTypes.stream().filter(x -> x.value().equals(instanceType)).findFirst()
                    .orElseThrow(() -> new NotFoundException("Couldn't find the VM metadata for the requested region and instance type combination!"))
                    .getMetaData();
            vmCache.put(region, instanceType, instanceTypeMetadata);
            return instanceTypeMetadata;
        }
    }
}
