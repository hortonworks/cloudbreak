package com.sequenceiq.freeipa.converter.stack;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.converter.authentication.StackAuthenticationToStackAuthenticationResponseConverter;
import com.sequenceiq.freeipa.converter.freeipa.FreeIpaToFreeIpaServerResponseConverter;
import com.sequenceiq.freeipa.converter.image.ImageToImageSettingsResponseConverter;
import com.sequenceiq.freeipa.converter.instance.InstanceGroupToInstanceGroupResponseConverter;
import com.sequenceiq.freeipa.converter.network.NetworkToNetworkResponseConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class StackToDescribeFreeIpaResponseConverter {

    @Inject
    private StackAuthenticationToStackAuthenticationResponseConverter authenticationResponseConverter;

    @Inject
    private ImageToImageSettingsResponseConverter imageSettingsResponseConverter;

    @Inject
    private FreeIpaToFreeIpaServerResponseConverter freeIpaServerResponseConverter;

    @Inject
    private NetworkToNetworkResponseConverter networkResponseConverter;

    @Inject
    private InstanceGroupToInstanceGroupResponseConverter instanceGroupConverter;

    public DescribeFreeIpaResponse convert(Stack stack, Image image, FreeIpa freeIpa) {
        DescribeFreeIpaResponse describeFreeIpaResponse = new DescribeFreeIpaResponse();
        describeFreeIpaResponse.setName(stack.getName());
        describeFreeIpaResponse.setEnvironmentId(stack.getEnvironment());
        describeFreeIpaResponse.setAuthentication(authenticationResponseConverter.convert(stack.getStackAuthentication()));
        Optional.ofNullable(image).ifPresent(i -> describeFreeIpaResponse.setImage(imageSettingsResponseConverter.convert(i)));
        Optional.ofNullable(freeIpa).ifPresent(f -> describeFreeIpaResponse.setFreeIpa(freeIpaServerResponseConverter.convert(f)));
        describeFreeIpaResponse.setNetwork(networkResponseConverter.convert(stack));
        describeFreeIpaResponse.setPlacement(convert(stack));
        describeFreeIpaResponse.setInstanceGroups(instanceGroupConverter.convert(stack.getInstanceGroups()));
        return describeFreeIpaResponse;
    }

    private PlacementResponse convert(Stack source) {
        PlacementResponse placementResponse = new PlacementResponse();
        placementResponse.setAvailabilityZone(source.getAvailabilityZone());
        placementResponse.setRegion(source.getRegion());
        return placementResponse;
    }
}
