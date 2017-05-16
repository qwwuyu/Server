var isLogin = true;
var login_pwd = "";
var register_nick = "";
$.ajaxSetup({
	timeout : 5000,
	type : "POST",
	cache : false,
});
$(document).ready(function() {
	// 处理认证dialog关闭
	$('.dialog_anth .close').bind("click", function(event) {
		clearAnth();
		$('.dialog_anth').hide();
	});
	// 弹出登录dialog
	$('#login').bind("click", function(event) {
		if (!isLogin) {
			toLogin();
		}
		isLogin = true;
		$('.dialog_anth').show();
	});
	// 弹出注册dialog
	$('#register').bind("click", function(event) {
		if (isLogin) {
			toRegister();
		}
		isLogin = false;
		$('.dialog_anth').show();
	});
	// 切换登录or注册
	$('#switch').bind("click", function(event) {
		isLogin ? toRegister() : toLogin();
		isLogin = !isLogin;
		return false;
	});
	// 忘记密码
	$('#forget').bind("click", function(event) {
		alert("请联系管理...");
	});
	// 注册or登录
	$('#complete').bind("click", function(event) {
		isLogin ? login() : register();
		return false;
	});
});
// 转为注册dialog
function toRegister() {
	$('.dialog_anth .content').css({
		"height" : "291px",
		"margin-top" : "-165px"
	});
	$('#nick').parent().css("display", "block");
	$('.dialog_anth .title').text("注册");
	$('#complete').text("注册");
	$('.prompt-box').children().not('#switch').css("display", "none");
	$('.prompt-box').children().eq(1).text("已有账号登录");
	login_pwd = $('#pwd').val()
	$('#nick').val(register_nick);
	$('#pwd').val("");
}
// 转为登录dialog
function toLogin() {
	$('.dialog_anth .content').css({
		"height" : "230px",
		"margin-top" : "-135px"
	});
	$('#nick').parent().css("display", "none");
	$('.dialog_anth .title').text("登录");
	$('#complete').text("登录");
	$('.prompt-box').children().not('#switch').css("display", "inline");
	$('.prompt-box').children().eq(1).text("注册");
	register_nick = $('#nick').val();
	$('#pwd').val(login_pwd);
}
// 清除填写的数据
function clearAnth() {
	$('#acc').val("");
	$('#pwd').val("");
	$('#nick').val("");
	register_nick = "";
	login_pwd = "";
}
// 登录请求
function login() {
	var acc = $('#acc').val();
	var pwd = $('#pwd').val();
	if (!check(acc, "aa", pwd))
		return;
	pwd = bcrypt(acc, pwd);
	var request = $.ajax({
		url : '/i/login',
		data : {
			"acc" : acc,
			"pwd" : pwd
		},
		beforeSend : function() {
			$('#complete').attr("disabled", "disabled").text("登录中...");
			$('#switch').css("pointer-events", "none");
		},
		complete : function() {
			$('#complete').removeAttr("disabled").text("登录");
			$('#switch').css("pointer-events", "auto");
		},
	});
	request.then(function(data) {
		handLogin(data);
	}, function(jqXHR, textStatus, errorThrown) {
		handErr(textStatus);
	});
}
// 注册请求
function register() {
	var acc = $('#acc').val();
	var nick = $('#nick').val().replace(/^\s+|\s+$/g, '').replace(/\s+/g, ' ');
	var pwd = $('#pwd').val();
	if (!check(acc, nick, pwd))
		return;
	pwd = rsaEncrypt(pwd);
	var request = $.ajax({
		url : '/i/register',
		data : {
			"acc" : acc,
			"nick" : nick,
			"pwd" : pwd
		},
		beforeSend : function() {
			$('#complete').attr("disabled", "disabled").text("注册中...");
			$('#switch').css("pointer-events", "none");
		},
		complete : function() {
			$('#complete').removeAttr("disabled").text("注册");
			$('#switch').css("pointer-events", "auto");
		},
	});
	request.then(function(data) {
		handRegister(data);
	}, function(jqXHR, textStatus, errorThrown) {
		handErr(textStatus);
	});
}
// 检查数据
function check(acc, nick, pwd) {
	if (!new RegExp("[\\w]{4,}").test(acc)) {
		showErr("帐号不低于4位的单词字符");
	} else if (nick.length < 2) {
		showErr("昵称不低于2位");
	} else if (pwd.length < 6) {
		showErr("密码不低于6位");
	} else {
		return true;
	}
	return false;
}
// 处理登录消息
function handLogin(data) {
	console.log(data);
	// location.reload(true);
}
// 处理注册消息
function handRegister(data) {
	console.log(data);
	// location.reload(true);
}
// 处理请求失败
function handErr(textStatus) {
	if ("timeout" == textStatus) {
		showErr("请求超时");
	} else if ("error" == textStatus) {
		showErr("请求失败");
	} else if ("abort" == textStatus) {
		showErr("请求取消");
	} else if ("parsererror" == textStatus) {
		showErr("解析错误");
	} else {
		showErr("请求失败");
	}
}
// 提醒
function showErr(err) {
	alert(err);
}
// rsa加密s
function rsaEncrypt(str) {
	var encrypt = new JSEncrypt();
	encrypt
			.setPublicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCOVmjkpQZsb3F8TYz/M9W3ltco5tGnrktIlTvV9c4w+b5kbt+qMKnbKl11Y4Rk0w706AnnZO+9jW8w3snWhJVrJ9wxwJq+rBZJvn8Egi6npXFehRyOEO5lZYIWLNHHN6mB7QIOcQQMGblH0/A6SQdt1LStGuoZ7n2hEcI2V0/cyQIDAQAB");
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