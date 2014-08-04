<form role="form" autocomplete="on" name="resetPasswordForm">

    <div class="form-group" ng-class="{ 'has-error': showResetPasswordError && resetPasswordForm.resetPasswField.$invalid }">
        <label class="sr-only" for="resetPasswField">new password</label>
        <div class="input-group">
            <span class="input-group-addon"><i class="fa fa-lock fa-fw"></i></span>
            <input class="form-control" ng-model="resetPasswField" ng-blur="showResetPasswordError=true;" type="password" id="resetPasswField" name="resetPasswField" placeholder="new password" required ng-minlength="6" ng-maxlength="200">
            <i class="fa fa-warning form-control-feedback" ng-show="showResetPasswordError && resetPasswordForm.resetPasswField.$invalid"></i>
        </div><!-- .input-group -->
                                                <span class="help-block" ng-show="showResetPasswordError && resetPasswordForm.resetPasswField.$invalid">
                                                    {{error_msg.pwd_invalid}}
                                                </span>
    </div><!-- .form-group -->
    <div class="form-group" ng-class="{ 'has-error': resetPasswordForm.resetPassw2Field.$dirty && resetPasswordForm.resetPassw2Field.$invalid }">
        <label class="sr-only" for="resetPassw2Field">repeat password</label>
        <div class="input-group">
            <span class="input-group-addon"><i class="fa fa-lock fa-fw"></i></span>
            <input class="form-control" ng-model="resetPassw2Field" type="password" id="resetPassw2Field" name="resetPassw2Field" placeholder="repeat password" match="resetPasswField" required>
            <i class="fa fa-warning form-control-feedback" ng-show="resetPasswordForm.resetPassw2Field.$dirty && resetPasswordForm.resetPassw2Field.$invalid"></i>
        </div><!-- .input-group -->
                                                <span class="help-block" ng-show="resetPasswordForm.resetPassw2Field.$dirty && resetPasswordForm.resetPassw2Field.$invalid">
                                                    {{error_msg.pwd_repeat}}
                                                </span>
    </div><!-- .form-group -->

    <a href="#" id="setpassw-btn" ng-click="resetPassword()" ng-disabled="resetPasswordForm.$invalid" class="btn btn-info btn-block" role="button"><i class="fa fa-lock fa-fw"></i> set new password</a>

</form>