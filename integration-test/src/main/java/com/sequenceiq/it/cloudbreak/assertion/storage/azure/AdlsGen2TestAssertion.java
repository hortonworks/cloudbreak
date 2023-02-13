package com.sequenceiq.it.cloudbreak.assertion.storage.azure;

import java.util.List;
import java.util.Objects;

import org.springframework.util.CollectionUtils;

import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class AdlsGen2TestAssertion {

    private AdlsGen2TestAssertion() {
    }

    public static Assertion<StackTestDto, CloudbreakClient> stackContainsAdlsGen2Properties() {
        return (testContext, testDto, cloudbreakClient) -> {
            cloudStorageParametersExists(testDto);
            accountKeyIsTheExpectedOnRequest(testDto);
            accountNameIsTheExpectedOnRequest(testDto);
            return testDto;
        };
    }

    public static Assertion<StackTestDto, CloudbreakClient> stackContainsStorageLocations() {
        return (testContext, testDto, cloudbreakClient) -> {
            storageLocationExists(testDto);
            storageLocationsSizeMatches(testDto);
            requestAndResponseValuesAreTheSame(testDto);
            return testDto;
        };
    }

    private static void requestAndResponseValuesAreTheSame(StackTestDto dto) {
        List<StorageLocationBase> requests = dto.getRequest().getCluster().getCloudStorage().getLocations();
        List<StorageLocationBase> responses = dto.getResponse().getCluster().getCloudStorage().getLocations();
        if (!responses.stream().allMatch(response -> requests.stream().anyMatch(request -> locationRequestEqualsResponse(request, response)))) {
            throw new IllegalArgumentException("Not all the location response are the same as the given request!");
        }
    }

    private static boolean locationRequestEqualsResponse(StorageLocationBase request, StorageLocationBase response) {
        return Objects.equals(response.getType(), request.getType()) && Objects.equals(response.getValue(), request.getValue());
    }

    private static void cloudStorageParametersExists(StackTestDto dto) {
        if (CollectionUtils.isEmpty(dto.getResponse().getCluster().getCloudStorage().getIdentities())) {
            throw new IllegalArgumentException("Identities should not be null in response!");
        }
        if (dto.getResponse().getCluster().getCloudStorage().getIdentities().get(0).getAdlsGen2() == null) {
            throw new IllegalArgumentException("AdlsGen2 parameters should not be null in response!");
        }
    }

    private static void accountKeyIsTheExpectedOnRequest(StackTestDto dto) {
        if (CollectionUtils.isEmpty(dto.getResponse().getCluster().getCloudStorage().getIdentities())) {
            throw new IllegalArgumentException("Identities should not be null in response!");
        }
        String actual = dto.getResponse().getCluster().getCloudStorage().getIdentities().get(0).getAdlsGen2().getAccountKey();
        String expected = dto.getRequest().getCluster().getCloudStorage().getIdentities().get(0).getAdlsGen2().getAccountKey();
        if (!Objects.equals(actual, expected)) {
            throw new IllegalArgumentException(String.format("The response does not contains the expected [%s] account key! currently: %s", expected, actual));
        }
    }

    private static void accountNameIsTheExpectedOnRequest(StackTestDto dto) {
        if (CollectionUtils.isEmpty(dto.getResponse().getCluster().getCloudStorage().getIdentities())) {
            throw new IllegalArgumentException("Identities should not be null in response!");
        }
        String actual = dto.getResponse().getCluster().getCloudStorage().getIdentities().get(0).getAdlsGen2().getAccountName();
        String expected = dto.getRequest().getCluster().getCloudStorage().getIdentities().get(0).getAdlsGen2().getAccountName();
        if (!Objects.equals(actual, expected)) {
            throw new IllegalArgumentException(String.format("The response does not contains the expected [%s] account name! currently: %s", expected, actual));
        }
    }

    private static void storageLocationExists(StackTestDto dto) {
        List<StorageLocationBase> locationResponses = dto.getResponse().getCluster().getCloudStorage().getLocations();
        if (locationResponses == null || locationResponses.isEmpty()) {
            throw new IllegalArgumentException("Cloud storage should contain storage locations, but now it doesn't!");
        }
    }

    private static void storageLocationsSizeMatches(StackTestDto dto) {
        List<StorageLocationBase> locationResponses = dto.getResponse().getCluster().getCloudStorage().getLocations();
        List<StorageLocationBase> storageLocationRequests = dto.getRequest().getCluster().getCloudStorage().getLocations();
        if (locationResponses.size() != storageLocationRequests.size()) {
            throw new IllegalArgumentException("The number of storage locations does not match with the expected amount!");
        }
    }

}
