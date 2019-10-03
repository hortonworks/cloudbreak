# TODO: For Azure, there's no link-local NTP service. Instead, we need to make
# sure the Linux Integration Services are installed.
# https://docs.microsoft.com/en-us/azure/virtual-machines/linux/time-sync#host-only

# https://aws.amazon.com/blogs/aws/keeping-time-with-amazon-time-sync-service/
{% if salt['pillar.get']('platform') == 'AWS' %}
    {% set ntp_server = '169.254.169.123' %}
# https://cloud.google.com/compute/docs/instances/managing-instances#configure_ntp_for_your_instances
{% elif salt['pillar.get']('platform') == 'GCP' %}
    {% set ntp_server = 'metadata.google.internal' %}
{% else %}
    {% set ntp_server = '' %}
{% endif %}

{% set ntp = {} %}
{% do ntp.update({
    'ntp_server': ntp_server
}) %}

