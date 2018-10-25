import json
import sys

with open('./src/main/resources/definitions/zones.json') as data_file:
    zoneData = json.load(data_file)

with open(sys.argv[1]) as data_file:
    zoneVmTypes = json.load(data_file)

for zone in zoneVmTypes["items"]:
    zoneName = zone["zone"]
    try:
        zone["zone"] = zoneData[zoneName]["zoneid"]
        zone["defaultVmType"] = zoneData[zoneName]["defaultvm"]
    except KeyError:
        print (">>>>> " + zoneName + " is not in zones.json, please add if you need <<<<<")

with open(sys.argv[2], 'w') as outfile:
    json.dump(zoneVmTypes, outfile, indent=2)
