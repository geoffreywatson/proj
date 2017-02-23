if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}

//encapsulating all JS code within a document.ready(function) ensures the code does not execute until
//the page has loaded.

$(document).ready(function() {

// validation is provided by attaching an event handler to the input fields in the form.

    //all input fields of type email will execute validation code once the field loses focus.
    $('#email').on('blur', {msg: "Please enter a valid email"}, function (e) {
        validateInput(emailIsValid(this.value), e, $(this));
    });

    //here, the field with the designated id will execute the given validation code once the field loses focus.
    $('#password').on('blur',{msg:"Password must: 8+ chars incl. [a-z] + [A-Z]"},function(e){
        validateInput(passwordIsValid(this.value),e,$(this));
    });

    //the re-entered password must match the password.
    $('#confirmPswd').on('blur',{msg:"Passwords do not match!"}, function(e){
        validateInput(passwordsMatch($(this).val(),$('#password').val()),e,$(this));
    });


    /**
     * decorate a control-group input using Bootstrap CSS. This styling is designed to be used with the
     * fieldConstructorTemplate.
     */


    function validateInput(func, e, $el) {
        if (!func) {
            $el.parent().next().text(e.data.msg)
                .addClass('alert alert-danger');
            $el.parent().parent().addClass('has-error has-feedback');
            $el.next().addClass('glyphicon glyphicon-remove form-control-feedback');
        } else {
            $el.parent().next().text('')
                .removeClass('alert alert-danger');
            $el.parent().parent().removeClass('has-error has-feedback')
                .addClass('has-success has-feedback');
            $el.next().removeClass('glyphicon glyphicon-remove form-control-feedback')
                .addClass('glyphicon glyphicon-ok form-control-feedback');
        }
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



