package com.sequenceiq.it.cloudbreak.newway.assertion.storage.azure;

import java.util.Objects;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.storage.location.StorageLocationV4Response;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

public class AdlsGen2TestAssertion {

    private AdlsGen2TestAssertion() {
    }

    public static AssertionV2<StackTestDto> stackContainsAdlsGen2Properties() {
        return (testContext, testDto, cloudbreakClient) -> {
            cloudStorageParametersExists(testDto);
            accountKeyIsTheExpectedOnRequest(testDto);
            accountNameIsTheExpectedOnRequest(testDto);
            return testDto;
        };
    }

    public static AssertionV2<StackTestDto> stackContainsStorageLocations() {
        return (testContext, testDto, cloudbreakClient) -> {
            storageLocationExists(testDto);
            storageLocationsSizeMatches(testDto);
            requestAndResponseValuesAreTheSame(testDto);
            return testDto;
        };
    }

    private static void requestAndResponseValuesAreTheSame(StackTestDto dto) {
        Set<StorageLocationV4Request> requests = dto.getRequest().getCluster().getCloudStorage().getLocations();
        Set<StorageLocationV4Response> responses = dto.getResponse().getCluster().getCloudStorage().getLocations();
        if (!responses.stream().allMatch(response -> requests.stream().anyMatch(request -> locationRequestEqualsResponse(request, response)))) {
            throw new IllegalArgumentException("Not all the location response are the same as the given request!");
        }
    }

    private static boolean locationRequestEqualsResponse(StorageLocationV4Request request, StorageLocationV4Response response) {
        return Objects.equals(response.getPropertyFile(), request.getPropertyFile()) && Objects.equals(response.getPropertyName(), request.getPropertyName())
                && Objects.equals(response.getValue(), request.getValue());
    }

    private static void cloudStorageParametersExists(StackTestDto dto) {
        if (dto.getResponse().getCluster().getCloudStorage().getAdlsGen2() == null) {
            throw new IllegalArgumentException("AdlsGen2 parameters should not be null in response!");
        }
    }

    private static void accountKeyIsTheExpectedOnRequest(StackTestDto dto) {
        String actual = dto.getResponse().getCluster().getCloudStorage().getAdlsGen2().getAccountKey();
        String expected = dto.getRequest().getCluster().getCloudStorage().getAdlsGen2().getAccountKey();
        if (!Objects.equals(actual, expected)) {
            throw new IllegalArgumentException(String.format("The response does not contains the expected [%s] account key! currently: %s", expected, actual));
        }
    }

    private static void accountNameIsTheExpectedOnRequest(StackTestDto dto) {
        String actual = dto.getResponse().getCluster().getCloudStorage().getAdlsGen2().getAccountName();
        String expected = dto.getRequest().getCluster().getCloudStorage().getAdlsGen2().getAccountName();
        if (!Objects.equals(actual, expected)) {
            throw new IllegalArgumentException(String.format("The response does not contains the expected [%s] account name! currently: %s", expected, actual));
        }
    }

    private static void storageLocationExists(StackTestDto dto) {
        Set<StorageLocationV4Response> locationResponses = dto.getResponse().getCluster().getCloudStorage().getLocations();
        if (locationResponses == null || locationResponses.isEmpty()) {
            throw new IllegalArgumentException("Cloud storage should contain storage locations, but now it doesn't!");
        }
    }

    private static void storageLocationsSizeMatches(StackTestDto dto) {
        Set<StorageLocationV4Response> locationResponses = dto.getResponse().getCluster().getCloudStorage().getLocations();
        Set<StorageLocationV4Request> storageLocationRequests = dto.getRequest().getCluster().getCloudStorage().getLocations();
        if (locationResponses.size() != storageLocationRequests.size()) {
            throw new IllegalArgumentException("The number of storage locations does not match with the expected amount!");
        }
    }

}
