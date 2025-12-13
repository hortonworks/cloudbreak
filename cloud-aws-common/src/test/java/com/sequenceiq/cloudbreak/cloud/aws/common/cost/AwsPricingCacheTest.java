package com.sequenceiq.cloudbreak.cloud.aws.common.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsCredentialVerifier;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonPricingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.Attributes;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.OfferTerm;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.PriceDimension;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.PriceListElement;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.PricePerUnit;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.Product;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.model.Terms;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

@ExtendWith(MockitoExtension.class)
public class AwsPricingCacheTest {

    private static final String REGION = "region";

    private static final String INSTANCE_TYPE = "instanceType";

    private static final double AWS_DEFAULT_STORAGE_PRICE = 0.00007638888889;

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonPricingClient awsPricingClient;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private AwsCredentialVerifier awsCredentialVerifier;

    @InjectMocks
    private AwsPricingCache underTest;

    @BeforeEach
    void setup() {
        lenient().when(awsClient.createPricingClient(any(), any())).thenReturn(awsPricingClient);
        lenient().when(awsPricingClient.getProducts(any(GetProductsRequest.class))).thenReturn(getGetProductsResult());
    }

    @Test
    void getUsdPriceWhenNoPermission() throws AwsPermissionMissingException {
        doThrow(new AwsPermissionMissingException()).when(awsCredentialVerifier).validateAws(any(), anyString());

        Optional<Double> price = underTest.getPriceForInstanceType(REGION, INSTANCE_TYPE,
                new ExtendedCloudCredential(new CloudCredential(), "", "", "", List.of()));

        assertTrue(price.isEmpty());
    }

    @Test
    void getUsdPrice() {
        Optional<Double> price = underTest.getPriceForInstanceType(REGION, INSTANCE_TYPE,
                new ExtendedCloudCredential(new CloudCredential(), "", "", "", List.of()));

        assertTrue(price.isPresent());
        assertEquals(0.69, price.get());
    }

    @Test
    void getCpuCount() {
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(getCloudVmTypes());

        Optional<Integer> cpu = underTest.getCpuCountForInstanceType(REGION, INSTANCE_TYPE,
                new ExtendedCloudCredential(new CloudCredential(), "", "", "", List.of()));

        assertTrue(cpu.isPresent());
        assertEquals(69, cpu.get());
    }

    @Test
    void getMemory() {
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), any(), any())).thenReturn(getCloudVmTypes());

        Optional<Integer> memory = underTest.getMemoryForInstanceType(REGION, INSTANCE_TYPE,
                new ExtendedCloudCredential(new CloudCredential(), "", "", "", List.of()));

        assertTrue(memory.isPresent());
        assertEquals(420, memory.get());
    }

    @Test
    void getUsdPriceAlreadyInCache() {
        Optional<Double> price1 = underTest.getPriceForInstanceType(REGION, INSTANCE_TYPE,
                new ExtendedCloudCredential(new CloudCredential(), "", "", "", List.of()));
        Optional<Double> price2 = underTest.getPriceForInstanceType(REGION, INSTANCE_TYPE,
                new ExtendedCloudCredential(new CloudCredential(), "", "", "", List.of()));

        assertTrue(price1.isPresent());
        assertTrue(price2.isPresent());
        assertEquals(0.69, price1.get());
        assertEquals(0.69, price2.get());
    }

    @Test
    void getStoragePriceWithZeroVolumeSize() {
        Optional<Double> price = underTest.getStoragePricePerGBHour(REGION, "gp2", 0);

        assertTrue(price.isEmpty());
    }

    @Test
    void getStoragePriceWithNullStorageType() {
        Optional<Double> price = underTest.getStoragePricePerGBHour(REGION, "unknown", 100);

        assertTrue(price.isPresent());
        assertEquals(AWS_DEFAULT_STORAGE_PRICE, price.get(), 0.0001);
    }

    @Test
    void getStoragePriceWithUnknownStorageType() {
        Optional<Double> price = underTest.getStoragePricePerGBHour(REGION, "unknown", 100);

        assertTrue(price.isPresent());
        assertEquals(AWS_DEFAULT_STORAGE_PRICE, price.get(), 0.0001);
    }

    @Test
    void getStoragePrice() {
        Optional<Double> price = underTest.getStoragePricePerGBHour(REGION, "gp2", 1000);

        assertTrue(price.isPresent());
        assertNotEquals(0.0, price.get());
    }

    private CloudVmTypes getCloudVmTypes() {
        VmTypeMeta vmTypeMeta = new VmTypeMeta();
        vmTypeMeta.setProperties(Map.of("Cpu", 69, "Memory", 420));
        VmType vmType = VmType.vmTypeWithMeta(INSTANCE_TYPE, vmTypeMeta, false);
        return new CloudVmTypes(Map.of(REGION, Set.of(vmType)), null);
    }

    private GetProductsResponse getGetProductsResult() {
        Attributes attributes = new Attributes();
        attributes.setVcpu(69);
        attributes.setMemory("420");
        Product product = new Product(null, attributes, null);
        PricePerUnit pricePerUnit = new PricePerUnit(0.69);
        PriceDimension priceDimension = new PriceDimension(null, null, null, null, null, null, pricePerUnit);
        OfferTerm offerTerm = new OfferTerm(Map.of("test", priceDimension), null, null, null, null);
        Terms terms = new Terms(Map.of("onDemand", offerTerm), null);
        PriceListElement priceListElement = new PriceListElement(product, null, terms, null, null);
        GetProductsResponse.Builder getProductsResponseBuilder = GetProductsResponse.builder();
        try {
            getProductsResponseBuilder.priceList(List.of(JsonUtil.writeValueAsString(priceListElement)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return getProductsResponseBuilder.build();
    }
}
