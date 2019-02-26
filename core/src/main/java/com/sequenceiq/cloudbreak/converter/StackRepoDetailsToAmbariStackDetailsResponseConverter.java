package com.sequenceiq.cloudbreak.converter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsResponse;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;

@Component
public class StackRepoDetailsToAmbariStackDetailsResponseConverter
        extends AbstractConversionServiceAwareConverter<StackRepoDetails, AmbariStackDetailsResponse> {
    @Inject
    private ConversionService conversionService;

    @Override
    public AmbariStackDetailsResponse convert(StackRepoDetails source) {
        AmbariStackDetailsResponse ambariRepoDetailsJson = new AmbariStackDetailsResponse();
        ambariRepoDetailsJson.setHdpVersion(source.getHdpVersion());
        ambariRepoDetailsJson.setVerify(source.isVerify());
        ambariRepoDetailsJson.setStack(Maps.newHashMap(source.getStack()));
        ambariRepoDetailsJson.setUtil(source.getUtil());
        ambariRepoDetailsJson.setEnableGplRepo(source.isEnableGplRepo());
        if (!source.getMpacks().isEmpty()) {
            List<ManagementPackDetails> mpacks = source.getMpacks().stream().filter(mp -> !mp.isStackDefault()).map(mp -> conversionService.convert(
                    mp, ManagementPackDetails.class)).collect(Collectors.toList());
            ambariRepoDetailsJson.setMpacks(mpacks);
            Optional<ManagementPackComponent> stackDefaultMpack = source.getMpacks().stream().filter(ManagementPackComponent::isStackDefault).findFirst();
            stackDefaultMpack.ifPresent(mp -> ambariRepoDetailsJson.getStack().put(StackRepoDetails.MPACK_TAG, mp.getMpackUrl()));
        }
        return ambariRepoDetailsJson;
    }
}
