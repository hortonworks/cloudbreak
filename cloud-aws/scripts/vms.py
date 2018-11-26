import json
import sys
import re

with open(sys.argv[1]) as terms_data_file:
    terms = json.load(terms_data_file)

with open(sys.argv[2]) as vms_data_file:
    vms = json.load(vms_data_file)

with open(sys.argv[3]) as encFlags_data_file:
    encryptionSupportedTypes = json.load(encFlags_data_file)

vmResults = {}
vmResults["items"] = []

vmMap = {}

to_float = lambda s : float(s.replace(',', ''))
to_int = lambda s : int(str(s).replace(',', ''))

for vm in vms["items"]:
    vmMap[vm["sku"]] = vm

for term in terms["items"]:

    if not vmMap.has_key(term["sku"]):
        continue

    vm = vmMap[term["sku"]]

    vmAttributes = vm["attributes"]
    if (vmAttributes["location"] != "US West (N. California)" and vmAttributes["location"] != "US East (N. Virginia)"):
        continue

    sku = vm["sku"]

    item = {}
    item["value"] = vmAttributes["instanceType"]
    item["meta"]= {}
    item["meta"]["properties"] = {}
    item["meta"]["properties"]["Memory"] = to_float(vmAttributes["memory"].split(' ')[0])
    item["meta"]["properties"]["Cpu"] = vmAttributes["vcpu"]

    priceDimensions = term["priceDimensions"]
    priceDimension = next(iter(priceDimensions))

    if "per On Demand Linux " + vmAttributes["instanceType"] not in priceDimensions[priceDimension]["description"]:
        continue

    item["meta"]["properties"]["Price"] = priceDimensions[priceDimension]["pricePerUnit"]["USD"]

    encryptionSupported = False
    for encryptionSupportedType in encryptionSupportedTypes["encryption_supported_types"]:
        if encryptionSupportedType == vmAttributes["instanceType"] or re.match(encryptionSupportedType, vmAttributes["instanceType"]):
            encryptionSupported = True

    item["meta"]["properties"]["encryption_supported"] = encryptionSupported

    item["meta"]["configs"] = []

    item["meta"]["configs"].append({
       "volumeParameterType": "MAGNETIC",
       "minimumSize": 1,
       "maximumSize": 1024,
       "minimumNumber": 1,
       "maximumNumber": 24
    })

    item["meta"]["configs"].append({
       "volumeParameterType": "SSD",
       "minimumSize": 1,
       "maximumSize": 17592,
       "minimumNumber": 1,
       "maximumNumber": 24
    })

    item["meta"]["configs"].append({
        "volumeParameterType": "ST1",
        "minimumSize": 500,
        "maximumSize": 17592,
        "minimumNumber": 1,
        "maximumNumber": 24
    })

    storage = vmAttributes["storage"]

    if storage == "EBS only":
        minimumSize = 0
        maximumSize = 0
        minimumNumber = 0
        maximumNumber = 0
    else:
        spittedStorage = storage.split(" x ")
        minimumSize = spittedStorage[1].split(" ")[0]
        maximumSize = spittedStorage[1].split(" ")[0]
        minimumNumber = spittedStorage[0]
        maximumNumber = spittedStorage[0]

    item["meta"]["configs"].append({
       "volumeParameterType": "EPHEMERAL",
       "minimumSize": to_int(minimumSize),
       "maximumSize": to_int(maximumSize),
       "minimumNumber": to_int(minimumNumber),
       "maximumNumber": to_int(maximumNumber)
    })

    vmResults["items"].append(item)

with open(sys.argv[4], 'w') as outfile:
    json.dump(vmResults, outfile, indent=2)
