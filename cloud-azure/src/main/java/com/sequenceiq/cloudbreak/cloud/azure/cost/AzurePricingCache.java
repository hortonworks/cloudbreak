package com.sequenceiq.cloudbreak.cloud.azure.cost;

import java.io.IOException;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.cloud.azure.cost.model.PriceResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.CdpResourceType;

@Service("azurePricingCache")
public class AzurePricingCache implements PricingCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePricingCache.class);

    private static final String PRICING_API_REQUEST_TEMPLATE = "https://prices.azure.com/api/retail/prices?$filter=%s";

    private static final String AZURE_STORAGE_PRICING_JSON_LOCATION = "cost/azure-storage-pricing.json";

    private static final String AZURE_DISK_SIZES_JSON_LOCATION = "cost/azure-disk-sizes.json";

    private static final double AZURE_DEFAULT_STORAGE_PRICE = 5.89;

    private static final int MAX_ATTEMPTS = 5;

    private static final int BACKOFF_DELAY = 500;

    private static final int HOURS_IN_30_DAYS = 720;

    private final Client client = ClientBuilder.newClient();

    @Inject
    private CloudParameterService cloudParameterService;

    private final Map<String, Map<String, Double>> storagePricingCache = loadStoragePricing();

    private final Map<String, Map<String, Map<String, Integer>>> azureDiskSizes = loadAzureDiskSizes();

    private static Map<String, Map<String, Double>> loadStoragePricing() {
        ClassPathResource classPathResource = new ClassPathResource(AZURE_STORAGE_PRICING_JSON_LOCATION);
        if (classPathResource.exists()) {
            try {
                String json = FileReaderUtils.readFileFromClasspath(AZURE_STORAGE_PRICING_JSON_LOCATION);
                return JsonUtil.readValue(json, new TypeReference<>() {
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to load Azure storage price json!", e);
            }
        }
        throw new RuntimeException("Azure storage price json file could not found!");
    }

    private static Map<String, Map<String, Map<String, Integer>>> loadAzureDiskSizes() {
        ClassPathResource classPathResource = new ClassPathResource(AZURE_DISK_SIZES_JSON_LOCATION);
        if (classPathResource.exists()) {
            try {
                String json = FileReaderUtils.readFileFromClasspath(AZURE_DISK_SIZES_JSON_LOCATION);
                return JsonUtil.readValue(json, new TypeReference<>() {
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to load Azure disk size json!", e);
            }
        }
        throw new RuntimeException("Azure disk price json file could not found!");
    }

    @Cacheable(cacheNames = "azureCostCache", key = "{ #region, #instanceType }")
    public double getPriceForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        PriceResponse priceResponse = getPriceResponse(region, instanceType);
        return priceResponse.getItems().stream().findFirst()
                .orElseThrow(() -> new NotFoundException(
                        String.format("Couldn't find the price for the requested region [%s] and instance type [%s] combination!", region, instanceType)))
                .getRetailPrice();
    }

    @Cacheable(cacheNames = "azureCostCache", key = "{ #region, #instanceType }")
    public int getCpuCountForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        VmTypeMeta instanceTypeMetadata = getVmMetadata(region, instanceType, extendedCloudCredential);
        return instanceTypeMetadata.getCPU();
    }

    @Cacheable(cacheNames = "azureCostCache", key = "{ #region, #instanceType }")
    public int getMemoryForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        VmTypeMeta instanceTypeMetadata = getVmMetadata(region, instanceType, extendedCloudCredential);
        return instanceTypeMetadata.getMemoryInGb().intValue();
    }

    @Cacheable(cacheNames = "azureCostCache", key = "{ #region, #storageType, #volumeSize }")
    public double getStoragePricePerGBHour(String region, String storageType, int volumeSize) {
        if (volumeSize == 0) {
            return 0.0;
        }
        Map<String, Double> pricesForRegion = storagePricingCache.getOrDefault(region, Map.of());
        String specificStorageType = getSpecificAzureStorageTypeBySize(storageType, volumeSize);
        return pricesForRegion.getOrDefault(specificStorageType, AZURE_DEFAULT_STORAGE_PRICE) / HOURS_IN_30_DAYS / volumeSize;
    }

    private String getSpecificAzureStorageTypeBySize(String storageType, int volumeSize) {
        Map<String, Map<String, Integer>> specificVolumeTypes = azureDiskSizes.getOrDefault(storageType, Map.of());
        for (Map.Entry<String, Map<String, Integer>> entry : specificVolumeTypes.entrySet()) {
            if (volumeSize > entry.getValue().get("minSize") && volumeSize <= entry.getValue().get("maxSize")) {
                return entry.getKey();
            }
        }
        return "";
    }

    private PriceResponse getPriceResponse(String region, String instanceType) {
        String query = String.format(PRICING_API_REQUEST_TEMPLATE, getQueryFilter(region, instanceType));
        WebTarget webTarget = client.target(query);
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
        return retryableGetPriceResponse(invocationBuilder);
    }

    private VmTypeMeta getVmMetadata(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(extendedCloudCredential, region, "AZURE", CdpResourceType.DEFAULT, Map.of());
        Set<VmType> vmTypes = cloudVmTypes.getCloudVmResponses().get(region);
        return vmTypes.stream().filter(x -> x.value().equals(instanceType)).findFirst()
                .orElseThrow(() ->
                        new NotFoundException(String.format("Couldn't find the price list for the region %s and instance type %s!", region, instanceType)))
                .getMetaData();
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

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
