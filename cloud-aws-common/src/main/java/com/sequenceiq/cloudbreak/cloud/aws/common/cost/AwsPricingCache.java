package com.sequenceiq.cloudbreak.cloud.aws.common.cost;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.ARCHITECTURE;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsCredentialVerifier;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonPricingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.OfferTerm;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.PriceDimension;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.PriceListElement;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;

import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Resource;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.core.auth.policy.internal.JsonPolicyWriter;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

@Service("awsPricingCache")
public class AwsPricingCache implements PricingCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsPricingCache.class);

    private static final String PRICING_API_ENDPOINT_REGION = "us-east-1";

    private static final String AWS_STORAGE_PRICING_JSON_LOCATION = "cost/aws-storage-pricing.json";

    private static final double AWS_DEFAULT_STORAGE_PRICE = 0.055;

    private static final int HOURS_IN_30_DAYS = 720;

    private static final String GET_PRODUCTS_ENCODED_POLICY_STRING = getPolicyBase64String();

    @Inject
    private CommonAwsClient awsClient;

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private AwsCredentialVerifier awsCredentialVerifier;

    private final Map<String, Map<String, Double>> storagePriceList = loadStoragePriceList();

    private static Map<String, Map<String, Double>> loadStoragePriceList() {
        ClassPathResource classPathResource = new ClassPathResource(AWS_STORAGE_PRICING_JSON_LOCATION);
        if (classPathResource.exists()) {
            try {
                String json = FileReaderUtils.readFileFromClasspath(AWS_STORAGE_PRICING_JSON_LOCATION);
                return JsonUtil.readValue(json, new TypeReference<>() {
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to load AWS storage price json!", e);
            }
        }
        throw new RuntimeException("AWS storage price json file could not found!");
    }

    @Cacheable(cacheNames = "awsCostCache", key = "{ #region, #instanceType }")
    public Optional<Double> getPriceForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        Optional<PriceListElement> priceListElement = getPriceList(region, instanceType, extendedCloudCredential.getCloudCredential());
        if (priceListElement.isPresent()) {
            Optional<OfferTerm> offerTerm = priceListElement.get().getTerms().getOnDemand().values().stream().findFirst();
            if (offerTerm.isPresent()) {
                Optional<PriceDimension> priceDimension = offerTerm.get().getPriceDimensions().values().stream().findFirst();
                if (priceDimension.isPresent()) {
                    return Optional.of(priceDimension.get().getPricePerUnit().getUsd());
                }
            }
        }
        LOGGER.info("Couldn't find the price for region {} and instance type {}.", region, instanceType);
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

    @Cacheable(cacheNames = "awsCostCache", key = "{ #region, #storageType, #volumeSize }")
    public Optional<Double> getStoragePricePerGBHour(String region, String storageType, int volumeSize) {
        if (volumeSize == 0 || "ephemeral".equalsIgnoreCase(storageType)) {
            return Optional.empty();
        }
        Map<String, Double> pricingForRegion = storagePriceList.getOrDefault(region, Map.of());
        double priceInGBMonth = pricingForRegion.getOrDefault(storageType, AWS_DEFAULT_STORAGE_PRICE);
        return Optional.of(priceInGBMonth / HOURS_IN_30_DAYS);
    }

    private Optional<PriceListElement> getPriceList(String region, String instanceType, CloudCredential cloudCredential) {
        try {
            GetProductsResponse productsResult = getProducts(region, instanceType, cloudCredential);
            Optional<String> priceListString = productsResult.priceList().stream().findFirst();
            if (priceListString.isPresent()) {
                PriceListElement priceListElement = JsonUtil.readValue(priceListString.get(), PriceListElement.class);
                LOGGER.info("Found {} on demand terms in price list retrieved for region '{}' and instance type '{}': {}",
                        priceListElement.getTerms().getOnDemand().size(), region, instanceType, priceListElement.getTerms().getOnDemand());
                return Optional.of(priceListElement);
            }
            LOGGER.info("Couldn't find the price list for the region {} and instance type {}!", region, instanceType);
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.info("Failed to get price list from provider!", e);
            return Optional.empty();
        } catch (AwsPermissionMissingException e) {
            LOGGER.info("There is a missing permission for retrieving AWS prices (probably pricing:GetProducts)", e);
            return Optional.empty();
        }
    }

    private GetProductsResponse getProducts(String region, String instanceType, CloudCredential cloudCredential) throws AwsPermissionMissingException {
        awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential), GET_PRODUCTS_ENCODED_POLICY_STRING);
        GetProductsRequest productsRequest = GetProductsRequest.builder()
                .serviceCode("AmazonEC2")
                .formatVersion("aws_v1")
                .filters(
                        Filter.builder().type(FilterType.TERM_MATCH).field("regionCode").value(region).build(),
                        Filter.builder().type(FilterType.TERM_MATCH).field("instanceType").value(instanceType).build(),
                        Filter.builder().type(FilterType.TERM_MATCH).field("operatingSystem").value("Linux").build(),
                        Filter.builder().type(FilterType.TERM_MATCH).field("preInstalledSw").value("NA").build(),
                        Filter.builder().type(FilterType.TERM_MATCH).field("capacitystatus").value("Used").build(),
                        Filter.builder().type(FilterType.TERM_MATCH).field("tenancy").value("Shared").build())
                .build();
        AmazonPricingClient pricingClient = awsClient.createPricingClient(new AwsCredentialView(cloudCredential), PRICING_API_ENDPOINT_REGION);
        return pricingClient.getProducts(productsRequest);
    }

    private static String getPolicyBase64String() {
        Policy policy = new Policy();
        Statement statement = new Statement(Statement.Effect.Allow)
                .withResources(new Resource("*")).withActions()
                .withActions(new Action("pricing:GetProducts"));
        policy.setStatements(List.of(statement));
        String policyString = new JsonPolicyWriter().writePolicyToString(policy);
        return Base64Util.encode(policyString);
    }

    private Optional<VmTypeMeta> getVmMetadata(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(extendedCloudCredential, region,
                getCloudPlatform().name(), CdpResourceType.DEFAULT, Map.of(ARCHITECTURE, Architecture.ALL_ARCHITECTURE));
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

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
