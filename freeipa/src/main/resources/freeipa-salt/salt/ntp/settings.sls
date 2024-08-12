# TODO: For Azure, there's no link-local NTP service. Instead, we need to make
# sure the Linux Integration Services are installed.
# https://docs.microsoft.com/en-us/azure/virtual-machines/linux/time-sync#host-only

# https://aws.amazon.com/blogs/aws/keeping-time-with-amazon-time-sync-service/
{% if salt['pillar.get']('platform') == 'AWS' %}
    {% set ntp_config_line_replace = 'server 169.254.169.123 .*' %}
    {% set ntp_config_line = 'server 169.254.169.123 prefer iburst trust'  %}
# https://cloud.google.com/compute/docs/instances/managing-instances#configure_ntp_for_your_instances
{% elif salt['pillar.get']('platform') == 'GCP' %}
    {% set ntp_config_line_replace = 'server metadata.google.internal .*' %}
    {% set ntp_config_line = 'server metadata.google.internal prefer iburst trust'  %}
{% elif salt['pillar.get']('platform') == 'AZURE' %}
    {% set ntp_config_line_replace = 'refclock PHC /dev/ptp.*' %}
    {% if salt['file.is_link']('/dev/ptp_hyperv') %}
      {% set ntp_config_line = 'refclock PHC /dev/ptp_hyperv poll 3 dpoll -2 offset 0 trust' %}
    {% else %}
      {% set ntp_config_line = 'refclock PHC /dev/ptp0 poll 3 dpoll -2 offset 0 trust' %}
    {% endif %}
{% else %}
    {% set ntp_config_line = '' %}
{% endif %}

{% set ntp = {} %}
{% do ntp.update({
    'ntp_config_line_replace': ntp_config_line_replace,
    'ntp_config_line': ntp_config_line
}) %}

