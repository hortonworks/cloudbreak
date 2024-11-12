# GCP overrides the hostname each time the network goes up (e.g. startup) and this messes up our hostname confifuration so it should be removed
remove-gcp-NetworkManager-hostname-override:
  file.absent:
    - name: /etc/NetworkManager/dispatcher.d/google_hostname.sh

remove-gcp-dhcp-hostname-override:
  file.absent:
    - name: /etc/dhcp/dhclient.d/google_hostname.sh
