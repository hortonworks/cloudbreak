 <form role="form" autocomplete="on" name="loginForm">

             <div class="form-group" ng-class="{ 'has-error has-feedback': showLoginEmailError && loginForm.emailFieldLogin.$invalid }">
              <!-- OPTIONAL HTML:
			  class: 'has-feedback' - got validation result AND
			  class: 'has-error' – error OR
			  class: 'has-success' - ok
			  -->
             <label class="sr-only" for="emailFieldLogin">email</label>

              <div class="input-group">
                <span class="input-group-addon"><i class="fa fa-envelope-o fa-fw"></i></span>
                <input class="form-control" type="email" ng-model="emailFieldLogin" ng-blur="showLoginEmailError=true;" id="emailFieldLogin" name="emailFieldLogin" placeholder="email"
                required>
                 <i class="fa fa-warning form-control-feedback" ng-show="showLoginEmailError && loginForm.emailFieldLogin.$invalid"></i>
              </div><!-- .input-group -->
              <span class="help-block" ng-show="showLoginEmailError && loginForm.emailFieldLogin.$invalid">
              {{error_msg.email_invalid}}
              </span>

             </div><!-- .form-group -->
             <div class="form-group" ng-class="{ 'has-error has-feedback': showLoginPasswordError && loginForm.passwFieldLogin.$invalid }">
                    <label class="sr-only" for="passwFieldLogin">jelszó</label>

                <div class="input-group" ng-class="{ 'has-error': showLoginPasswordError && loginForm.passwFieldLogin.$invalid }">
                    <span class="input-group-addon"><i class="fa fa-lock fa-fw"></i></span>
                <input class="form-control" type="password" ng-model="passwFieldLogin" ng-blur="showLoginPasswordError=true;" name="passwFieldLogin" id="passwFieldLogin" placeholder="password" ng-minlength="6" ng-maxlength="200"
                  required>
                <i class="fa fa-warning form-control-feedback" ng-show="showLoginPasswordError && loginForm.passwFieldLogin.$invalid"></i>
             </div>
            <span class="help-block" ng-show="showLoginPasswordError && loginForm.passwFieldLogin.$invalid">
                {{error_msg.pwd_invalid}}
            </span>
            <!-- .input-group -->
            <p class="text-right"><a href="#" id="login-forgot-passw"><i class="fa fa-question-circle fa-fw"></i> forgot my password</a>
            </p>
           </div>
       <a href="#" type="submit" ng-click="signIn()" ng-disabled="loginForm.$invalid" id="login-btn" class="btn btn-info btn-block" role="button">
            <i class="fa fa-sign-in fa-fw"></i> login</a>
       <a href="#" id="forgot-btn" ng-click="forgotPassword()" ng-disabled="loginForm.emailFieldLogin.$invalid" class="btn btn-info btn-block hidden" role="button"><i class='fa fa-paper-plane fa-fw'></i> reset email</a>
       <a href="#" id="login-back-btn" class="btn btn-info btn-block backToSelector" role="button"> back</a>
 </form>