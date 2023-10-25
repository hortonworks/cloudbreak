// CHECKSTYLE:OFF
/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 *  !!!!!!!!!!!!!!!!!
 *  !!!!COPIED FROM AWS SDK to extend Stack waiter with UPDATE_COMPLETE and UPDATE_FAILED statuses
 *  !!!!!!!!!!!!!!!!!
 *  !!!!IMPORTANT!!!! Leave this override here until aws fixes https://github.com/aws/aws-sdk-java-v2/issues/4629.
 *  !!!!!!!!!!!!!!!!!
 *
 */

package software.amazon.awssdk.services.cloudformation.waiters;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.internal.waiters.WaiterAttribute;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeChangeSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeChangeSetResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.waiters.internal.WaitersRuntime;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
@ThreadSafe
public final class DefaultCloudFormationWaiter implements CloudFormationWaiter {
    private static final WaiterAttribute<SdkAutoCloseable> CLIENT_ATTRIBUTE = new WaiterAttribute<>(SdkAutoCloseable.class);

    private final CloudFormationClient client;

    private final AttributeMap managedResources;

    private final Waiter<DescribeStacksResponse> stackExistsWaiter;

    private final Waiter<DescribeStacksResponse> stackCreateCompleteWaiter;

    private final Waiter<DescribeStacksResponse> stackDeleteCompleteWaiter;

    private final Waiter<DescribeStacksResponse> stackUpdateCompleteWaiter;

    private final Waiter<DescribeStacksResponse> stackImportCompleteWaiter;

    private final Waiter<DescribeStacksResponse> stackRollbackCompleteWaiter;

    private final Waiter<DescribeChangeSetResponse> changeSetCreateCompleteWaiter;

    private final Waiter<DescribeTypeRegistrationResponse> typeRegistrationCompleteWaiter;

    private DefaultCloudFormationWaiter(DefaultBuilder builder) {
        AttributeMap.Builder attributeMapBuilder = AttributeMap.builder();
        if (builder.client == null) {
            this.client = CloudFormationClient.builder().build();
            attributeMapBuilder.put(CLIENT_ATTRIBUTE, this.client);
        } else {
            this.client = builder.client;
        }
        managedResources = attributeMapBuilder.build();
        this.stackExistsWaiter = Waiter.builder(DescribeStacksResponse.class).acceptors(stackExistsWaiterAcceptors())
                .overrideConfiguration(stackExistsWaiterConfig(builder.overrideConfiguration)).build();
        this.stackCreateCompleteWaiter = Waiter.builder(DescribeStacksResponse.class)
                .acceptors(stackCreateCompleteWaiterAcceptors())
                .overrideConfiguration(stackCreateCompleteWaiterConfig(builder.overrideConfiguration)).build();
        this.stackDeleteCompleteWaiter = Waiter.builder(DescribeStacksResponse.class)
                .acceptors(stackDeleteCompleteWaiterAcceptors())
                .overrideConfiguration(stackDeleteCompleteWaiterConfig(builder.overrideConfiguration)).build();
        this.stackUpdateCompleteWaiter = Waiter.builder(DescribeStacksResponse.class)
                .acceptors(stackUpdateCompleteWaiterAcceptors())
                .overrideConfiguration(stackUpdateCompleteWaiterConfig(builder.overrideConfiguration)).build();
        this.stackImportCompleteWaiter = Waiter.builder(DescribeStacksResponse.class)
                .acceptors(stackImportCompleteWaiterAcceptors())
                .overrideConfiguration(stackImportCompleteWaiterConfig(builder.overrideConfiguration)).build();
        this.stackRollbackCompleteWaiter = Waiter.builder(DescribeStacksResponse.class)
                .acceptors(stackRollbackCompleteWaiterAcceptors())
                .overrideConfiguration(stackRollbackCompleteWaiterConfig(builder.overrideConfiguration)).build();
        this.changeSetCreateCompleteWaiter = Waiter.builder(DescribeChangeSetResponse.class)
                .acceptors(changeSetCreateCompleteWaiterAcceptors())
                .overrideConfiguration(changeSetCreateCompleteWaiterConfig(builder.overrideConfiguration)).build();
        this.typeRegistrationCompleteWaiter = Waiter.builder(DescribeTypeRegistrationResponse.class)
                .acceptors(typeRegistrationCompleteWaiterAcceptors())
                .overrideConfiguration(typeRegistrationCompleteWaiterConfig(builder.overrideConfiguration)).build();
    }

