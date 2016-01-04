package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component("CollectMetadataAction")
public class CollectMetadataAction extends AbstractStackCreationAction<CollectMetadataResult> {
    @Inject
    private StackCreationService stackCreationService;
    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    public CollectMetadataAction() {
        super(CollectMetadataResult.class);
    }

    @Override
    protected Long getStackId(CollectMetadataResult payload) {
        return payload.getRequest().getCloudContext().getId();
    }

    @Override
    protected void doExecute(StackContext context, CollectMetadataResult payload, Map<Object, Object> variables) {
        Stack stack = stackCreationService.setupMetadata(context, payload);
        InstanceMetaData gatewayMetaData = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        CloudInstance gatewayInstance = metadataConverter.convert(gatewayMetaData);
        GetSSHFingerprintsRequest<GetSSHFingerprintsResult> sshFingerprintReq = new GetSSHFingerprintsRequest<>(context.getCloudContext(),
                context.getCloudCredential(), gatewayInstance);
        sendEvent(context.getFlowId(), sshFingerprintReq.selector(), sshFingerprintReq);
    }
}
