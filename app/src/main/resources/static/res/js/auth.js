var expiresValue = 7 * 86400;

var isLogin = true;
var login_pwd = "";
var register_nick = "";
$(document).ready(function () {
    // 处理认证dialog关闭
    $('.dialog_anth .close').bind("click", function (event) {
        clearAnth();
        $('.dialog_anth').hide();
    });
    // 弹出登录dialog
    $('#login').bind("click", function (event) {
        if (!isLogin) {
            toLogin();
        }
        isLogin = true;
        $('.dialog_anth').show();
    });
    // 弹出注册dialog
    $('#register').bind("click", function (event) {
        if (isLogin) {
            toRegister();
        }
        isLogin = false;
        $('.dialog_anth').show();
    });
    // 切换登录or注册
    $('#switch').bind("click", function (event) {
        isLogin ? toRegister() : toLogin();
        isLogin = !isLogin;
        return false;
    });
    // 忘记密码
    $('#forget').bind("click", function (event) {
        alert("请联系管理...");
    });
    // 注册or登录
    $('#complete').bind("click", function (event) {
        isLogin ? login() : register();
        return false;
    });
    // 用户菜单
    $('#user').mouseover(function () {
        $("#user-menu").stop().fadeIn(300);
    }).mouseout(function () {
        $("#user-menu").stop().fadeOut(300);
    });
    $('#offline').bind("click", function (event) {
        offline();
    });
});

// 转为注册dialog
function toRegister() {
    $('.dialog_anth .content').css({
        "height": "330px",
        "margin-top": "-185px"
    });
    var nick = $('#nick');
    var pwd = $('#pwd');
    var pwd2 = $('#pwd2');
    var prompt_box = $('.prompt-box');
    nick.parent().css("display", "block");
    pwd2.parent().css("display", "block");
    $('.dialog_anth .title').text("注册");
    $('#complete').text("注册");
    prompt_box.children().not('#switch').css("display", "none");
    prompt_box.children().eq(1).text("已有账号登录");
    login_pwd = pwd.val();
    nick.val(register_nick);
    pwd.val("");
    pwd2.val("");
}

// 转为登录dialog
function toLogin() {
    $('.dialog_anth .content').css({
        "height": "230px",
        "margin-top": "-135px"
    });
    var nick = $('#nick');
    var prompt_box = $('.prompt-box');
    nick.parent().css("display", "none");
    $('#pwd2').parent().css("display", "none");
    $('.dialog_anth .title').text("登录");
    $('#complete').text("登录");
    prompt_box.children().not('#switch').css("display", "inline");
    prompt_box.children().eq(1).text("注册");
    register_nick = nick.val();
    $('#pwd').val(login_pwd);
}

// 清除填写的数据
function clearAnth() {
    $('#acc').val("");
    $('#pwd').val("");
    $('#pwd2').val("");
    $('#nick').val("");
    register_nick = "";
    login_pwd = "";
}

// 登录请求
function login() {
    var acc = $('#acc').val();
    var pwd = $('#pwd').val();
    if (!check(acc, "aa", pwd, pwd))
        return;
    pwd = bcrypt(acc, pwd);
    var request = $.ajax({
        url: '/i/login',
        data: {
            "acc": acc,
            "pwd": pwd
        },
        beforeSend: function () {
            $('#complete').attr("disabled", "disabled").text("登录中...");
            $('#switch').css("pointer-events", "none");
        },
        complete: function () {
            $('#complete').removeAttr("disabled").text("登录");
            $('#switch').css("pointer-events", "auto");
        }
    });
    request.then(function (data) {
        handLogin(data);
    }, function (jqXHR, textStatus, errorThrown) {
        handErr(textStatus);
    });
}

// 注册请求
function register() {
    var acc = $('#acc').val();
    var nick = $('#nick').val().replace(/^\s+|\s+$/g, '').replace(/\s+/g, ' ');
    var pwd = $('#pwd').val();
    var pwd2 = $('#pwd2').val();
    if (!check(acc, nick, pwd, pwd2))
        return;
    pwd = rsaEncrypt(pwd);
    var request = $.ajax({
        url: '/i/register',
        data: {
            "acc": acc,
            "nick": nick,
            "pwd": pwd
        },
        beforeSend: function () {
            $('#complete').attr("disabled", "disabled").text("注册中...");
            $('#switch').css("pointer-events", "none");
        },
        complete: function () {
            $('#complete').removeAttr("disabled").text("注册");
            $('#switch').css("pointer-events", "auto");
        }
    });
    request.then(function (data) {
        handRegister(data);
    }, function (jqXHR, textStatus, errorThrown) {
        handErr(textStatus);
    });
}

// 检查数据
function check(acc, nick, pwd, pwd2) {
    if (!new RegExp("[\\w]{4,}").test(acc)) {
        showErr("帐号4~20位单词字符");
    } else if (nick.length < 2) {
        showErr("昵称2~20位");
    } else if (pwd.length < 6) {
        showErr("密码6~20位");
    } else if (pwd != pwd2) {
        showErr("两次密码不一致");
    } else {
        return true;
    }
    return false;
}

// 处理注册消息
function handRegister(data) {
    handLogin(data);
}

// 处理登录消息
function handLogin(data) {
    if (1 == data.state) {
        Cookies.set('token', data.data, {
            expires: expiresValue
        });
        location.reload();
    } else if (typeof (data.info) != "undefined") {
        showErr(data.info);
    }
}

function offline() {
    Cookies.remove('token');
    location.reload();
}

// rsa加密s
function rsaEncrypt(str) {
    var encrypt = new JSEncrypt();
    encrypt.setPublicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCOVmjkpQZsb3F8TYz/M9W3ltco5tGnrktIlTvV9c4w+b5kbt+qMKnbKl11Y4Rk0w706AnnZO+9jW8w3snWhJVrJ9wxwJq+rBZJvn8Egi6npXFehRyOEO5lZYIWLNHHN6mB7QIOcQQMGblH0/A6SQdt1LStGuoZ7n2hEcI2V0/cyQIDAQAB");
    return encrypt.encrypt(str);
}

// BCrypt固定格式加密密码
function bcrypt(acc, pwd) {
    var salt = acc;
    for (var i = acc.length; i < 22; i++) {
        salt = salt + "0";
    }
    salt = "$2a$10$" + salt.replace("_", "/");
    return dcodeIO.bcrypt.hashSync(pwd, salt);
}

// 处理认证
function handToken() {
    var token = Cookies.get('token');
    if ('string' == typeof (token)) {
        var info = JSON.parse(BASE64.decode(token));
        $('.header-anth-y').css("display", "block");
        $('#user_nick').text(info.nick);
        var request = $.ajax({
            url: '/i/checkToken',
            data: {
                "token": token
            }
        });
        request.then(function (data) {
            if (1 == data.state) {
                Cookies.set('token', token, {
                    expires: expiresValue
                });
            } else if (2 == data.state || 3 == data.state) {
                showErr(data.info, 5000);
                $('.header-anth').css("display", "block");
                $('.header-anth-y').css("display", "none");
                Cookies.remove('token');
            }
        }, function (jqXHR, textStatus, errorThrown) {
        });
    } else {
        $('.header-anth').css("display", "block");
    }
}

handToken();

function handUI() {
    var user_menu = $("#user-menu");
    var left = (user_menu.parent().width() - user_menu.width()) / 2;
    user_menu.css("left", left);
}

handUI();