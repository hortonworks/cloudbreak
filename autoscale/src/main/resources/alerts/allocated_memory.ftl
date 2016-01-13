{
  "AlertDefinition": {
    "cluster_name": "${clusterName}",
    "component_name": "RESOURCEMANAGER",
    "description": "This alarm triggers if the available memory falls below a certain threshold. The threshold values are in percent.",
    "enabled": true,
    "ignore_host": false,
    "interval": 5,
    "label": "Allocated memory",
    "name": "allocated_memory",
    "scope": "ANY",
    "service_name": "YARN",
    "source": {
      "jmx": {
        "property_list": [
          "Hadoop:service=ResourceManager,name=QueueMetrics,q0=root/AvailableMB",
          "Hadoop:service=ResourceManager,name=QueueMetrics,q0=root/AllocatedMB"
        ],
        "value": "{0}/({0} + {1}) * 100"
      },
      "reporting": {
        "ok": {
          "text": "Memory available: {0} MB, allocated: {1} MB"
        },
        "warning": {
          "text": "Memory available: {0} MB, allocated: {1} MB",
          "value": 80
        },
        "critical": {
          "text": "Memory available: {0} MB, allocated: {1} MB",
          "value": 95
        },
        "units": "%"
      },
      "type": "METRIC",
      "uri": {
        "http": "{{yarn-site/yarn.resourcemanager.webapp.address}}",
        "https": "{{yarn-site/yarn.resourcemanager.webapp.https.address}}",
        "https_property": "{{yarn-site/yarn.http.policy}}",
        "https_property_value": "HTTPS_ONLY",
        "default_port": 0,
        "high_availability": {
          "alias_key": "{{yarn-site/yarn.resourcemanager.ha.rm-ids}}",
          "http_pattern": "{{yarn-site/yarn.resourcemanager.webapp.address.{{alias}}}}",
          "https_pattern": "{{yarn-site/yarn.resourcemanager.webapp.https.address.{{alias}}}}"
        }
      }
    }
  }
}