{#- vm.min_free_kbytes is derived from the node's own memory (see settings.sls), the policy comes from the kernel-tuning pillar.
    Our file sorts first in /etc/sysctl.d, so any other config setting the key wins, both at boot and when applied below.
    Only this key is applied, and only when the live value differs from the effective one (the last definition in
    systemd-sysctl's config order), so a failed apply is retried on the next run.
    Disabling writes the restore value into our file on nodes we already tuned; nodes never tuned are left alone. -#}
{%- from 'kernel-tuning/settings.sls' import kernel_tuning with context %}
{%- set min_free_conf = '/etc/sysctl.d/00-00-cdp-min-free-kbytes.conf' %}
{%- if kernel_tuning.supported and kernel_tuning.minFreeKbytes.configured and kernel_tuning.minFreeKbytes.valueKb > 0
      and (kernel_tuning.minFreeKbytes.enabled or salt['file.file_exists'](min_free_conf)) %}
{{ min_free_conf }}:
  file.managed:
    - user: root
    - group: root
    - mode: 644
    - contents: |
        # Managed by Cloudbreak (salt-common kernel-tuning state). Sorted first on purpose:
        # any other sysctl config that sets vm.min_free_kbytes overrides this value.
        vm.min_free_kbytes = {{ kernel_tuning.minFreeKbytes.valueKb }}

apply_min_free_kbytes:
  cmd.run:
    - name: /usr/lib/systemd/systemd-sysctl --prefix=vm.min_free_kbytes
    - unless: test "$(sysctl -n vm.min_free_kbytes)" -eq "$(/usr/lib/systemd/systemd-sysctl --cat-config | sed -n 's/^[[:space:]]*-\{0,1\}vm[.]min_free_kbytes[[:space:]]*=[[:space:]]*//p' | tail -1)"
    - require:
      - file: {{ min_free_conf }}
{%- endif %}
