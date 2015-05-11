package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.domain.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.AzureNetwork;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.GcpNetwork;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.OpenStackNetwork;
import com.sequenceiq.cloudbreak.service.network.DefaultNetworkCreator;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/networks", description = ControllerDescription.NETWORK_DESCRIPTION, position = 8)
public class NetworkController {

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private NetworkService networkService;


    @Autowired
    private DefaultNetworkCreator networkCreator;

    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    @RequestMapping(value = "user/networks", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createPrivateNetwork(@ModelAttribute("user") CbUser user, @RequestBody @Valid NetworkJson networkJson) {
        return createNetwork(user, networkJson, false);
    }

    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    @RequestMapping(value = "account/networks", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountNetwork(@ModelAttribute("user") CbUser user, @RequestBody @Valid NetworkJson networkJson) {
        return createNetwork(user, networkJson, true);
    }

    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    @RequestMapping(value = "user/networks", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<NetworkJson>> getPrivateNetworks(@ModelAttribute("user") CbUser user) {
        Set<Network> networks = networkCreator.createDefaultNetworks(user);
        networks.addAll(networkService.retrievePrivateNetworks(user));
        return new ResponseEntity<>(convert(networks), HttpStatus.OK);
    }

    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    @RequestMapping(value = "account/networks", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<NetworkJson>> getAccountNetworks(@ModelAttribute("user") CbUser user) {
        Set<Network> networks = networkCreator.createDefaultNetworks(user);
        networks.addAll(networkService.retrieveAccountNetworks(user));
        return new ResponseEntity<>(convert(networks), HttpStatus.OK);
    }

    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    @RequestMapping(value = "networks/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<NetworkJson> getNetwork(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        Network network = networkService.getById(id);
        return new ResponseEntity<>(convert(network), HttpStatus.OK);
    }

    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    @RequestMapping(value = "user/networks/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<NetworkJson> getNetworkInPrivate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Network network = networkService.getPrivateNetwork(name, user);
        return new ResponseEntity<>(convert(network), HttpStatus.OK);
    }

    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    @RequestMapping(value = "account/networks/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<NetworkJson> getNetworkInAccount(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Network network = networkService.getPublicNetwork(name, user);
        return new ResponseEntity<>(convert(network), HttpStatus.OK);
    }

    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    @RequestMapping(value = "networks/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<NetworkJson> deleteNetworkById(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        networkService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    @RequestMapping(value = "account/networks/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<NetworkJson> deletePublicNetwork(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        networkService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = OperationDescriptions.NetworkOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.NETWORK_NOTES)
    @RequestMapping(value = "user/networks/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<NetworkJson> deletePrivateNetwork(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        networkService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<IdJson> createNetwork(CbUser user, NetworkJson networkRequest, boolean publicInAccount) {
        Network network = convert(networkRequest, publicInAccount);
        network = networkService.create(user, network);
        return new ResponseEntity<>(new IdJson(network.getId()), HttpStatus.CREATED);
    }

    private Network convert(NetworkJson networkRequest, boolean publicInAccount) {
        Network converted = null;
        switch (networkRequest.getCloudPlatform()) {
            case AWS:
                converted = conversionService.convert(networkRequest, AwsNetwork.class);
                break;
            case AZURE:
                converted = conversionService.convert(networkRequest, AzureNetwork.class);
                break;
            case GCC:
                converted = conversionService.convert(networkRequest, GcpNetwork.class);
                break;
            case OPENSTACK:
                converted = conversionService.convert(networkRequest, OpenStackNetwork.class);
                break;
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", networkRequest.getCloudPlatform()));
        }
        converted.setPublicInAccount(publicInAccount);
        return converted;
    }

    private NetworkJson convert(Network network) {
        return conversionService.convert(network, NetworkJson.class);
    }

    private Set<NetworkJson> convert(Set<Network> networks) {
        Set<NetworkJson> jsons = new HashSet<>();
        for (Network network : networks) {
            jsons.add(convert(network));
        }
        return jsons;
    }
}
