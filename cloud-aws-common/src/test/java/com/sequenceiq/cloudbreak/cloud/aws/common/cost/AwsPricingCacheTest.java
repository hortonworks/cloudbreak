package com.sequenceiq.cloudbreak.cloud.aws.common.cost;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.pricing.AWSPricing;
import com.amazonaws.services.pricing.model.GetProductsRequest;
import com.amazonaws.services.pricing.model.GetProductsResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.Attributes;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.OfferTerm;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.PriceDimension;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.PriceListElement;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.PricePerUnit;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.Product;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.Terms;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@ExtendWith(MockitoExtension.class)
public class AwsPricingCacheTest {

    private static final String REGION = "region";

    private static final String INSTANCE_TYPE = "instanceType";

    @Mock
    private AWSPricing awsPricing;

    @InjectMocks
    private AwsPricingCache underTest;

    @BeforeEach
    void setup() {
        when(awsPricing.getProducts(any(GetProductsRequest.class))).thenReturn(getGetProductsResult());
    }

    @Test
    void getUsdPrice() {
        double price = underTest.getPriceForInstanceType(REGION, INSTANCE_TYPE);

        Assertions.assertEquals(0.69, price);
    }

    @Test
    void getCpuCount() {
        int cpu = underTest.getCpuCountForInstanceType(REGION, INSTANCE_TYPE);

        Assertions.assertEquals(69, cpu);
    }

    @Test
    void getMemory() {
        int memory = underTest.getMemoryForInstanceType(REGION, INSTANCE_TYPE);

        Assertions.assertEquals(420, memory);
    }

    @Test
    void getUsdPriceAlreadyInCache() {
        double price1 = underTest.getPriceForInstanceType(REGION, INSTANCE_TYPE);
        double price2 = underTest.getPriceForInstanceType(REGION, INSTANCE_TYPE);

        Assertions.assertEquals(0.69, price1);
        Assertions.assertEquals(0.69, price2);
    }

    private GetProductsResult getGetProductsResult() {
        Attributes attributes = new Attributes();
        attributes.setVcpu(69);
        attributes.setMemory("420");
        Product product = new Product(null, attributes, null);
        PricePerUnit pricePerUnit = new PricePerUnit(0.69);
        PriceDimension priceDimension = new PriceDimension(null, null, null, null, null, null, pricePerUnit);
        OfferTerm offerTerm = new OfferTerm(Map.of("test", priceDimension), null, null, null, null);
        Terms terms = new Terms(Map.of("onDemand", offerTerm), null);
        PriceListElement priceListElement = new PriceListElement(product, null, terms, null, null);
        GetProductsResult getProductsResult = new GetProductsResult();
        try {
            getProductsResult.setPriceList(List.of(JsonUtil.writeValueAsString(priceListElement)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return getProductsResult;
    }
}
