{%- from 'metadata/settings.sls' import metadata with context %}

create_metadata_dir:
  file.directory:
    - name: /opt/metadata

ambari_server_metadata:
  file.serialize:
    - name: /opt/metadata/cluster.json
    - dataset:
        {{ metadata }}
    - formatter: json