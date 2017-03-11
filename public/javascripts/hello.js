if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}

/**
 * encapsulating all JS code within a document.ready(function) ensures the code does not execute until
 * the page has loaded.
 */
$(document).ready(function() {

// validation is provided by attaching an event handler to the input fields in the form.


    var successFn = function(data){
       var $el = $('#email');
        if(data){
            var msg = "User exists! -- Please try logging in";
                decorateError(msg,$el);
        } else{
            if(emailIsValid($el.val())){
                decorateOK($el);
            } else {
                var msg = "invalid email, try again.";
                decorateError(msg,$el);
            }
        }
    }

    var errorFn = function(err){
        var msg = "Could not complete AJAX request";
        decorateError(msg,$('#email'));
    }

    ajax1 = {
        success: successFn,
        error: errorFn
    }

    /**
     * an AJAX call using Play's javascriptRouter to check username (email) is available. Since this is AJAX,
     * the call is async so the validation check is a little more involved than the simple boolean checks
     * used elsewhere.
     */

    $('#email').blur(function(){
        var data = {};
        data = this.value;
        jsRoutes.controllers.Application.userExists(data).ajax(ajax1);
    });


    /**
     *  the password entered must be valid. Once the user leaves the password inputBox a function fires
     *  to check the value is valid. If it is valid decorate the field with Bootstrap 'OK', if not then decorate
     *  with Bootstrap 'remove'.
     */

    $('#password').on('blur', function(e){
        var msg = 'Password must be 8+ chars incl. [a-z] + [A-Z]';
        if(passwordIsValid(this.value)){
            decorateOK($(this));
        } else {
            decorateError(msg,$(this));
        }
    });


    //the re-entered password must match the password.


    $('#confirmPswd').on('blur', function(e){
        var msg = 'Passwords do not match!';
        if(passwordsMatch($(this).val(), $('#password').val())){
            decorateOK($(this));
        } else {
            decorateError(msg,$(this));
        }
    });



    /**
     * decorate a control-group input using Bootstrap CSS. This styling is designed to be used with the
     * fieldConstructorTemplate.
     */

    /**
     * Error - decorate the fields with Bootstrap's 'remove' style
     * @param msg
     * @param $el
     */
    function decorateError(msg, $el){
        $el.parent().next().text(msg)
            .addClass('alert alert-danger');
        $el.parent().parent().addClass('has-error has-feedback');
        $el.next().addClass('glyphicon glyphicon-remove form-control-feedback');
    }

    /**
     * valid input - decorate the fields with Bootstrap's 'OK' style
     * @param $el
     */

    function decorateOK($el){
        $el.parent().next().text('')
            .removeClass('alert alert-danger');
        $el.parent().parent().removeClass('has-error has-feedback')
            .addClass('has-success has-feedback');
        $el.next().removeClass('glyphicon glyphicon-remove form-control-feedback')
            .addClass('glyphicon glyphicon-ok form-control-feedback');
    }



    //the validation functions each returning a boolean.

    function emailIsValid(email) {
        var re = /\S+@\S+\.\S+/;
        return re.test(email);
    }

    function passwordIsValid(pswd){
        return (/[A-Z]/.test(pswd) && /[a-z]/.test(pswd) && pswd.length>7);
    }

    function passwordsMatch(p1,p2){
        return (p1===p2);
    }
});



