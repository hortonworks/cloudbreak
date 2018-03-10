smartsense-hst:
  pkg.installed: []

{% if grains['os_family'] == 'RedHat' %}
# TODO: updata SmartSense .deb package

update-smartsense:
  cmd.run:
    - name: yum -q -y update http://s3.amazonaws.com/dev.hortonworks.com/hst/centos7/smartsense-hst-1.4.1.2.5.0.1-1817.x86_64.rpm
    - unless: test -f /usr/hdp/share/hst/ambari-service/SMARTSENSE/configuration/product-info.xml

{% endif %}

upgrade-smartsense-ambari-service:
    file.copy:
    - name:  /var/lib/ambari-server/resources/stacks/HDP/2.1/services/SMARTSENSE
    - force: True
    - mode: 755
    - source: /usr/hdp/share/hst/ambari-service/SMARTSENSE
    - unless: test -f /var/lib/ambari-server/resources/stacks/HDP/2.1/services/SMARTSENSE/configuration/product-info.xml

/etc/hst/conf/hst-gateway.ini:
  file.managed:
    - makedirs: True
    - source: salt://smartsense/gateway/hst-gateway.ini
    - mode: 755
    - template: jinja
    - context:
        smartsense_upload: {{ salt['pillar.get']('smartsense_upload') }}
    - watch:
      - pkg: smartsense-hst

{% if grains['os_family'] == 'RedHat' %}
# TODO: disable SmartSense services on Ubuntu/Debian

disable-hst-gateway:
  cmd.run:
    - name: chkconfig hst-gateway off

enable-hst:
  cmd.run:
    - name: chkconfig hst off

{% endif %}