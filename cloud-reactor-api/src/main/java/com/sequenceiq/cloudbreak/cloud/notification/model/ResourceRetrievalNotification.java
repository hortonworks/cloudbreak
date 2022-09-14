package com.sequenceiq.cloudbreak.cloud.notification.model;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import reactor.rx.Promise;
import reactor.rx.Promises;

public class ResourceRetrievalNotification {

    private final List<String> resourceReferences;

    private final CommonStatus status;

    private final ResourceType resourceType;

    private final Long stackId;

    private final Promise<List<CloudResource>> promise;

    public ResourceRetrievalNotification(List<String> resourceReferences, CommonStatus status, ResourceType resourceType) {
        this(resourceReferences, status, resourceType, null);
    }

    public ResourceRetrievalNotification(List<String> resourceReferences, CommonStatus status, ResourceType resourceType, Long stackId) {
        this.resourceReferences = resourceReferences;
        this.status = status;
        this.resourceType = resourceType;
        this.promise = Promises.prepare();
        this.stackId = stackId;
    }

    public List<String> getResourceReferences() {
        return resourceReferences;
    }

    public CommonStatus getStatus() {
        return status;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public Promise<List<CloudResource>> getPromise() {
        return promise;
    }

    public List<CloudResource> getResult() {
        try {
            return promise.await();
        } catch (InterruptedException e) {
            throw new CloudConnectorException("ResourceNotification has been interrupted", e);
        }
    }

    public Long getStackId() {
        return stackId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResourceRetrievalNotification{");
        sb.append("resourceReference=").append(resourceReferences);
        sb.append("status=").append(status);
        sb.append("resourceType=").append(resourceType);
        sb.append(", promise=").append(promise);
        sb.append(", stackId=").append(stackId);
        return sb.toString();
    }
}
