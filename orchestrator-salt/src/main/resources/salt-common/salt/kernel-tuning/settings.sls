{% set kernel_tuning = {} %}
{% set os = salt['grains.get']('os') %}
{% set os_major_release = salt['grains.get']('osmajorrelease') | int %}
{# Only RHEL 8+ is tuned; CentOS also reports os_family RedHat, hence the check on os. #}
{% set supported = os == 'RedHat' and os_major_release >= 8 %}

{% set min_free_pillar = salt['pillar.get']('kernel-tuning:minFreeKbytes', {}) %}

{% set ram_kb = salt['grains.get']('mem_total') | int * 1024 %}
{% set min_free_target_kb = [min_free_pillar.get('floorKb', 0) | int, (ram_kb * min_free_pillar.get('ratioPercent', 0) / 100) | int] | max %}
{% if min_free_pillar.get('enabled', False) %}
    {% set min_free_value_kb = min_free_target_kb %}
{% else %}
    {% set min_free_value_kb = min_free_pillar.get('restoreKb', 0) | int %}
{% endif %}

{% do kernel_tuning.update({
    "supported": supported,
    "minFreeKbytes": {
        "configured": min_free_pillar | length > 0,
        "enabled": min_free_pillar.get('enabled', False),
        "valueKb": min_free_value_kb
    }
}) %}
