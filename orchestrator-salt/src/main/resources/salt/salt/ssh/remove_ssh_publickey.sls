
{{ salt['user.info'](salt['pillar.get']('tmpssh:user')).home }}/.ssh/authorized_keys:
  file.blockreplace:
    - marker_start: "# {{ salt['pillar.get']('tmpssh:comment') }}"
    - marker_end: "# end of {{ salt['pillar.get']('tmpssh:comment') }}"
    - append_if_not_found: True
    - append_newline: False

remove_leading_comment:
  file.line:
    - name: {{ salt['user.info'](salt['pillar.get']('tmpssh:user')).home }}/.ssh/authorized_keys
    - match: "# {{ salt['pillar.get']('tmpssh:comment') }}"
    - mode: delete

remove_trailing_comment:
  file.line:
    - name: {{ salt['user.info'](salt['pillar.get']('tmpssh:user')).home }}/.ssh/authorized_keys
    - match: "# end of {{ salt['pillar.get']('tmpssh:comment') }}"
    - mode: delete
