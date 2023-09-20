package com.sequenceiq.cloudbreak.cloud.azure.cost;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
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
import com.sequenceiq.cloudbreak.cloud.azure.cost.model.PriceDetails;
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
                return JsonUtil.readValue(json, new TypeReference<>() { });
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
                return JsonUtil.readValue(json, new TypeReference<>() { });
            } catch (IOException e) {
                throw new RuntimeException("Failed to load Azure disk size json!", e);
            }
        }
        throw new RuntimeException("Azure disk price json file could not found!");
    }

    @Cacheable(cacheNames = "azureCostCache", key = "{ #region, #instanceType }")
    public Optional<Double> getPriceForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        Optional<PriceResponse> priceResponse = getPriceResponse(region, instanceType);
        if (priceResponse.isPresent()) {
            Optional<PriceDetails> priceDetails = priceResponse.get().getItems().stream().findFirst();
            if (priceDetails.isPresent()) {
                return Optional.of(priceDetails.get().getRetailPrice());
            }
        }
        LOGGER.info("Couldn't find the price for the requested region {} and instance type {} combination!", region, instanceType);
        return Optional.empty();
    }

    public Optional<Integer> getCpuCountForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        Optional<VmTypeMeta> instanceTypeMetadata = getVmMetadata(region, instanceType, extendedCloudCredential);
        return instanceTypeMetadata.map(VmTypeMeta::getCPU);
    }

    public Optional<Integer> getMemoryForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        Optional<VmTypeMeta> instanceTypeMetadata = getVmMetadata(region, instanceType, extendedCloudCredential);
        return instanceTypeMetadata.map(vmTypeMeta -> vmTypeMeta.getMemoryInGb().intValue());
    }

    @Cacheable(cacheNames = "azureCostCache", key = "{ #region, #storageType, #volumeSize }")
    public Optional<Double> getStoragePricePerGBHour(String region, String storageType, int volumeSize) {
        if (volumeSize == 0) {
            return Optional.empty();
        }
        Map<String, Double> pricesForRegion = storagePricingCache.getOrDefault(region, Map.of());
        Optional<String> specificStorageType = getSpecificAzureStorageTypeBySize(storageType, volumeSize);
        Double price = pricesForRegion.getOrDefault(specificStorageType.orElse(""), AZURE_DEFAULT_STORAGE_PRICE);
        return Optional.of(price / HOURS_IN_30_DAYS / volumeSize);
    }

    private Optional<String> getSpecificAzureStorageTypeBySize(String storageType, int volumeSize) {
        Map<String, Map<String, Integer>> specificVolumeTypes = azureDiskSizes.getOrDefault(storageType, Map.of());
        for (Map.Entry<String, Map<String, Integer>> entry : specificVolumeTypes.entrySet()) {
            if (volumeSize > entry.getValue().get("minSize") && volumeSize <= entry.getValue().get("maxSize")) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private Optional<PriceResponse> getPriceResponse(String region, String instanceType) {
        try {
            String query = String.format(PRICING_API_REQUEST_TEMPLATE, getQueryFilter(region, instanceType));
            WebTarget webTarget = client.target(query);
            Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
            PriceResponse priceResponse = retryableGetPriceResponse(invocationBuilder);
            return Optional.of(priceResponse);
        } catch (Exception e) {
            LOGGER.info("Couldn't get prices for Azure instance with type '{}' in region '{}'", instanceType, region, e);
            return Optional.empty();
        }
    }

    private Optional<VmTypeMeta> getVmMetadata(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(extendedCloudCredential, region,
                getCloudPlatform().name(), CdpResourceType.DEFAULT, Map.of());
        Optional<Set<VmType>> vmTypesOptional = cloudVmTypes.getCloudVmResponses().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(region))
                .map(Map.Entry::getValue)
                .findFirst();
        if (vmTypesOptional.isPresent()) {
            Optional<VmType> vmType = vmTypesOptional.get().stream().filter(x -> x.value().equals(instanceType)).findFirst();
            if (vmType.isPresent()) {
                return Optional.of(vmType.get().getMetaData());
            }
        }
        return Optional.empty();
    }

    private String getQueryFilter(String region, String instanceType) {
        Map<String, String> filters = Map.of(
                "armRegionName", "'" + region.replace(" ", "").toLowerCase(Locale.ROOT) + "'",
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
