package com.sequenceiq.datalake.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

public class SdxRepairSettings {

    private List<String> hostGroupNames = new ArrayList<>();

    private List<String> nodeIds = new ArrayList<>();

    private SdxRepairSettings() {
    }

    public static SdxRepairSettings from(SdxRepairRequest request) {
        if (StringUtils.isBlank(request.getHostGroupName())
                && CollectionUtils.isEmpty(request.getHostGroupNames())
                && CollectionUtils.isEmpty(request.getNodesIds())) {
            throw new BadRequestException("Please select the repairable host groups or nodes.");
        }
        if (Stream.of(StringUtils.isNotBlank(request.getHostGroupName()),
                CollectionUtils.isNotEmpty(request.getHostGroupNames()),
                CollectionUtils.isNotEmpty(request.getNodesIds()))
                .filter(Predicate.isEqual(true)).count() > 1) {
            throw new BadRequestException("Please select one host group ('hostGroupName'), multiple host groups ('hostGroupNames')" +
                    ", or nodes ('nodesIds').");
        }
        SdxRepairSettings settings = new SdxRepairSettings();
        if (Strings.isNotEmpty(request.getHostGroupName())) {
            settings.hostGroupNames.add(request.getHostGroupName());
        } else if (CollectionUtils.isNotEmpty(request.getHostGroupNames())) {
            settings.hostGroupNames.addAll(request.getHostGroupNames());
        } else {
            settings.nodeIds.addAll(request.getNodesIds());
        }
        return settings;
    }

    public List<String> getHostGroupNames() {
        return hostGroupNames;
    }

    public List<String> getNodeIds() {
        return nodeIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SdxRepairSettings that = (SdxRepairSettings) o;
        return hostGroupNames.equals(that.hostGroupNames) && nodeIds.equals(that.nodeIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostGroupNames, nodeIds);
    }
}
