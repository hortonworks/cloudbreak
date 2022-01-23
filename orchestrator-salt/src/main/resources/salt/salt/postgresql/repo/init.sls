{%- set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}

# Ensure that we remove the unnecessary db package if embedded db is not used, to avoid upgrade issues
cloudera-manager-server-db-2:
  pkg.removed

{% if 'None' == configure_remote_db %}

# In case of Embedded database we just need to upgrade the database to the right version

{% if grains['os_family'] == 'RedHat' %}

/etc/yum.repos.d/pgdg10.repo:
  file.managed:
    - source: salt://postgresql/repo/pgdg10.repo
    - template: jinja

{% elif grains['os_family'] == 'Debian' %}

# No support for Debian

{% elif grains['os_family'] == 'Suse' %}

# No support for Suse

{% endif %}
{% endif %}