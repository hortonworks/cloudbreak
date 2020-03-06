package com.sequenceiq.datalake.settings;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

public class SdxRepairSettings {

    private List<String> hostGroupNames;

    private SdxRepairSettings() {
    }

    public static SdxRepairSettings from(SdxRepairRequest request) {
        if (StringUtils.isNotBlank(request.getHostGroupName()) && CollectionUtils.isNotEmpty(request.getHostGroupNames())) {
            throw new BadRequestException("Please send only one hostGroupName in the 'hostGroupName' field " +
                    "or multiple hostGroups in the 'hostGroupNames' fields");
        }
        SdxRepairSettings settings = new SdxRepairSettings();
        if (Strings.isNotEmpty(request.getHostGroupName())) {
            settings.hostGroupNames = List.of(request.getHostGroupName());
        } else {
            settings.hostGroupNames = request.getHostGroupNames();
        }
        return settings;
    }

    public List<String> getHostGroupNames() {
        return hostGroupNames;
    }
}
