package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import com.sequenceiq.cloudbreak.service.retry.Retry;

import software.amazon.awssdk.services.efs.EfsClient;
import software.amazon.awssdk.services.efs.model.CreateFileSystemRequest;
import software.amazon.awssdk.services.efs.model.CreateFileSystemResponse;
import software.amazon.awssdk.services.efs.model.DeleteFileSystemRequest;
import software.amazon.awssdk.services.efs.model.DeleteFileSystemResponse;
import software.amazon.awssdk.services.efs.model.DeleteMountTargetRequest;
import software.amazon.awssdk.services.efs.model.DeleteMountTargetResponse;
import software.amazon.awssdk.services.efs.model.DescribeFileSystemsRequest;
import software.amazon.awssdk.services.efs.model.DescribeFileSystemsResponse;
import software.amazon.awssdk.services.efs.model.DescribeMountTargetsRequest;
import software.amazon.awssdk.services.efs.model.DescribeMountTargetsResponse;
import software.amazon.awssdk.services.efs.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.efs.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.efs.model.TagResourceRequest;
import software.amazon.awssdk.services.efs.model.TagResourceResponse;

public class AmazonEfsClient extends AmazonClient {
    private final EfsClient client;

    private final Retry retry;

    public AmazonEfsClient(EfsClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public CreateFileSystemResponse createFileSystem(CreateFileSystemRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.createFileSystem(request));
    }

    public DeleteFileSystemResponse deleteFileSystem(DeleteFileSystemRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.deleteFileSystem(request));
    }

    public DeleteMountTargetResponse deleteMountTarget(DeleteMountTargetRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.deleteMountTarget(request));
    }

    public DescribeFileSystemsResponse describeFileSystems(DescribeFileSystemsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeFileSystems(request));
    }

    public DescribeMountTargetsResponse describeMountTargets(DescribeMountTargetsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeMountTargets(request));
    }

    public ListTagsForResourceResponse listTagsForResource(ListTagsForResourceRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.listTagsForResource(request));
    }

    public TagResourceResponse tagResource(TagResourceRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.tagResource(request));
    }
}
