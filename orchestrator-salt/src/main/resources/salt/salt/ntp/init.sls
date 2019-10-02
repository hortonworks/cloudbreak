{% if salt['file.file_exists' ]('/etc/chrony.conf') %}

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
{% if salt['pillar.get']('platform') == 'AWS' %}
# https://aws.amazon.com/blogs/aws/keeping-time-with-amazon-time-sync-service/
    - text: |

        server 169.254.169.123 prefer iburst

{% elif salt['pillar.get']('platform') == 'GCP' %}
# https://cloud.google.com/compute/docs/instances/managing-instances#configure_ntp_for_your_instances
    - text: |

        server metadata.google.internal prefer iburst
{% elif salt['pillar.get']('platform') == 'Azure' %}
# TODO: For Azure, there's no link-local NTP service. Instead, we need to make
# sure the Linux Integration Services are installed.
#
# https://docs.microsoft.com/en-us/azure/virtual-machines/linux/time-sync#host-only

{% endif %}

{% endif %}
