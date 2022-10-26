package com.sequenceiq.cloudbreak.cloud.azure.cost;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.cloud.azure.cost.model.PriceResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.common.api.type.CdpResourceType;

@Service("azurePricingCache")
public class AzurePricingCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePricingCache.class);

    private static final String PRICING_API_REQUEST_TEMPLATE = "https://prices.azure.com/api/retail/prices?$filter=%s";

    private static final int MAX_ATTEMPTS = 5;

    private static final int BACKOFF_DELAY = 500;

    private final Table<String, String, PriceResponse> priceCache = HashBasedTable.create();

    private final Table<String, String, VmTypeMeta> vmCache = HashBasedTable.create();

    @Inject
    private CloudParameterService cloudParameterService;

    public double getPriceForInstanceType(String region, String instanceType) {
        PriceResponse priceResponse = getPriceResponse(region, instanceType);
        return priceResponse.getItems().stream().findFirst().orElseThrow(() -> new NotFoundException(
                String.format("Couldn't find the price for the requested region [%s] and instance type [%s] combination!", region, instanceType)))
                .getRetailPrice();
    }

    public int getCpuCountForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        VmTypeMeta instanceTypeMetadata = getVmMetadata(region, instanceType, extendedCloudCredential);
        return instanceTypeMetadata.getCPU();
    }

    public int getMemoryForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        VmTypeMeta instanceTypeMetadata = getVmMetadata(region, instanceType, extendedCloudCredential);
        return instanceTypeMetadata.getMemoryInGb().intValue();
    }

    private PriceResponse getPriceResponse(String region, String instanceType) {
        if (priceCache.contains(region, instanceType)) {
            LOGGER.info("Price for region [{}] and instance type [{}] found in cache.", region, instanceType);
            return priceCache.get(region, instanceType);
        } else {
            Client client = ClientBuilder.newClient();
            String query = String.format(PRICING_API_REQUEST_TEMPLATE, getQueryFilter(region, instanceType));
            WebTarget webTarget = client.target(query);
            Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
            PriceResponse priceResponse = retryableGetPriceResponse(invocationBuilder);
            priceCache.put(region, instanceType, priceResponse);
            return priceResponse;
        }
    }

    private VmTypeMeta getVmMetadata(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        if (vmCache.contains(region, instanceType)) {
            LOGGER.info("VM metadata for region [{}] and instance type [{}] found in cache.", region, instanceType);
            return vmCache.get(region, instanceType);
        } else {
            CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(extendedCloudCredential, region, "AZURE", CdpResourceType.DEFAULT, Map.of());
            Set<VmType> vmTypes = cloudVmTypes.getCloudVmResponses().get(region);
            VmTypeMeta instanceTypeMetadata = vmTypes.stream().filter(x -> x.value().equals(instanceType)).findFirst()
                    .orElseThrow(() -> new NotFoundException("Couldn't find the VM metadata for the requested region and instance type combination!"))
                    .getMetaData();
            vmCache.put(region, instanceType, instanceTypeMetadata);
            return instanceTypeMetadata;
        }
    }

    private String getQueryFilter(String region, String instanceType) {
        Map<String, String> filters = Map.of(
                "armRegionName", "'" + region.replace(" ", "").toLowerCase() + "'",
                "armSkuName", "'" + instanceType + "'",
                "serviceName", "'Virtual Machines'",
                "priceType", "'Consumption'",
                "contains(productName, 'Windows')", "false",
                "contains(skuName, 'Spot')", "false",
                "contains(skuName, 'Low Priority')", "false");
        String queryFilter = filters.entrySet().stream()
                .map(x -> x.getKey() + " eq " + x.getValue())
                .collect(Collectors.joining(" and "));
        return URLEncoder.encode(queryFilter, StandardCharsets.UTF_8);
    }

    @Retryable(maxAttempts = MAX_ATTEMPTS, backoff = @Backoff(delay = BACKOFF_DELAY))
    public PriceResponse retryableGetPriceResponse(Invocation.Builder invocationBuilder) {
        LOGGER.info("Retryable GET for PriceResponse called with max retries [{}] and backoff delay [{}].", MAX_ATTEMPTS, BACKOFF_DELAY);
        return invocationBuilder.get(PriceResponse.class);
    }
}
