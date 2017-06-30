import json
import subprocess

items = []

with open('./src/main/resources/definitions/azure-zone.json') as data_file:
    regionData = json.load(data_file)

for region in regionData["items"]:
    p = subprocess.Popen(["azure", "vm", "sizes", "--location", region["name"], "--json"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = p.communicate()
    regionVmTypes = json.loads(out)
    dict = {}
    dict["zone"] = region["name"]
    vmTypes = []
    dict["vmTypes"] = vmTypes
    dict["defaultVmType"] = "Standard_A6"
    for vmType in regionVmTypes:
        vmTypes.append(vmType["name"])
    items.append(dict)

outputDict = {}
outputDict["items"] = items

with open("./src/main/resources/definitions/azure-zone-vm.json", 'w') as outfile:
    json.dump(outputDict, outfile, indent=2)
