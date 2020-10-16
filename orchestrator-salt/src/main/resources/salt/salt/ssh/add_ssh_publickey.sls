
{{ salt['user.info'](salt['pillar.get']('tmpssh:user')).home }}/.ssh/authorized_keys:
  file.append:
    - text:
      - '# {{ salt['pillar.get']('tmpssh:comment') }}'
      - '{{ salt['pillar.get']('tmpssh:publickey') }}'
      - '# end of {{ salt['pillar.get']('tmpssh:comment') }}'
