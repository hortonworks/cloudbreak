package com.sequenceiq.cloudbreak.service.identitymapping;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;

@Component
public class ObjectStorageValidateRequestDecorator {

    @Inject
    private AwsMockAccountMappingService awsMockAccountMappingService;

    @Inject
    private AzureMockAccountMappingService azureMockAccountMappingService;

    @Inject
    private GcpMockAccountMappingService gcpMockAccountMappingService;

    public void decorateWithMockAccountMapping(ObjectStorageValidateRequest request) {
        if (request.getCloudStorageRequest().getAccountMapping() == null) {
            Map<String, String> groupMappings = null;
            Map<String, String> userMappings = null;
            String adminGroupName = request.getMockAccountMappingSettings().getAdminGroupName();
            switch (request.getCloudPlatform()) {
                case AWS:
                    if (adminGroupName != null) {
                        groupMappings = awsMockAccountMappingService.getGroupMappings(request.getMockAccountMappingSettings().getRegion(),
                                request.getCredential(),
                                adminGroupName);
                    }
                    userMappings = awsMockAccountMappingService.getUserMappings(request.getMockAccountMappingSettings().getRegion(),
                            request.getCredential());
                    break;
                case AZURE:
                    if (adminGroupName != null) {
                        groupMappings = azureMockAccountMappingService.getGroupMappings(AzureMockAccountMappingService.MSI_RESOURCE_GROUP_NAME,
                                request.getCredential(),
                                adminGroupName);
                    }
                    userMappings = azureMockAccountMappingService.getUserMappings(AzureMockAccountMappingService.MSI_RESOURCE_GROUP_NAME,
                            request.getCredential());
                    break;
                case GCP:
                    if (adminGroupName != null) {
                        groupMappings = gcpMockAccountMappingService.getGroupMappings(request.getMockAccountMappingSettings().getRegion(),
                                request.getCredential(),
                                adminGroupName);
                    }
                    userMappings = gcpMockAccountMappingService.getUserMappings(request.getMockAccountMappingSettings().getRegion(),
                            request.getCredential());
                    break;
                default:
            }
            AccountMappingBase accountMappingBase = new AccountMappingBase();
            accountMappingBase.setGroupMappings(groupMappings);
            accountMappingBase.setUserMappings(userMappings);
            request.getCloudStorageRequest().setAccountMapping(accountMappingBase);
        }
    }
}
