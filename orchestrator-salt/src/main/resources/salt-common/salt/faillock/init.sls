{% if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int >= 8 %}
enable-faillock:
  cmd.run:
    - name: authselect enable-feature with-faillock
    - unless: grep -q "with-faillock" /etc/authselect/authselect.conf
{% endif %}