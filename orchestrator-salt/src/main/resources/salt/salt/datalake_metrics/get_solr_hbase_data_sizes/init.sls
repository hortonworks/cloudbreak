/opt/salt/scripts/get_solr_hbase_data_sizes.sh:
  file.managed:
    - makedirs: True
    - mode: 750
    - source: salt://datalake_metrics/get_solr_hbase_data_sizes/scripts/get_solr_hbase_data_sizes.sh
