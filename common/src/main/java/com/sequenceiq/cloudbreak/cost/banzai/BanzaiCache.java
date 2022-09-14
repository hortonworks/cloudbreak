package com.sequenceiq.cloudbreak.cost.banzai;

import static com.sequenceiq.cloudbreak.logger.MDCContextFilter.REQUEST_ID_HEADER;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Service;

@Service
public class BanzaiCache {

    @Inject
    private BanzaiRetryableWebTarget retryableWebTarget;

    @Inject
    private BanzaiResponseReader responseReader;

    private Map<String, Map<String, BanzaiProductResponse>> cache = new HashMap<>();

    private static final Set<String> REGIONS = Set.of(
            "us-east-1",
            "us-east-2",
            "us-west-1",
            "us-west-2",
            "ca-central-1",
            "eu-north-1",
            "eu-west-1",
            "eu-west-2",
            "eu-west-3",
            "eu-central-1",
            "eu-south-1");

    @PostConstruct
    public void init() {
        for (String region : REGIONS) {
            cache.put(region, getPrice(region));
        }
    }

    private Map<String, BanzaiProductResponse> getPrice(String region) {
        Client client = ClientBuilder.newClient();

        WebTarget webTarget = client.target(String.format("https://alpha.dev.banzaicloud.com/cloudinfo/api/v1/providers/amazon/services/compute/regions/%s/products", region));
        Invocation.Builder call = createInvocationBuilder(webTarget);
        try (Response result = executeCall(webTarget.getUri(), () -> retryableWebTarget.get(call))) {
            return responseReader.read(webTarget.getUri().toString(), result, BanzaiResponse.class)
                    .orElseThrow(() -> new BanzaiOperationFailedException(String.format("Exception occured when we tried to setup the cache on region %s", region)))
                    .getProducts()
                    .stream()
                    .collect(Collectors.toMap(BanzaiProductResponse::getType, e -> e));
        } catch (RuntimeException e) {
            throw new BanzaiOperationFailedException(
                    String.format("Exception occured when we tried to setup the cache on region %s", region), e);
        }
    }

    private Response executeCall(URI path, Callable<Response> toCall) {
        try {
            return toCall.call();
        } catch (Exception re) {
            throw new BanzaiOperationFailedException(re);
        }
    }

    private Invocation.Builder createInvocationBuilder(WebTarget webTarget) {
        return webTarget
                .request()
                .accept(APPLICATION_JSON)
                .header(REQUEST_ID_HEADER, UUID.randomUUID().toString());
    }

    public Double priceByInstanceType(String region, String machineType) {
        Map<String, BanzaiProductResponse> stringBanzaiProductResponseMap = cache.get(region);
        if (stringBanzaiProductResponseMap != null) {
            BanzaiProductResponse banzaiProductResponse = stringBanzaiProductResponseMap.get(machineType);
            if (banzaiProductResponse != null) {
                return banzaiProductResponse.getOnDemandPrice();
            }
        }
        return 1.0;
    }

    public int cpuByInstanceType(String region, String machineType) {
        Map<String, BanzaiProductResponse> stringBanzaiProductResponseMap = cache.get(region);
        if (stringBanzaiProductResponseMap != null) {
            BanzaiProductResponse banzaiProductResponse = stringBanzaiProductResponseMap.get(machineType);
            if (banzaiProductResponse != null) {
                return banzaiProductResponse.getCpusPerVm();
            }
        }
        return 2;
    }

    public int memoryByInstanceType(String region, String machineType) {
        Map<String, BanzaiProductResponse> stringBanzaiProductResponseMap = cache.get(region);
        if (stringBanzaiProductResponseMap != null) {
            BanzaiProductResponse banzaiProductResponse = stringBanzaiProductResponseMap.get(machineType);
            if (banzaiProductResponse != null) {
                return banzaiProductResponse.getMemPerVm();
            }
        }
        return 4;
    }

}
