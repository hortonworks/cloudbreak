{%- from 'metadata/settings.sls' import metadata, hostattrs with context %}

create_metadata_dir:
  file.directory:
    - name: /opt/metadata

cluster_metadata:
  file.serialize:
    - name: /opt/metadata/cluster.json
    - dataset:
        {{ metadata }}
    - formatter: json

node_attributes:
  file.serialize:
    - name: /opt/metadata/node.json
    - dataset:
        {{ hostattrs }}
    - formatter: json