<form role="form" autocomplete="on" name="loginForm">

    <div class="form-group"><!-- OPTIONAL HTML:
										        class: 'has-feedback' - got validation result AND
										        class: 'has-error' – error OR
										        class: 'has-success' - ok
										        -->
        <label class="sr-only" for="emailFieldLogin">email</label>

        <div class="input-group">
            <span class="input-group-addon"><i class="fa fa-envelope-o fa-fw"></i></span>
            <input class="form-control" type="email" ng-model="emailFieldLogin" id="emailFieldLogin" name="emailFieldLogin" placeholder="email"
                   required>

        </div>

    </div><!-- .form-group -->
    <div class="form-group">
        <label class="sr-only" for="passwFieldLogin">jelszó</label>

        <div class="input-group">
            <span class="input-group-addon"><i class="fa fa-lock fa-fw"></i></span>
            <input class="form-control" type="password" ng-model="passwFieldLogin" id="passwFieldLogin" placeholder="password"
                   required>
        </div>
        <!-- .input-group -->
        <p class="text-right"><a href="#" id="login-forgot-passw"><i class="fa fa-question-circle fa-fw"></i> forgot my password</a>
        </p>
    </div>
    <a href="#" type="submit" ng-click="signIn()" ng-disabled="loginForm.$invalid" id="login-btn" class="btn btn-info btn-block" role="button">
        <i class="fa fa-sign-in fa-fw"></i> login</a>
    <a href="#" id="forgot-btn" ng-click="forgotPassword()" ng-disabled="loginForm.emailFieldLogin.$invalid" class="btn btn-info btn-block hidden" role="button"><i class='fa fa-paper-plane fa-fw'></i> reset email</a>
    <a href="#" id="login-back-btn" class="btn btn-info btn-block backToSelector" role="button"> back</a>
</form>