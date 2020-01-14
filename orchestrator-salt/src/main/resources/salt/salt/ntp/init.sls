{%- from 'ntp/settings.sls' import ntp with context %}

{% if salt['file.file_exists' ]('/etc/chrony.conf') and ntp.ntp_config_line|length > 0 %}

# Restart chronyd if its configuration file changes, but only if it was actually
# running from the get go.
chronyd:
  service.running:
    - watch:
      - file: /etc/chrony.conf
    - onlyif:
      - service chronyd status

# Add the cloud provider's NTP server to chronyd's configuration. We'll still
# consider whatever servers chronyd was configured with out of the box; this
# just gives preference to the cloud native one.
add_cloud_platform_time_server_to_chrony:
  file.append:
    - name: /etc/chrony.conf
    - text: |

        {{ ntp.ntp_config_line }}

{% endif %}
