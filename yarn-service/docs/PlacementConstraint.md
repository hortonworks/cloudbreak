
# PlacementConstraint

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **String** | An optional name associated to this constraint. |  [optional]
**type** | [**PlacementType**](PlacementType.md) | The type of placement. | 
**scope** | [**PlacementScope**](PlacementScope.md) | The scope of placement. | 
**targetTags** | **List&lt;String&gt;** | The name of the components that this component&#39;s placement policy is depending upon are added as target tags. So for affinity say, this component&#39;s containers are requesting to be placed on hosts where containers of the target tag component(s) are running on. Target tags can also contain the name of this component, in which case it implies that for anti-affinity say, no more than one container of this component can be placed on a host. Similarly, for cardinality, it would mean that containers of this component is requesting to be placed on hosts where at least minCardinality but no more than maxCardinality containers of the target tag component(s) are running. |  [optional]
**nodeAttributes** | [**Map&lt;String, List&lt;String&gt;&gt;**](List.md) | Node attributes are a set of key:value(s) pairs associated with nodes. |  [optional]
**nodePartitions** | **List&lt;String&gt;** | Node partitions where the containers of this component can run. |  [optional]
**minCardinality** | **Long** | When placement type is cardinality, the minimum number of containers of the depending component that a host should have, where containers of this component can be allocated on. |  [optional]
**maxCardinality** | **Long** | When placement type is cardinality, the maximum number of containers of the depending component that a host should have, where containers of this component can be allocated on. |  [optional]



