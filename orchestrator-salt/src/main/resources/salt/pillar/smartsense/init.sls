smartsense_update_pkg:
{% if grains['os_family'] == 'RedHat' %}
  http://s3.amazonaws.com/dev.hortonworks.com/hst/centos7/smartsense-hst-1.4.1.2.5.0.1-1817.x86_64.rpm
{% elif grains['os'] == 'Debian' %}
  http://s3.amazonaws.com/dev.hortonworks.com/hst/debian7/smartsense-hst_1.4.1.2.5.0.1-1817.deb
{% elif grains['os'] == 'Ubuntu' %}
  http://s3.amazonaws.com/dev.hortonworks.com/hst/ubuntu14/smartsense-hst_1.4.1.2.5.0.1-1817.deb
{% endif %}