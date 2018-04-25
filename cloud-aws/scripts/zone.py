import json
import sys

with open('./src/main/resources/definitions/zones.json') as data_file:
    zoneData = json.load(data_file)

with open(sys.argv[1]) as data_file:
    zoneVmTypes = json.load(data_file)

#Filter zones that are supported by Cloudbreak
zoneVmTypes["items"] = [zone for zone in zoneVmTypes["items"] if zone["zone"] in zoneData]

for zone in zoneVmTypes["items"]:
    zoneName = zone["zone"]
    print zoneName
    zone["zone"] = zoneData[zoneName]["zoneid"]
    defaultVm = zoneData[zoneName]["defaultvm"]
    if defaultVm in zone["vmTypes"]:
        zone["defaultVmType"] = defaultVm
    else:
        print "Default VM type '%s' doesn't exist as available VM type in zone '%s'." % (defaultVm, zoneName)
        sys.exit(1)

with open(sys.argv[2], 'w') as outfile:
    json.dump(zoneVmTypes, outfile, indent=2)
