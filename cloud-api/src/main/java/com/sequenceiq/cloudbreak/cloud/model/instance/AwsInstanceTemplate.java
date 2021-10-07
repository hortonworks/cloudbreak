package com.sequenceiq.cloudbreak.cloud.model.instance;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;

public class AwsInstanceTemplate extends InstanceTemplate {

    /**
     * Key of the optional dynamic parameter denoting whether EBS encryption is enabled or not. This applies to both root & attached (data) volumes.
     *
     * <p>
     *     Permitted values:
     *     <ul>
     *         <li>{@code Boolean.TRUE} instance, {@code "true"} (ignoring case): Encryption is enabled.</li>
     *         <li>{@code Boolean.FALSE} instance, {@code "false"} (or any other {@code String} not equal to {@code "true"} ignoring case), {@code null}:
     *         Encryption is disabled.</li>
     *     </ul>
     * </p>
     *
     * If enabled, the encryption key to use is determined by {@link #VOLUME_ENCRYPTION_KEY_TYPE} and {@link #VOLUME_ENCRYPTION_KEY_ID}.
     *
     * @see #VOLUME_ENCRYPTION_KEY_ID
     * @see #VOLUME_ENCRYPTION_KEY_TYPE
     * @see #putParameter(String, Object)
     * @see Boolean#parseBoolean(String)
     */
    public static final String EBS_ENCRYPTION_ENABLED = "encrypted";

    /**
     * Key of the optional dynamic parameter denoting the percentage of EC2 instances to be allocated to spot instances. The remaining fraction of the total
     * amount of EC2 instances will be running on on-demand instances.
     *
     * <p>
     *     Permitted values:
     *     <ul>
     *         <li>{@code null}: Equivalent with a setting of 0 (i.e. 100% allocation to on-demand instances).</li>
     *         <li>An {@link Integer} {@code i} in the range [0, 100], both inclusive: {@code i}% allocation to spot instances, (100 - {@code i})% allocation
     *         to on-demand instances.</li>
     *     </ul>
     * </p>
     *
     * @see #putParameter(String, Object)
     */
    public static final String EC2_SPOT_PERCENTAGE = "spotPercentage";

    /**
     * Key of the optional dynamic parameter denoting the max hourly price of EC2 instances to be allocated to spot instances.
     *
     * <p>
     *     Permitted values:
     *     <ul>
     *         <li>{@code null}: AWS will automatically determine the maximum price, which is the current price of an on-demand instance.</li>
     *         <li>A {@link Double} in the range [0.001, 255] with maximum 4 fraction digits.</li>
     *     </ul>
     * </p>
     *
     * @see #putParameter(String, Object)
     */
    public static final String EC2_SPOT_MAX_PRICE = "spotMaxPrice";

    /**
     * Key of the optional dynamic parameter denoting PlacementGroup Strategy for the instance.
     *
     * <p>
     *     Permitted values:
     *     <ul>
     *         <li>{@code "NONE"}: This setting does not configure any placement group.</li>
     *         <li>{@code "CLUSTER"}: This setting configures placement group strategy as "cluster".</li>
     *         <li>{@code "PARTITION"}: This setting configures placement group strategy as "partition".</li>
     *         <li>{@code "SPREAD"}: This setting configures placement group strategy as "spread".</li>
     *     </ul>
     * </p>
     *
     * @see #putParameter(String, Object)
     */
    public static final String PLACEMENT_GROUP_STRATEGY = "placementGroupStrategy";

    public AwsInstanceTemplate(String flavor, String groupName, Long privateId, Collection<Volume> volumes, InstanceStatus status,
            Map<String, Object> parameters, Long templateId, String imageId, TemporaryStorage temporaryStorage, Long temporaryStorageCount) {
        super(flavor, groupName, privateId, volumes, status, parameters, templateId, imageId, temporaryStorage, temporaryStorageCount);
    }

}
