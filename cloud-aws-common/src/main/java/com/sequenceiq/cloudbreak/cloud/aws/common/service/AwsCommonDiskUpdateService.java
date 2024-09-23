package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DetachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.ModifyVolumeRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeModification;
import software.amazon.awssdk.services.ec2.model.VolumeModificationState;
import software.amazon.awssdk.services.ec2.model.VolumeState;

@Service
public class AwsCommonDiskUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCommonDiskUpdateService.class);

    private static final int MAX_READ_MODIFY_COUNT = 20;

    private static final int SLEEP_INTERVAL_MODIFY_SECONDS = 30;

    private static final int MAXIMUM_READ_COUNT = 15;

    private static final int SLEEP_INTERVAL_IN_SECONDS = 2;

    public void modifyVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception {
        AmazonEc2Client amazonEC2Client = getEc2Client(authenticatedContext);
        for (String volume: volumeIds) {
            LOGGER.debug("Modifying volume {} to disk type {} and size {}", volume, diskType, size);
            try {
                ModifyVolumeRequest.Builder modifyVolumeRequestBuilder = ModifyVolumeRequest.builder().volumeId(volume);
                buildModifyVolumeRequest(modifyVolumeRequestBuilder, diskType, size);
                amazonEC2Client.modifyVolume(modifyVolumeRequestBuilder.build());
            } catch (Ec2Exception | BadRequestException ex) {
                LOGGER.error("AWS threw BAD Request exception, while modifying volume: {}, exception: {}", volume, ex.getMessage());
                throw new CloudbreakException(format("Exception while modifying disk volume: %s, exception: %s", volume, ex.getMessage()));
            }
        }
        Polling.waitPeriodly(SLEEP_INTERVAL_MODIFY_SECONDS, TimeUnit.SECONDS).stopIfException(false).stopAfterAttempt(MAX_READ_MODIFY_COUNT)
            .run(() -> {
                LOGGER.debug("Getting volume modification states - {}", volumeIds);
                boolean volumeStateIsNotComplete = getVolumeModificationsState(volumeIds, amazonEC2Client).stream()
                        .map(VolumeModification::modificationState)
                        .anyMatch(state -> !state.equals(VolumeModificationState.COMPLETED) && !state.equals(VolumeModificationState.FAILED)
                                && !state.equals(VolumeModificationState.OPTIMIZING));
                LOGGER.debug("Result of checking all volumes available - {}", volumeStateIsNotComplete);
                if (volumeStateIsNotComplete) {
                    return AttemptResults.justContinue();
                }
                return AttemptResults.justFinish();
            });
        List<String> failedVolumes = getVolumeModificationsState(volumeIds, amazonEC2Client).stream()
                .filter(volMod -> volMod.modificationState().equals(VolumeModificationState.FAILED)).map(VolumeModification::volumeId).toList();
        if (!CollectionUtils.isEmpty(failedVolumes)) {
            throw new CloudbreakException(format("Some volumes were not modified: %s, please retry after 6 hours",
                    String.join("'", failedVolumes)));
        }
    }

    private void buildModifyVolumeRequest(ModifyVolumeRequest.Builder modifyVolumeRequestBuilder, String diskType, int size) throws BadRequestException {
        if (null == diskType && size == 0) {
            throw new BadRequestException("At least one of disk type or size should be provided for disk modification.");
        }
        if (null != diskType) {
            modifyVolumeRequestBuilder.volumeType(diskType);
        }
        if (size > 0) {
            modifyVolumeRequestBuilder.size(size);
        }
    }

    private AmazonEc2Client getEc2Client(AuthenticatedContext authenticatedContext) {
        return new AuthenticatedContextView(authenticatedContext).getAmazonEC2Client();
    }

    protected List<VolumeModification> getVolumeModificationsState(List<String> volumeIds, AmazonEc2Client amazonEC2Client) {
        LOGGER.debug("Checking modification status for Volumes {}", volumeIds);
        DescribeVolumesModificationsRequest describeVolumesModificationsRequest = DescribeVolumesModificationsRequest.builder()
                .volumeIds(volumeIds).build();
        return amazonEC2Client.describeVolumeModifications(describeVolumesModificationsRequest).volumesModifications();
    }

    public void detachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        Map<String, List<VolumeSetAttributes.Volume>> instanceVolumeIdsMap = cloudResources.stream().filter(res -> null != res.getInstanceId())
                .collect(toMap(CloudResource::getInstanceId, resource -> resource.getParameter(CloudResource.ATTRIBUTES,
                        VolumeSetAttributes.class).getVolumes()));
        AmazonEc2Client amazonEC2Client = getEc2Client(authenticatedContext);
        List<String> volumeIdsToPoll = new ArrayList<>();
        Map<String, VolumeState> volumeStatesMap = getVolumeStates(instanceVolumeIdsMap.values().stream().flatMap(Collection::stream)
                .map(VolumeSetAttributes.Volume::getId).toList(), amazonEC2Client, Map.of());
        for (Map.Entry<String, List<VolumeSetAttributes.Volume>> entry: instanceVolumeIdsMap.entrySet()) {
            entry.getValue().forEach(volume -> {
                if (VolumeState.IN_USE.equals(volumeStatesMap.get(volume.getId()))) {
                    LOGGER.debug("Starting to detach volume - {}", volume.getId());
                    volumeIdsToPoll.add(volume.getId());
                    DetachVolumeRequest detachVolumeRequest = DetachVolumeRequest.builder().instanceId(entry.getKey()).volumeId(volume.getId())
                            .device(volume.getDevice()).build();
                    amazonEC2Client.detachVolume(detachVolumeRequest);
                }
            });
        }
        pollVolumeStates(amazonEC2Client, volumeIdsToPoll);
    }

    public void deleteVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        Map<String, List<VolumeSetAttributes.Volume>> instanceVolumeIdsMap = cloudResources.stream().filter(res -> null != res.getInstanceId())
                .collect(toMap(CloudResource::getInstanceId, resource -> resource.getParameter(CloudResource.ATTRIBUTES,
                        VolumeSetAttributes.class).getVolumes()));
        AmazonEc2Client amazonEC2Client = getEc2Client(authenticatedContext);
        for (Map.Entry<String, List<VolumeSetAttributes.Volume>> entry: instanceVolumeIdsMap.entrySet()) {
            entry.getValue().forEach(volume -> {
                LOGGER.debug("Deleting volume after detach - {}", volume.getId());
                DeleteVolumeRequest deleteVolumeRequest = DeleteVolumeRequest.builder().volumeId(volume.getId()).build();
                amazonEC2Client.deleteVolume(deleteVolumeRequest);
            });
        }
    }

    protected Map<String, VolumeState> getVolumeStates(List<String> volumeIds, AmazonEc2Client amazonEC2Client, Map<String, List<String>> filters) {
        DescribeVolumesRequest.Builder describeVolumesRequestBuilder = DescribeVolumesRequest.builder();
        if (!volumeIds.isEmpty()) {
            describeVolumesRequestBuilder.volumeIds(volumeIds);
        } else if (!filters.isEmpty()) {
            List<Filter> filterRequest = getFiltersForDescribeVolumeRequest(filters);
            describeVolumesRequestBuilder.filters(filterRequest);
        }
        try {
            DescribeVolumesResponse volumesResponse = amazonEC2Client.describeVolumes(describeVolumesRequestBuilder.build());
            if (volumesResponse.hasVolumes()) {
                return volumesResponse.volumes().stream().collect(Collectors.toMap(software.amazon.awssdk.services.ec2.model.Volume::volumeId,
                        software.amazon.awssdk.services.ec2.model.Volume::state));
            }
            return Map.of("", VolumeState.UNKNOWN_TO_SDK_VERSION);
        } catch (Exception ex) {
            String exceptionMessage = format("Exception while querying the volume states for volumes - %s. returning empty map. Exception - %s",
                    volumeIds, ex.getMessage());
            LOGGER.warn(exceptionMessage);
            throw new CloudbreakServiceException(exceptionMessage);
        }
    }

    protected Map<String, List<Volume>> getVolumesInAvailableStatusByTagsFilter(AmazonEc2Client amazonEC2Client,
            Map<String, List<String>> tagsFilter) {
        DescribeVolumesRequest.Builder describeVolumesRequestBuilder = DescribeVolumesRequest.builder();
        List<Filter> filterRequest = getFiltersForDescribeVolumeRequest(tagsFilter);
        describeVolumesRequestBuilder.filters(filterRequest);
        try {
            DescribeVolumesResponse volumesResponse = amazonEC2Client.describeVolumes(describeVolumesRequestBuilder.build());
            if (volumesResponse.hasVolumes()) {
                return volumesResponse.volumes().stream().collect(Collectors.groupingBy(vol -> vol.tags().stream()
                                .filter(tag -> tag.key().equals("created-for")).map(Tag::value).findAny().orElseThrow()));
            }
        } catch (Exception ex) {
            String exceptionMessage = format("Exception while querying the volume for volume filters - %s. returning empty map. Exception - %s",
                    tagsFilter, ex.getMessage());
            LOGGER.warn(exceptionMessage);
        }
        return Map.of();
    }

    protected void pollVolumeStates(AmazonEc2Client amazonEC2Client, List<String> volumeIdsToPoll) {
        LOGGER.debug("Polling volume states - {}", volumeIdsToPoll);
        if (!volumeIdsToPoll.isEmpty()) {
            Polling.waitPeriodly(SLEEP_INTERVAL_IN_SECONDS, TimeUnit.SECONDS).stopIfException(true).stopAfterAttempt(MAXIMUM_READ_COUNT)
                .run(() -> {
                    LOGGER.debug("Getting volume states - {}", volumeIdsToPoll);
                    Map<String, VolumeState> volumeStateMap = getVolumeStates(volumeIdsToPoll, amazonEC2Client, Map.of());
                    List<String> volumeIdsNotInAvailable = volumeStateMap.keySet().stream()
                            .filter(key -> !volumeStateMap.get(key).equals(VolumeState.AVAILABLE)).toList();
                    boolean volumeStateIsNotAvailable = volumeIdsNotInAvailable.size() > 0;
                    LOGGER.debug("Result of checking all volumes available - {}", volumeIdsNotInAvailable);
                    if (volumeStateIsNotAvailable) {
                        return AttemptResults.justContinue();
                    }
                    return AttemptResults.justFinish();
                });
        }
    }

    protected List<Filter> getFiltersForDescribeVolumeRequest(Map<String, List<String>> filterInputs) {
        List<Filter> filters = Lists.newArrayList();
        for (String key : filterInputs.keySet()) {
            Filter filter = Filter.builder().name(key).values(filterInputs.get(key)).build();
            filters.add(filter);
        }
        Filter availableFilter = Filter.builder().name("status").values("available").build();
        filters.add(availableFilter);
        return filters;
    }
}
