package com.sequenceiq.cloudbreak.cloud.aws.common.cost;

import java.io.IOException;
import java.time.Duration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.amazonaws.services.pricing.AWSPricing;
import com.amazonaws.services.pricing.AWSPricingClientBuilder;
import com.amazonaws.services.pricing.model.Filter;
import com.amazonaws.services.pricing.model.FilterType;
import com.amazonaws.services.pricing.model.GetProductsRequest;
import com.amazonaws.services.pricing.model.GetProductsResult;
import com.amazonaws.services.pricing.model.NotFoundException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.PriceListElement;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.cost.model.PricingCacheKey;

@Service("awsPricingCache")
public class AwsPricingCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsPricingCache.class);

    private static final String PRICING_API_ENDPOINT_REGION = "us-east-1";

    private static final int CACHE_MAX_SIZE = 100;

    private static final int CACHE_RETENTION_IN_MINUTES = 5;

    @Inject
    private AWSPricing awsPricing;

    private final Cache<PricingCacheKey, PriceListElement> cache;

    public AwsPricingCache() {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                .expireAfterAccess(Duration.ofMinutes(CACHE_RETENTION_IN_MINUTES))
                .build();
    }

    public double getPriceForInstanceType(String region, String instanceType) {
        PriceListElement priceListElement = getPriceList(region, instanceType);
        return priceListElement.getTerms().getOnDemand().values().stream().findFirst().orElseThrow(() ->
                        new NotFoundException(String.format("Couldn't find the price for region [%s] and instance type [%s].", region, instanceType)))
                .getPriceDimensions().values().stream().findFirst().orElseThrow(() ->
                        new NotFoundException(String.format("Couldn't find the price for region [%s] and instance type [%s].", region, instanceType)))
                .getPricePerUnit().getUsd();
    }

    public int getCpuCountForInstanceType(String region, String instanceType) {
        PriceListElement priceListElement = getPriceList(region, instanceType);
        return priceListElement.getProduct().getAttributes().getVcpu();
    }

    public int getMemoryForInstanceType(String region, String instanceType) {
        PriceListElement priceListElement = getPriceList(region, instanceType);
        String memory = priceListElement.getProduct().getAttributes().getMemory();
        return Integer.parseInt(memory.replaceAll("\\D", ""));
    }

    private PriceListElement getPriceList(String region, String instanceType) {
        PriceListElement value = cache.getIfPresent(new PricingCacheKey(region, instanceType));
        if (value == null) {
            GetProductsResult productsResult = getProducts(region, instanceType);
            String priceListString = productsResult.getPriceList().stream().findFirst()
                    .orElseThrow(() -> new NotFoundException("Couldn't find the price list for the requested region and instance type combination!"));
            try {
                PriceListElement priceListElement = JsonUtil.readValue(priceListString, PriceListElement.class);
                cache.put(new PricingCacheKey(region, instanceType), priceListElement);
                return priceListElement;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.info("PriceList for region [{}] and instance type [{}] found in cache.", region, instanceType);
            return value;
        }
    }

    private GetProductsResult getProducts(String region, String instanceType) {
        GetProductsRequest productsRequest = new GetProductsRequest()
                .withServiceCode("AmazonEC2")
                .withFormatVersion("aws_v1")
                .withFilters(
                        new Filter().withType(FilterType.TERM_MATCH).withField("regionCode").withValue(region),
                        new Filter().withType(FilterType.TERM_MATCH).withField("instanceType").withValue(instanceType),
                        new Filter().withType(FilterType.TERM_MATCH).withField("operatingSystem").withValue("Linux"),
                        new Filter().withType(FilterType.TERM_MATCH).withField("preInstalledSw").withValue("NA"),
                        new Filter().withType(FilterType.TERM_MATCH).withField("capacitystatus").withValue("Used"),
                        new Filter().withType(FilterType.TERM_MATCH).withField("tenancy").withValue("Shared"));
        return awsPricing.getProducts(productsRequest);
    }

    @Bean
    public AWSPricing getAwsPricing() {
        return AWSPricingClientBuilder.standard().withRegion(PRICING_API_ENDPOINT_REGION).build();
    }
}
