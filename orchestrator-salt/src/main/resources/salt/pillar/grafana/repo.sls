grafana:
  repo:
{% if grains['os_family'] == 'RedHat' %}
    baseUrl: https://packagecloud.io/grafana/stable/el/7/$basearch
    gpgKeyUrl: https://grafanarel.s3.amazonaws.com/RPM-GPG-KEY-grafana
{% elif grains['os_family'] == 'Debian' %}
    baseUrl: https://packagecloud.io/grafana/stable/{{ grains['os'] | lower }}/
    gpgKeyUrl: https://packagecloud.io/gpg.key
{% endif %}
