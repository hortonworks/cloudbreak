# Auto-Scaling

The purpose of `auto-scaling` is to apply SLA scaling policies to a Cloudbreak-managed Hadoop cluster.

> This feature is currently `TECHNICAL PREVIEW`.

##How It Works

The auto-scaling capabilities is based on [Ambari Metrics](https://cwiki.apache.org/confluence/display/AMBARI/Metrics) - and [Ambari Alerts](https://cwiki.apache.org/confluence/display/AMBARI/Alerts). Based on the Blueprint
used and the running services, Cloudbreak can access all the available metrics from the subsystem and define `alerts` based on this information.

Beside the default Ambari Metrics, Cloudbreak includes two custom metrics: `Pending YARN containers` and `Pending applications`. These two custom metrics works with the YARN subsystem in order to bring `application` level QoS to the cluster.

> In order to use the `autoscaling` feature with Cloudbreak you will have to enable from the UI or shell.

![](/images/enable_periscope.png)

####Alerts

Auto-scaling supports two **Alert** types: `metric` and `time` based.

**Metric-based Alerts**

Metric based alerts are using the default (or custom) Ambari metrics. These metrics have a default `Threshold` value configured in Ambari - nevertheless these thresholds can be configured, changed or altered in Ambari. In order to change the default threshold for a metric please go to Ambari UI and select the `Alerts` tab and the metric. The values can be changed in the `Threshold` section.

![](/images/ambari_threshold.png)

Metric alerts have a few configurable fields.

* `alert name` - name of the alert
* `description` - description of the alert
* `metric - desired state` - the Ambari metrics based on the installed services and their *state* (OK, WARN, CRITICAL), based on the *threshold* value
* `period` - for how many *minutes* the metric state has to be sustained in order for an alert to be triggered

![](/images/metric_alert.png)

**Time-based Alerts**

Time based alerts are based on `cron` expressions and allow alerts to be triggered based on time.

Time alerts have a few configurable fields.

* `alert name` - name of the alert
* `description` - description of the alert
* `time zone` - the time zone
* `crom expression` - the *cron* expression to be used for the alert

![](/images/time_alert.png)


####Scaling Policies
Scaling is the ability to increase or decrease the capacity of the Hadoop cluster or application based on an alert.
When scaling policies are used, the capacity is automatically increased or decreased according to the conditions defined.
Cloudbreak will do the heavy lifting and based on the alerts and the scaling policy linked to them it executes the associated policy. We scaling granularity is at the `hostgroup` level - thus you have the option to scale services or components only, not the whole cluster.

Scaling policies have a few configurable fields.

* `policy name` - name of the scaling policy
* `scaling adjustment` - the number of added or removed noded based on `node count` (the number of nodes), `percentage` (computed percentage adjustment based on the cluster size) and `exact` (a given exact size of the cluster)
* `host group` - the Ambari hostgroup to be scaled
* `alert` - the triggered alert based on that the scaling policy applies

![](/images/policy.png)

####Cluster Scaling Configuration

An SLA scaling policy can contain multiple alerts. When an alert is triggered a `scaling adjustment` is applied, however to keep the cluster size within boundaries a `cluster size min.` and `cluster size max.` is attached to the cluster - thus a scaling policy can never over or undersize a cluster. Also in order to avoid stressing the cluster we have introduced a `cooldown time` period (minutes) - though an alert is raised and there is an associated scaling policy, the system will not apply the policy within the configured timeframe. In an SLA scaling policy the triggered rules are applied in order.

* `cooldown time` - period (minutes) between two scaling events while the cluster is locked from adjustments
* `cluster size min.` - size will never go under the minimum value, despite scaling adjustments
* `cluster size max.` - size will never go above the maximum value, despite scaling adjustments

![](/images/scaling_config.png)

**Downscale Scaling Considerations**

Cloudbreak auto-scaling will try to keep a healthy cluster, thus does several background checks during `downscale`.

* We never remove `Application master nodes` from a cluster. In order to make sure that a node running AM is not removed, Cloudbreak has to be able to access the YARN Resource Manager - when creating a cluster using the `default` secure network template please make sure that the RM's port is open on the node
* In order to keep a healthy HDFS during downscale we always keep the configured `replication` factor and make sure there is enough `space` on HDFS to rebalance data. Also during downscale in order to minimize the rebalancing, replication and HDFS storms we check block locations and compute the least costly operations.
