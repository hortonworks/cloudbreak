<form role="form" autocomplete="on" name="signUpForm">

    <div class="form-group" ng-class="{ 'has-error': showSignUpEmailError && signUpForm.signUpEmailField.$invalid }">
        <label class="sr-only" for="signUpEmailField">email</label>
        <div class="input-group">
            <span class="input-group-addon"><i class="fa fa-envelope-o fa-fw"></i></span>
            <input class="form-control" type="email" ng-blur="showSignUpEmailError=true;" ng-model="signUpEmailField" id="signUpEmailField" name="signUpEmailField" placeholder="email" required>
            <i ng-show="showSignUpEmailError && signUpForm.signUpEmailField.$invalid" class="fa fa-warning form-control-feedback"></i>
        </div><!-- .input-group -->
                                                <span class="help-block" ng-show="showSignUpEmailError && signUpForm.signUpEmailField.$invalid">
                                                    {{error_msg.email_invalid}}
                                                </span>
    </div><!-- .form-group -->
    <div class="form-group" ng-class="{ 'has-error': showSingUpPasswordFieldError && signUpForm.signUpPasswField.$invalid }">
        <label class="sr-only" for="signUpPasswField">password</label>
        <div class="input-group">
            <span class="input-group-addon"><i class="fa fa-lock fa-fw"></i></span>
            <input class="form-control" type="password" ng-blur="showSingUpPasswordFieldError=true;" ng-model="signUpPasswField" name="signUpPasswField" id="signUpPasswField" placeholder="password" ng-minlength="6" ng-maxlength="200" required>
            <i class="fa fa-warning form-control-feedback" ng-show="showSingUpPasswordFieldError && signUpForm.signUpPasswField.$invalid"></i>
        </div><!-- .input-group -->
                                                <span class="help-block" ng-show="showSingUpPasswordFieldError && signUpForm.signUpPasswField.$invalid">
                                                    {{error_msg.pwd_invalid}}
                                                </span>
    </div><!-- .form-group -->
    <div class="form-group" ng-class="{ 'has-error': signUpForm.signUpPassw2Field.$dirty && signUpForm.signUpPassw2Field.$invalid }">
        <label class="sr-only" for="signUpPassw2Field">repeat password</label>
        <div class="input-group">
            <span class="input-group-addon"><i class="fa fa-lock fa-fw"></i></span>
            <input class="form-control" type="password" ng-model="signUpPassw2Field" name="signUpPassw2Field" id="signUpPassw2Field" placeholder="repeat password" match="signUpPasswField" required>
            <i class="fa fa-warning form-control-feedback" ng-show="signUpForm.signUpPassw2Field.$dirty && signUpForm.signUpPassw2Field.$invalid"></i>
        </div>
                                                <span class="help-block" ng-show="signUpForm.signUpPassw2Field.$dirty && signUpForm.signUpPassw2Field.$invalid">
                                                    {{error_msg.pwd_repeat}}
                                                </span>
        <!-- .input-group -->
    </div><!-- .form-group -->
    <div class="form-group" ng-class="{ 'has-error': showSingUpFirstNameError && signUpForm.signUpFirstNameField.$invalid }">
        <label class="sr-only" for="signUpFirstNameField">name</label>
        <div class="input-group">
            <span class="input-group-addon"><i class="fa fa-male fa-fw"></i></span>
            <input class="form-control" type="text" ng-blur="showSingUpFirstNameError=true;" ng-model="signUpFirstNameField" name="signUpFirstNameField" id="signUpFirstNameField" placeholder="first name" required>
            <i class="fa fa-warning form-control-feedback" ng-show="showSingUpFirstNameError && signUpForm.signUpFirstNameField.$invalid"></i>
        </div><!-- .input-group -->
                                                <span class="help-block" ng-show="showSingUpFirstNameError && signUpForm.signUpFirstNameField.$invalid">
                                                    {{error_msg.first_name_empty}}
                                                </span>
    </div><!-- .form-group -->
    <div class="form-group" ng-class="{ 'has-error': showSingUpLastNameError && signUpForm.signUpLastNameField.$invalid }">
        <label class="sr-only" for="signUpLastNameField">name</label>
        <div class="input-group">
            <span class="input-group-addon"><i class="fa fa-male fa-fw"></i></span>
            <input class="form-control" type="text" ng-blur="showSingUpLastNameError=true;" ng-model="signUpLastNameField" name="signUpLastNameField" id="signUpLastNameField" placeholder="last name" required>
            <i class="fa fa-warning form-control-feedback" ng-show="showSingUpLastNameError && signUpForm.signUpLastNameField.$invalid"></i>
        </div><!-- .input-group -->
                                                <span class="help-block" ng-show="showSingUpLastNameError && signUpForm.signUpLastNameField.$invalid">
                                                    {{error_msg.last_name_empty}}
                                                </span>
    </div><!-- .form-group -->
    <div class="form-group" ng-class="{ 'has-error': showSingUpCompanyError && signUpForm.signUpCompanyField.$invalid }">
        <label class="sr-only" for="signUpCompanyField">company</label>
        <div class="input-group">
            <span class="input-group-addon"><i class="fa fa-institution fa-fw"></i></span>
            <input class="form-control" type="text" ng-blur="showSingUpCompanyError=true;" ng-model="signUpCompanyField" name="signUpCompanyField"  id="signUpCompanyField" placeholder="company" required>
            <i class="fa fa-warning form-control-feedback" ng-show="showSingUpCompanyError && signUpForm.signUpCompanyField.$invalid"></i>
        </div><!-- .input-group -->
                                                <span class="help-block" ng-show="showSingUpCompanyError && signUpForm.signUpCompanyField.$invalid">
                                                    {{error_msg.company_empty}}
                                                </span>
    </div><!-- .form-group -->

    <a href="#" id="signup-btn" ng-click="signUp()" ng-disabled="signUpForm.$invalid" class="btn btn-info btn-block" role="button"><i class="fa fa-user fa-fw"></i> sign up</a>
    <a href="#" id="signup-back-btn" class="btn btn-info btn-block backToSelector" role="button"> back</a>

</form>
