/**
 * encapsulating all JS code within a document.ready(function) ensures the code does not execute until
 * the page has loaded.
 */
$(document).ready(function() {
    /**
     * Validation is provided by attaching an event handler to the email input field in the form. The function accepts
     * a boolean value; if true the username already exists and the field with id = 'email' will be decorated with
     * error with an appropriate message. If false, the email is checked for validity.
     * @param data
     */
    var successFn = function(data){
       var $el = $('#email');
        if(data){
            var msg = "User exists! -- Please try logging in";
                decorateError(msg,$el);
        } else {
            if(emailIsValid($el.val())){
                decorateOK($el);
            } else {
                var msg = "invalid email, try again.";
                decorateError(msg,$el);
            }
        }
    }

    // This function is executed if there was a problem on the server and the JsRouter could not complete async request.
    var errorFn = function(err){
        var msg = "Could not complete AJAX request";
        decorateError(msg,$('#email'));
    }

    var ajax1 = {
        success: successFn,
        error: errorFn
    }

    /**
     * an AJAX call using Play's javascriptRouter to check username (email) is available. The function is attached to
     * the field with id = 'email'. If the AJAX call was successful then the value returned by the router is a JSON boolean; this value is then
     * placed into ajax1 success function, if unsuccessful (there was a problem on the server) then the ajax1 error function is executed.
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



