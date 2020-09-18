package com.sequenceiq.cloudbreak.cloud.notification.model;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import reactor.rx.Promise;
import reactor.rx.Promises;

public class ResourceRetrievalNotification {

    private final String resourceReference;

    private final CommonStatus status;

    private final ResourceType resourceType;

    private final Promise<Optional<CloudResource>> promise;

    public ResourceRetrievalNotification(String resourceReference, CommonStatus status, ResourceType resourceType) {
        this.resourceReference = resourceReference;
        this.status = status;
        this.resourceType = resourceType;
        this.promise = Promises.prepare();
    }

    public String getResourceReference() {
        return resourceReference;
    }

    public CommonStatus getStatus() {
        return status;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public Promise<Optional<CloudResource>> getPromise() {
        return promise;
    }

    public Optional<CloudResource> getResult() {
        try {
            return promise.await();
        } catch (InterruptedException e) {
            throw new CloudConnectorException("ResourceNotification has been interrupted", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResourceRetrievalNotification{");
        sb.append("resourceReference=").append(resourceReference);
        sb.append("status=").append(status);
        sb.append("resourceType=").append(resourceType);
        sb.append(", promise=").append(promise);
        return sb.toString();
    }
}
