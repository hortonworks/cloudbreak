prometheus:
  server:
      source: https://github.com/prometheus/prometheus/releases/download/v1.4.1/prometheus-1.4.1.linux-amd64.tar.gz
      source_hash: md5=6cfb712ef7f33f42611bf7ebb02bc740
      config:
        global:
          scrape_interval:     15s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
          evaluation_interval: 15s # Evaluate rules every 15 seconds. The default is every 1 minute.
          # scrape_timeout is set to the global default (10s).

          # Attach these labels to any time series or alerts when communicating with
          # external systems (federation, remote storage, Alertmanager).

        # Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
        rule_files:
          - "/etc/prometheus/rules/*.rule"

        # A scrape configuration containing exactly one endpoint to scrape:
        # Here it's Prometheus itself.

        scrape_configs:
          - job_name: 'datanode'
            metrics_path: /prom/metrics
            consul_sd_configs:
              - server:    'consul.service.consul:8500'
                datacenter: 'dc1'
                services: [ 'datanode' ]
            relabel_configs:
              - source_labels: ['__meta_consul_service']
                regex:         '(.*)'
                target_label:  'job'
                replacement:   '$1'
              - source_labels: ['__meta_consul_tags']
                regex:         '.*,jobname=(.*?),.*'
                target_label:  'job'
                replacement:   '$1'
              - source_labels: ['__meta_consul_node']
                regex:         '(.*)'
                target_label:  'instance'
                replacement:   '$1'
              - source_labels: ['__meta_consul_tags']
                regex:         ',(installed|maintenance),'
                target_label:  'service_status'
                replacement:   '$1'
          - job_name: 'namenode'
            metrics_path: /prom/metrics
            consul_sd_configs:
              - server:    'consul.service.consul:8500'
                datacenter: 'dc1'
                services: [ 'namenode' ]
            relabel_configs:
               - source_labels: ['__meta_consul_service']
                 regex:         '(.*)'
                 target_label:  'job'
                 replacement:   '$1'
               - source_labels: ['__meta_consul_tags']
                 regex:         '.*,jobname=(.*?),.*'
                 target_label:  'job'
                 replacement:   '$1'
               - source_labels: ['__meta_consul_node']
                 regex:         '(.*)'
                 target_label:  'instance'
                 replacement:   '$1'
               - source_labels: ['__meta_consul_tags']
                 regex:         ',(installed|maintenance),'
                 target_label:  'service_status'
                 replacement:   '$1'