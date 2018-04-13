package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsResponse;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
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
        ambariRepoDetailsJson.setStack(source.getStack());
        ambariRepoDetailsJson.setUtil(source.getUtil());
        ambariRepoDetailsJson.setEnableGplRepo(source.isEnableGplRepo());
        if (!source.getMpacks().isEmpty()) {
            ambariRepoDetailsJson.setMpacks(source.getMpacks().stream().map(mp -> conversionService.convert(
                    mp, ManagementPackDetails.class)).collect(Collectors.toList()));
        }
        return ambariRepoDetailsJson;
    }
}
