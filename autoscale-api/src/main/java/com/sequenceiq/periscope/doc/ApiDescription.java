package com.sequenceiq.periscope.doc;

public class ApiDescription {

    public static final String ALERT_DESCRIPTION = "Operations on alerts";
    public static final String CONFIGURATION_DESCRIPTION = "Operations on configurations";
    public static final String HISTORY_DESCRIPTION = "Operations on histories";
    public static final String CLUSTERS_DESCRIPTION = "Operations on clusters";
    public static final String POLICIES_DESCRIPTION = "Operations on policies";

    public static class AlertOpDescription {
        public static final String METRIC_BASED_POST = "create alert which metric based";
        public static final String METRIC_BASED_PUT = "modify alert which metric based";
        public static final String METRIC_BASED_GET = "retrieve alert which metric based";
        public static final String METRIC_BASED_DELETE = "delete alert which metric based";
        public static final String METRIC_BASED_DEFINITIONS = "retrieve alert definitions";

        public static final String TIME_BASED_POST = "create alert which are time based";
        public static final String TIME_BASED_PUT = "modify alert which are time based";
        public static final String TIME_BASED_GET = "retrieve alert which are time based";
        public static final String TIME_BASED_DELETE = "delete alert which are time based";
        public static final String TIME_BASED_CRON = "cron expression validation";

        public static final String LOAD_BASED_POST = "create alert which is load based";
        public static final String LOAD_BASED_PUT = "modify alert which is load based";
        public static final String LOAD_BASED_GET = "retrieve alert which is load based";
        public static final String LOAD_BASED_DELETE = "delete alert which is load based";

        public static final String PROMETHEUS_BASED_POST = "create alert which prometheus based";
        public static final String PROMETHEUS_BASED_PUT = "modify alert which prometheus based";
        public static final String PROMETHEUS_BASED_GET = "retrieve alert which prometheus based";
        public static final String PROMETHEUS_BASED_DELETE = "delete alert which prometheus based";
        public static final String PROMETHEUS_BASED_DEFINITIONS = "retrieve Prometheus alert rule definitions";
    }

    public static class AlertNotes {
        public static final String TIME_BASED_NOTES = "Auto-scaling supports two Alert types: load and time based. "
                + "Time based alerts are based on cron expressions and allow alerts to be triggered based on time.";
        public static final String METRIC_BASED_NOTES = "Auto-scaling supports two Alert types: metric and time based. "
                + "Metric based alerts are using the default (or custom) Ambari metrics. These metrics have a default Threshold value configured in Ambari - "
                + "nevertheless these thresholds can be configured, changed or altered in Ambari. In order to change the default threshold for a metric "
                + "please go to Ambari UI and select the Alerts tab and the metric. The values can be changed in the Threshold section. ";
        public static final String PROMETHEUS_BASED_NOTES = "Prometheus based alerts are using Prometheus under the hood. ";
        public static final String LOAD_BASED_NOTES = "Load based alerts are based on the load observed in the cluster ";
    }

    public static class ConfigurationOpDescription {
        public static final String CONFIGURATION_POST = "create configuration";
        public static final String CONFIGURATION_GET = "retrieve configuration";
    }

    public static class ConfigurationNotes {
        public static final String NOTES = "An SLA scaling policy can contain multiple alerts. When an alert is triggered a scaling adjustment is applied,"
                + " however to keep the cluster size within boundaries a cluster size min. and cluster size max. is attached to the cluster - "
                + "thus a scaling policy can never over or undersize a cluster. Also in order to avoid stressing the cluster we have introduced a "
                + "cooldown time period (minutes) - though an alert is raised and there is an associated scaling policy, the system will not apply "
                + "the policy within the configured timeframe. In an SLA scaling policy the triggered rules are applied in order.";
    }

    public static class HistoryOpDescription {
        public static final String HISTORY_GET_ALL = "retrieve full history";
        public static final String HISTORY_GET = "retrieve a specific history";
    }

    public static class HistoryNotes {
        public static final String NOTES = "Get Auto-scaling history on a specific cluster";
    }

    public static class PolicyOpDescription {
        public static final String POLICY_POST = "create policy";
        public static final String POLICY_PUT = "modify policy";
        public static final String POLICY_GET = "retrieve policy";
        public static final String POLICY_DELETE = "delete policy";
    }

    public static class PolicyNotes {
        public static final String NOTES = "Scaling is the ability to increase or decrease the capacity of the Hadoop cluster or application "
                + "based on an alert. When scaling policies are used, the capacity is automatically increased or decreased according to the "
                + "conditions defined. Cloudbreak will do the heavy lifting and based on the alerts and the scaling policy linked to them it "
                + "executes the associated policy. We scaling granularity is at the hostgroup level - thus you have the option to scale services "
                + "or components only, not the whole cluster.";
    }

    public static class ClusterOpDescription {
        public static final String CLUSTER_POST = "create cluster";
        public static final String CLUSTER_PUT = "modify cluster";
        public static final String CLUSTER_GET = "retrieve cluster";
        public static final String CLUSTER_GET_ALL = "retrieve all cluster";
        public static final String CLUSTER_DELETE = "delete cluster";
        public static final String CLUSTER_SET_STATE = "set cluster state";
        public static final String CLUSTER_SET_AUTOSCALE_STATE = "enable or disable cluster's autoscale feature";
        public static final String CLUSTER_UPDATE_AUTOSCALE_CONFIG = "update cluster's autoscale config";
        public static final String CLUSTER_DELETE_ALERTS = "delete a cluster's alerts";
    }

