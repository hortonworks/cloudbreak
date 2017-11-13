{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}

# Recipes named "post" are deprecated, but keeping them for backward compatibility
{% if args['post'] is defined %}
{% for script_name in args['post'] %}
/opt/scripts/post/{{ script_name }}:
  file.managed:
     - source:
       - salt://post-recipes/scripts/{{ script_name }}
       - salt://post-recipes/scripts/post-date.sh
     - makedirs: True
     - mode: 755

run_post_script_{{ script_name }}:
  cmd.run:
    - name: sh -x /opt/scripts/post/{{ script_name }} 2>&1 | tee -a /var/log/recipes/post-{{ script_name }}.log && exit ${PIPESTATUS[0]}
    - onlyif:
      - ls /opt/scripts/post/{{ script_name }}
    - unless: ls /var/log/recipes/post-{{ script_name }}.log
{% endfor %}
{% endif %}

{% if args['post-cluster-install'] is defined %}
{% for script_name in args['post-cluster-install'] %}
/opt/scripts/post-cluster-install/{{ script_name }}:
  file.managed:
     - source:
       - salt://post-recipes/scripts/{{ script_name }}
       - salt://post-recipes/scripts/post-date.sh
     - makedirs: True
     - mode: 755

run_post_cluster_install_script_{{ script_name }}:
  cmd.run:
    - name: sh -x /opt/scripts/post-cluster-install/{{ script_name }} 2>&1 | tee -a /var/log/recipes/post-cluster-install-{{ script_name }}.log && exit ${PIPESTATUS[0]}
    - onlyif:
      - ls /opt/scripts/post-cluster-install/{{ script_name }}
    - unless: ls /var/log/recipes/post-cluster-install-{{ script_name }}.log
{% endfor %}
{% endif %}

{% endif %}
{% endfor %}