    private static String errorCode(Throwable error) {
        if (error instanceof AwsServiceException) {
            return ((AwsServiceException) error).awsErrorDetails().errorCode();
        }
        return null;
    }

    @Override
    public WaiterResponse<DescribeChangeSetResponse> waitUntilChangeSetCreateComplete(
            DescribeChangeSetRequest describeChangeSetRequest) {
        return changeSetCreateCompleteWaiter.run(() -> client.describeChangeSet(applyWaitersUserAgent(describeChangeSetRequest)));
    }

    @Override
    public WaiterResponse<DescribeChangeSetResponse> waitUntilChangeSetCreateComplete(
            DescribeChangeSetRequest describeChangeSetRequest, WaiterOverrideConfiguration overrideConfig) {
        return changeSetCreateCompleteWaiter.run(() -> client.describeChangeSet(applyWaitersUserAgent(describeChangeSetRequest)),
                changeSetCreateCompleteWaiterConfig(overrideConfig));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackCreateComplete(DescribeStacksRequest describeStacksRequest) {
        return stackCreateCompleteWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackCreateComplete(DescribeStacksRequest describeStacksRequest,
            WaiterOverrideConfiguration overrideConfig) {
        return stackCreateCompleteWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)),
                stackCreateCompleteWaiterConfig(overrideConfig));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackDeleteComplete(DescribeStacksRequest describeStacksRequest) {
        return stackDeleteCompleteWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackDeleteComplete(DescribeStacksRequest describeStacksRequest,
            WaiterOverrideConfiguration overrideConfig) {
        return stackDeleteCompleteWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)),
                stackDeleteCompleteWaiterConfig(overrideConfig));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackExists(DescribeStacksRequest describeStacksRequest) {
        return stackExistsWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackExists(DescribeStacksRequest describeStacksRequest,
            WaiterOverrideConfiguration overrideConfig) {
        return stackExistsWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)),
                stackExistsWaiterConfig(overrideConfig));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackImportComplete(DescribeStacksRequest describeStacksRequest) {
        return stackImportCompleteWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackImportComplete(DescribeStacksRequest describeStacksRequest,
            WaiterOverrideConfiguration overrideConfig) {
        return stackImportCompleteWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)),
                stackImportCompleteWaiterConfig(overrideConfig));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackRollbackComplete(DescribeStacksRequest describeStacksRequest) {
        return stackRollbackCompleteWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackRollbackComplete(DescribeStacksRequest describeStacksRequest,
            WaiterOverrideConfiguration overrideConfig) {
        return stackRollbackCompleteWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)),
                stackRollbackCompleteWaiterConfig(overrideConfig));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackUpdateComplete(DescribeStacksRequest describeStacksRequest) {
        return stackUpdateCompleteWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)));
    }

    @Override
    public WaiterResponse<DescribeStacksResponse> waitUntilStackUpdateComplete(DescribeStacksRequest describeStacksRequest,
            WaiterOverrideConfiguration overrideConfig) {
        return stackUpdateCompleteWaiter.run(() -> client.describeStacks(applyWaitersUserAgent(describeStacksRequest)),
                stackUpdateCompleteWaiterConfig(overrideConfig));
    }

    @Override
    public WaiterResponse<DescribeTypeRegistrationResponse> waitUntilTypeRegistrationComplete(
            DescribeTypeRegistrationRequest describeTypeRegistrationRequest) {
        return typeRegistrationCompleteWaiter.run(() -> client
                .describeTypeRegistration(applyWaitersUserAgent(describeTypeRegistrationRequest)));
    }

    @Override
    public WaiterResponse<DescribeTypeRegistrationResponse> waitUntilTypeRegistrationComplete(
            DescribeTypeRegistrationRequest describeTypeRegistrationRequest, WaiterOverrideConfiguration overrideConfig) {
        return typeRegistrationCompleteWaiter.run(
                () -> client.describeTypeRegistration(applyWaitersUserAgent(describeTypeRegistrationRequest)),
                typeRegistrationCompleteWaiterConfig(overrideConfig));
    }

    private static List<WaiterAcceptor<? super DescribeStacksResponse>> stackExistsWaiterAcceptors() {
        List<WaiterAcceptor<? super DescribeStacksResponse>> result = new ArrayList<>();
        result.add(new WaitersRuntime.ResponseStatusAcceptor(200, WaiterState.SUCCESS));
        result.add(WaiterAcceptor.retryOnExceptionAcceptor(error -> Objects.equals(errorCode(error), "ValidationError")));
        result.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return result;
    }

    private static List<WaiterAcceptor<? super DescribeStacksResponse>> stackCreateCompleteWaiterAcceptors() {
        List<WaiterAcceptor<? super DescribeStacksResponse>> result = new ArrayList<>();
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().allMatch(v -> Objects.equals(v, "CREATE_COMPLETE"));
        }));
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "UPDATE_COMPLETE"));
        }));
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "UPDATE_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "CREATE_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "DELETE_COMPLETE"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "DELETE_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "ROLLBACK_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "ROLLBACK_COMPLETE"));
        }));
        result.add(WaiterAcceptor.errorOnExceptionAcceptor(error -> Objects.equals(errorCode(error), "ValidationError")));
        result.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return result;
    }

    private static List<WaiterAcceptor<? super DescribeStacksResponse>> stackDeleteCompleteWaiterAcceptors() {
        List<WaiterAcceptor<? super DescribeStacksResponse>> result = new ArrayList<>();
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().allMatch(v -> Objects.equals(v, "DELETE_COMPLETE"));
        }));
        result.add(WaiterAcceptor.successOnExceptionAcceptor(error -> Objects.equals(errorCode(error), "ValidationError")));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "DELETE_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "CREATE_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "ROLLBACK_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty()
                    && resultValues.stream().anyMatch(v -> Objects.equals(v, "UPDATE_ROLLBACK_IN_PROGRESS"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "UPDATE_ROLLBACK_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "UPDATE_ROLLBACK_COMPLETE"));
        }));
        result.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return result;
    }

    private static List<WaiterAcceptor<? super DescribeStacksResponse>> stackUpdateCompleteWaiterAcceptors() {
        List<WaiterAcceptor<? super DescribeStacksResponse>> result = new ArrayList<>();
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().allMatch(v -> Objects.equals(v, "UPDATE_COMPLETE"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "UPDATE_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "UPDATE_ROLLBACK_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "UPDATE_ROLLBACK_COMPLETE"));
        }));
        result.add(WaiterAcceptor.errorOnExceptionAcceptor(error -> Objects.equals(errorCode(error), "ValidationError")));
        result.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return result;
    }

    private static List<WaiterAcceptor<? super DescribeStacksResponse>> stackImportCompleteWaiterAcceptors() {
        List<WaiterAcceptor<? super DescribeStacksResponse>> result = new ArrayList<>();
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().allMatch(v -> Objects.equals(v, "IMPORT_COMPLETE"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "ROLLBACK_COMPLETE"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "ROLLBACK_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty()
                    && resultValues.stream().anyMatch(v -> Objects.equals(v, "IMPORT_ROLLBACK_IN_PROGRESS"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "IMPORT_ROLLBACK_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "IMPORT_ROLLBACK_COMPLETE"));
        }));
        result.add(WaiterAcceptor.errorOnExceptionAcceptor(error -> Objects.equals(errorCode(error), "ValidationError")));
        result.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return result;
    }

    private static List<WaiterAcceptor<? super DescribeStacksResponse>> stackRollbackCompleteWaiterAcceptors() {
        List<WaiterAcceptor<? super DescribeStacksResponse>> result = new ArrayList<>();
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().allMatch(v -> Objects.equals(v, "UPDATE_ROLLBACK_COMPLETE"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "UPDATE_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "UPDATE_ROLLBACK_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("Stacks").flatten().field("StackStatus").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "DELETE_FAILED"));
        }));
        result.add(WaiterAcceptor.errorOnExceptionAcceptor(error -> Objects.equals(errorCode(error), "ValidationError")));
        result.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return result;
    }

    private static List<WaiterAcceptor<? super DescribeChangeSetResponse>> changeSetCreateCompleteWaiterAcceptors() {
        List<WaiterAcceptor<? super DescribeChangeSetResponse>> result = new ArrayList<>();
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            return Objects.equals(input.field("Status").value(), "CREATE_COMPLETE");
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            return Objects.equals(input.field("Status").value(), "FAILED");
        }));
        result.add(WaiterAcceptor.errorOnExceptionAcceptor(error -> Objects.equals(errorCode(error), "ValidationError")));
        result.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return result;
    }

    private static List<WaiterAcceptor<? super DescribeTypeRegistrationResponse>> typeRegistrationCompleteWaiterAcceptors() {
        List<WaiterAcceptor<? super DescribeTypeRegistrationResponse>> result = new ArrayList<>();
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            return Objects.equals(input.field("ProgressStatus").value(), "COMPLETE");
        }));
        result.add(WaiterAcceptor.errorOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            return Objects.equals(input.field("ProgressStatus").value(), "FAILED");
        }));
        result.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return result;
    }

    private static WaiterOverrideConfiguration stackExistsWaiterConfig(WaiterOverrideConfiguration overrideConfig) {
        Optional<WaiterOverrideConfiguration> optionalOverrideConfig = Optional.ofNullable(overrideConfig);
        int maxAttempts = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::maxAttempts).orElse(20);
        BackoffStrategy backoffStrategy = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::backoffStrategy).orElse(
                FixedDelayBackoffStrategy.create(Duration.ofSeconds(5)));
        Duration waitTimeout = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::waitTimeout).orElse(null);
        return WaiterOverrideConfiguration.builder().maxAttempts(maxAttempts).backoffStrategy(backoffStrategy)
                .waitTimeout(waitTimeout).build();
    }

    private static WaiterOverrideConfiguration stackCreateCompleteWaiterConfig(WaiterOverrideConfiguration overrideConfig) {
        Optional<WaiterOverrideConfiguration> optionalOverrideConfig = Optional.ofNullable(overrideConfig);
        int maxAttempts = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::maxAttempts).orElse(120);
        BackoffStrategy backoffStrategy = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::backoffStrategy).orElse(
                FixedDelayBackoffStrategy.create(Duration.ofSeconds(30)));
        Duration waitTimeout = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::waitTimeout).orElse(null);
        return WaiterOverrideConfiguration.builder().maxAttempts(maxAttempts).backoffStrategy(backoffStrategy)
                .waitTimeout(waitTimeout).build();
    }

    private static WaiterOverrideConfiguration stackDeleteCompleteWaiterConfig(WaiterOverrideConfiguration overrideConfig) {
        Optional<WaiterOverrideConfiguration> optionalOverrideConfig = Optional.ofNullable(overrideConfig);
        int maxAttempts = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::maxAttempts).orElse(120);
        BackoffStrategy backoffStrategy = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::backoffStrategy).orElse(
                FixedDelayBackoffStrategy.create(Duration.ofSeconds(30)));
        Duration waitTimeout = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::waitTimeout).orElse(null);
        return WaiterOverrideConfiguration.builder().maxAttempts(maxAttempts).backoffStrategy(backoffStrategy)
                .waitTimeout(waitTimeout).build();
    }

    private static WaiterOverrideConfiguration stackUpdateCompleteWaiterConfig(WaiterOverrideConfiguration overrideConfig) {
        Optional<WaiterOverrideConfiguration> optionalOverrideConfig = Optional.ofNullable(overrideConfig);
        int maxAttempts = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::maxAttempts).orElse(120);
        BackoffStrategy backoffStrategy = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::backoffStrategy).orElse(
                FixedDelayBackoffStrategy.create(Duration.ofSeconds(30)));
        Duration waitTimeout = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::waitTimeout).orElse(null);
        return WaiterOverrideConfiguration.builder().maxAttempts(maxAttempts).backoffStrategy(backoffStrategy)
                .waitTimeout(waitTimeout).build();
    }

    private static WaiterOverrideConfiguration stackImportCompleteWaiterConfig(WaiterOverrideConfiguration overrideConfig) {
        Optional<WaiterOverrideConfiguration> optionalOverrideConfig = Optional.ofNullable(overrideConfig);
        int maxAttempts = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::maxAttempts).orElse(120);
        BackoffStrategy backoffStrategy = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::backoffStrategy).orElse(
                FixedDelayBackoffStrategy.create(Duration.ofSeconds(30)));
        Duration waitTimeout = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::waitTimeout).orElse(null);
        return WaiterOverrideConfiguration.builder().maxAttempts(maxAttempts).backoffStrategy(backoffStrategy)
                .waitTimeout(waitTimeout).build();
    }

    private static WaiterOverrideConfiguration stackRollbackCompleteWaiterConfig(WaiterOverrideConfiguration overrideConfig) {
        Optional<WaiterOverrideConfiguration> optionalOverrideConfig = Optional.ofNullable(overrideConfig);
        int maxAttempts = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::maxAttempts).orElse(120);
        BackoffStrategy backoffStrategy = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::backoffStrategy).orElse(
                FixedDelayBackoffStrategy.create(Duration.ofSeconds(30)));
        Duration waitTimeout = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::waitTimeout).orElse(null);
        return WaiterOverrideConfiguration.builder().maxAttempts(maxAttempts).backoffStrategy(backoffStrategy)
                .waitTimeout(waitTimeout).build();
    }

    private static WaiterOverrideConfiguration changeSetCreateCompleteWaiterConfig(WaiterOverrideConfiguration overrideConfig) {
        Optional<WaiterOverrideConfiguration> optionalOverrideConfig = Optional.ofNullable(overrideConfig);
        int maxAttempts = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::maxAttempts).orElse(120);
        BackoffStrategy backoffStrategy = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::backoffStrategy).orElse(
                FixedDelayBackoffStrategy.create(Duration.ofSeconds(30)));
        Duration waitTimeout = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::waitTimeout).orElse(null);
        return WaiterOverrideConfiguration.builder().maxAttempts(maxAttempts).backoffStrategy(backoffStrategy)
                .waitTimeout(waitTimeout).build();
    }

    private static WaiterOverrideConfiguration typeRegistrationCompleteWaiterConfig(WaiterOverrideConfiguration overrideConfig) {
        Optional<WaiterOverrideConfiguration> optionalOverrideConfig = Optional.ofNullable(overrideConfig);
        int maxAttempts = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::maxAttempts).orElse(120);
        BackoffStrategy backoffStrategy = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::backoffStrategy).orElse(
                FixedDelayBackoffStrategy.create(Duration.ofSeconds(30)));
        Duration waitTimeout = optionalOverrideConfig.flatMap(WaiterOverrideConfiguration::waitTimeout).orElse(null);
        return WaiterOverrideConfiguration.builder().maxAttempts(maxAttempts).backoffStrategy(backoffStrategy)
                .waitTimeout(waitTimeout).build();
    }

    @Override
    public void close() {
        managedResources.close();
    }

    public static CloudFormationWaiter.Builder builder() {
        return new DefaultBuilder();
    }

    private <T extends CloudFormationRequest> T applyWaitersUserAgent(T request) {
        Consumer<AwsRequestOverrideConfiguration.Builder> userAgentApplier = b -> b.addApiName(ApiName.builder()
                .version("waiter").name("hll").build());
        AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration()
                .map(c -> c.toBuilder().applyMutation(userAgentApplier).build())
                .orElse((AwsRequestOverrideConfiguration.builder().applyMutation(userAgentApplier).build()));
        return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
    }

    public static final class DefaultBuilder implements CloudFormationWaiter.Builder {
        private CloudFormationClient client;

        private WaiterOverrideConfiguration overrideConfiguration;

        private DefaultBuilder() {
        }

        @Override
        public CloudFormationWaiter.Builder overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public CloudFormationWaiter.Builder client(CloudFormationClient client) {
            this.client = client;
            return this;
        }

        public CloudFormationWaiter build() {
            return new DefaultCloudFormationWaiter(this);
        }
    }
}
// CHECKSTYLE:ON