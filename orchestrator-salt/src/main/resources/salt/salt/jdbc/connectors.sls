{% if salt['pillar.get']('jdbc_connectors:MYSQL') != "" %}

install-mysql-client:
  pkg.installed:
     - name: mariadb

download_custom_mysql_connector_jar:
  file.managed:
     - name: /opt/jdbc-drivers/mysql-connector-java.jar
     - source: {{ salt['pillar.get']('jdbc_connectors:MYSQL') }}
     - makedirs: True
     - mode: 755
     - skip_verify: True

{% elif salt['pillar.get']('jdbc_connectors:ORACLE11') != "" %}

download_custom_oracle_connector_jar:
  file.managed:
     - name: /opt/jdbc-drivers/ojdbc6.jar
     - source: {{ salt['pillar.get']('jdbc_connectors:ORACLE11') }}
     - makedirs: True
     - mode: 755
     - skip_verify: True
{% endif %}

{% elif salt['pillar.get']('jdbc_connectors:ORACLE12') != "" %}

download_custom_oracle_connector_jar:
  file.managed:
     - name: /opt/jdbc-drivers/ojdbc7.jar
     - source: {{ salt['pillar.get']('jdbc_connectors:ORACLE12') }}
     - makedirs: True
     - mode: 755
     - skip_verify: True
{% endif %}§