    public static class ClusterNotes {
        public static final String NOTES = "Ambari cluster.";
    }

    public static class DistroXClusterNotes {
        public static final String NOTES = "DataHub cluster.";
    }

    public static class ClusterJsonsProperties {
        public static final String WORKSPACE_ID = "Workspace id";
        public static final String HOST = "Ambari server host address";
        public static final String PORT = "Ambari server port";
        public static final String USERNAME = "Ambari server username";
        public static final String PASSWORD = "Ambari server password";
        public static final String STACK_CRN = "crn of the stack in Cloudbreak";
        public static final String ENABLE_AUTOSCALING = "Enable or Disable the Autoscaling feature set on the underlying Periscope cluster";
        public static final String AUTOSCALING_ENABLED = "Indicate that the Autoscaling feature set is Enabled or Disabled";
        public static final String ID = "Id of the cluster";
        public static final String STACK_NAME = "Name of stack in Cloudbreak";
        public static final String STACK_TYPE = "Type of stack in Cloudbreak";
        public static final String STATE = "State of the cluster";
        public static final String METRIC_ALERTS = "Metric based alerts of the cluster";
        public static final String TIME_ALERTS = "Time based alerts of the cluster";
        public static final String PROMETHEUS_ALERTS = "Prometheus based alerts of the cluster";
        public static final String LOAD_ALERTS = "Load based alerts of the cluster";
        public static final String SCALING_CONFIGURATION = "Scaling configuration for the cluster";
    }

    public static class ScalingConfigurationJsonProperties {
        public static final String MINSIZE = "The minimum size of the cluster after scaling";
        public static final String MAXSIZE = "The maximum size of the cluster after scaling";
        public static final String COOLDOWN = "The time between two scaling activities";
    }

    public static class ScalingPolicyJsonProperties {
        public static final String ID = "Id of the policy";
        public static final String NAME = "Name of the policy";
        public static final String ADJUSTMENTTYPE = "Type of the scaling count";
        public static final String SCALINGADJUSTMENT = "Count of the scaling";
        public static final String ALERTID = "Id of the alert which trigger the scaling";
        public static final String HOSTGROUP = "Name of hostgroup which affected by the scaling";
    }

    public static class StateJsonProperties {
        public static final String STATE = "State of the cluster";
    }

    public static class ClusterAutoscaleState {
        public static final String ENABLE_AUTOSCALING = "field to switch on or off autoscaling feature";
    }

    public static class TimeAlertJsonProperties {
        public static final String TIMEZONE = "Timezone of the time alert";
        public static final String CRON = "Cron expression of the time alert";
    }

    public static class MetricAlertJsonProperties {
        public static final String ALERTDEFINITION = "Definition of the alert";
        public static final String PERIOD = "Period of the alert";
        public static final String ALERTSTATE = "State of the alert";
    }

    public static class PrometheusAlertJsonProperties {
        public static final String ALERTRULE = "Name of the predefined Prometheus alert rule that could be parameterized by the period and threshold fields.";
        public static final String PERIOD = "Period of the alert";
        public static final String THRESHOLD = "Threshold of the alert in percent";
        public static final String ALERTSTATE = "State of the alert";
        public static final String ALERTOPERATOR = "Operator of the alert's query.";
    }

    public static class LoadAlertJsonProperties {
        public static final String LOAD_ALERT_CONFIGURATION_MIN_RESOUCE_VALUE = "The lower bound for the resource";
        public static final String LOAD_ALERT_CONFIGURATION_MAX_RESOUCE_VALUE = "The upper bound for the resource";
        public static final String LOAD_ALERT_CONFIGURATION_COOL_DOWN_MINS_VALUE = "CoolDown between successive cluster autoscaling actions.";
        public static final String LOAD_ALERT_CONFIGURATION = "Configuration of Load Alert";
    }

    public static class BaseAlertJsonProperties {
        public static final String ID = "Id of the alert";
        public static final String ALERTNAME = "Name of the alert";
        public static final String DESCRIPTION = "Description of the alert";
        public static final String SCALINGPOLICYID = "Id of the scaling ploicy";
    }

    public static class HistoryJsonProperties {
        public static final String ID = "Id of the history object";
        public static final String CLUSTERID = "If of the cluster";
        public static final String CBSTACKCRN = "crn of the cloudbreak stack";
        public static final String ORIGINALNODECOUNT = "The node count before of the scaling";
        public static final String ADJUSTMENT = "Count of scaling";
        public static final String ADJUSTMENTTYPE = "Type of adjustment";
        public static final String SCALINGSTATUS = "Status of scaling activity";
        public static final String STATUSREASON = "Reason of the status";
        public static final String TIMESTAMP = "Time of the creation";
        public static final String HOSTGROUP = "The affected hostgroup name";
        public static final String ALERTTYPE = "Type of the alert event";
        public static final String PROPERTIES = "Additional properties";
    }

    private ApiDescription() {
    }
}
