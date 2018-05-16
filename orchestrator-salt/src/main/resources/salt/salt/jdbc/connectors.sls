{% if salt['pillar.get']('jdbc_connectors:databaseType') != None %}

download_custom_{{ salt['pillar.get']('jdbc_connectors:databaseType') }}_connector_jar:
  file.managed:
     - name: /opt/jdbc-drivers/{{ salt['pillar.get']('jdbc_connectors:connectorJarName') }}
     - source: {{ salt['pillar.get']('jdbc_connectors:connectorJarUrl') }}
     - makedirs: True
     - mode: 755
     - skip_verify: True

{% endif %}