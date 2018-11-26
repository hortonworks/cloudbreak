generate-zone-vms:
	mkdir -p ./build
	curl -L -o ./build/pricing.json https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonEC2/current/index.json


	cat ./build/pricing.json | jq '.products | .[] | select(.productFamily!=null and .productFamily=="Compute Instance" or .productFamily == "Compute Instance (bare metal)") | .attributes | {location, instanceType} | select(.location!=null and .instanceType !=null)' | jq --slurp '. | group_by(.location) | .[] | {"zone":(.[0].location), "vmTypes": map(.instanceType)|sort|unique}' | jq --slurp '{ "items": .}' > ./build/tmp-zones.json
	python scripts/zone.py ./build/tmp-zones.json ./src/main/resources/definitions/aws-zone-vm.json

	cat ./build/pricing.json | jq '.terms | .OnDemand | .[] | .[] | {sku,priceDimensions}' | jq --slurp '{ "items": .}' > ./build/tmp-terms.json

	cat ./build/pricing.json | jq '.products | .[] | select(.productFamily!=null and .productFamily=="Compute Instance" or .productFamily == "Compute Instance (bare metal)") | {sku, attributes} | select(.attributes.location!=null and .attributes.instanceType !=null)' | jq --slurp '{ "items": .}' > ./build/tmp-vms.json
	python scripts/vms.py ./build/tmp-terms.json ./build/tmp-vms.json ./src/main/resources/definitions/aws-encryption-support.json ./src/main/resources/definitions/aws-vm.json
