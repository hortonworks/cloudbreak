{% if salt['pillar.get']('hdp:stack:vdf-url') != None %}

{% if 'HDF' in salt['pillar.get']('hdp:stack:repoid') %}

HDF:
  pkgrepo.managed:
    - humanname: {{ salt['pillar.get']('hdp:stack:repoid') }}
    - baseurl: "{{ salt['cmd.run']("cat /tmp/hdf-repo-url.text") }}"
    - gpgcheck: 0
    - enabled: 1
    - path: /

{% else %}

HDP:
  pkgrepo.managed:
    - humanname: {{ salt['pillar.get']('hdp:stack:repoid') }}
    - baseurl: "{{ salt['cmd.run']("cat /tmp/hdp-repo-url.text") }}"
    - gpgcheck: 0
    - enabled: 1
    - path: /

{% endif %}

HDP-UTILS:
  pkgrepo.managed:
    - humanname: {{ salt['pillar.get']('hdp:util:repoid') }}
    - baseurl: "{{ salt['cmd.run']("cat /tmp/hdp-util-repo-url.text") }}"
    - gpgcheck: 0
    - enabled: 1
    - path: /

{% else %}

/etc/yum.repos.d/HDP.repo:
  file.managed:
    - replace: False
    - source: salt://gateway/yum/hdp.repo
    - template: jinja

/etc/yum.repos.d/HDP-UTILS.repo:
  file.managed:
    - replace: False
    - source: salt://gateway/yum/hdp-utils.repo
    - template: jinja

{% endif %}