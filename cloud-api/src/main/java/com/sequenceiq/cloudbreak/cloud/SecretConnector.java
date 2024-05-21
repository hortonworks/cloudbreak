package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.secret.CloudSecret;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.CreateCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.DeleteCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.GetCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.UpdateCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.UpdateCloudSecretResourceAccessRequest;

public interface SecretConnector extends CloudPlatformAware {

    CloudSecret createCloudSecret(CreateCloudSecretRequest request);

    CloudSecret getCloudSecret(GetCloudSecretRequest request);

    CloudSecret updateCloudSecretResourceAccess(UpdateCloudSecretResourceAccessRequest request);

    CloudSecret updateCloudSecret(UpdateCloudSecretRequest request);

    void deleteCloudSecret(DeleteCloudSecretRequest request);
}
