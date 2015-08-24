package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.controller.json.AccountPreferencesJson;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Api(value = "/accountpreferences", description = ControllerDescription.ACCOUNT_PREFERENCES_DESCRIPTION)
public class AccountPreferencesController {

    @Inject
    private AccountPreferencesService service;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @ApiOperation(value = OperationDescriptions.AccountPreferencesDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    @RequestMapping(value = "accountpreferences", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<AccountPreferencesJson> getAccountPreferencesForUser(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildUserMdcContext(user);
        AccountPreferences preferences = service.getOneByAccount(user);
        return new ResponseEntity<>(convert(preferences), HttpStatus.OK);
    }

    @ApiOperation(value = OperationDescriptions.AccountPreferencesDescription.PUT_PRIVATE, produces = ContentType.JSON, notes = Notes.ACCOUNT_PREFERENCES_NOTES)
    @RequestMapping(value = "accountpreferences", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateAccountPreferences(@ModelAttribute("user") CbUser user, @Valid @RequestBody AccountPreferencesJson updateRequest) {
        MDCBuilder.buildUserMdcContext(user);
        service.saveOne(user, convert(updateRequest));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private AccountPreferencesJson convert(AccountPreferences preferences) {
        return conversionService.convert(preferences, AccountPreferencesJson.class);
    }

    private AccountPreferences convert(AccountPreferencesJson preferences) {
        return conversionService.convert(preferences, AccountPreferences.class);
    }
}
