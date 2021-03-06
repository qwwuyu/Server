var isHistoryApi = !!(window.history && history.pushState);
// temp_file模版
template(
    'temp_file',
    '<ul> {{each datas}} <li> {{if $value.dir == true}} <a class="file-folder flex-center" href="{{dirPath}}{{$value.path}}" data-name="{{$value.name}}"> <i class="ion-icon ion-folder"></i> <span class="file-text file-text-folder">{{$value.name}}</span> </a> {{if $value.date != null || $value.info != null}} <div class="flex-center file-ctrl"> {{if $value.date != null}} <span class="file-date ml12">{{$value.date}}</span> {{/if}} {{if $value.date != null && $value.info != null}} <span class="file-info ml24">{{$value.info}}</span> {{else $value.info != null}} <span class="file-info ml12">{{$value.info}}</span> {{/if}} </div> {{/if}} {{else if $value.dir != true}} <div class="flex-center"> <i class="ion-icon ion-document"></i> <span class="file-text file-text-file">{{$value.name}}</span> </div> <div class="flex-center file-ctrl"> <a class="file-open ml12" href="{{openPath}}{{$value.path}}" target="_blank">打开</a> <a class="file-download ml24" href="{{downloadPath}}{{$value.path}}">下载</a> <a class="file-delete ml24" href="javascript:;" data-name="{{$value.name}}">删除</a> {{if $value.apk == true}} <a class="file-apk ml24" href="javascript:;" data-name="{{$value.name}}">安装</a> {{/if}} {{if $value.date != null}} <span class="file-date ml24">{{$value.date}}</span> {{/if}} {{if $value.info != null}} <span class="file-info ml24">{{$value.info}}</span> {{/if}} </div> {{/if}} </li> {{/each}}</ul>');

$(document).ready(function () {
    initUpload(decodeURI(getParam("path")));
    $("#progress").hide();
    $("#result").hide();
    if (isHistoryApi) {
        $(window).on("popstate", function (event) {
            var path = decodeURI(getParam("path"));
            initUpload(path);
        });
    }
    $('body').on('click', '.file-folder', function (e) {
        if (!isHistoryApi) {
            return true;
        }
        var oldPath = decodeURI(getParam("path"));
        var name = $(this).data("name");
        var path = oldPath + ("" != oldPath ? "/" : "") + name;
        var url = location.pathname + "?path=" + path.replace(/\[/g, "%5B").replace(/]/g, "%5D").replace(/{/g, "%7B").replace(/}/g, "%7D");
        history.pushState(null, name, url);
        initUpload(path);
        return false;
    }).on('click', '.file-delete', function (e) {
        var oldPath = decodeURI(getParam("path"));
        var name = $(this).data("name");
        var path = oldPath + ("" != oldPath ? "/" : "") + name;
        if (confirm("你确认要删除文件：" + name + "?")) {
            deleteFile(path, $(this))
        }
    }).on('click', '#back', function (e) {
        var path = decodeURI(getParam("path"));
        goBack(path)
    }).on('click', '#deleteDir', function (e) {
        var path = decodeURI(getParam("path"));
        if ("" != path && confirm("你确认要删除目录：" + path + "?")) {
            deleteDir(path)
        }
    }).on('click', '#newDir', function (e) {
        var word = prompt("输入文件夹名称");
        if (word && "" != word) {
            var path = decodeURI(getParam("path"));
            createDir(word, path)
        }
    }).on('click', '#downloadFile', function (e) {
        var word = prompt("输入下载地址");
        if (word && "" != word) {
            var path = decodeURI(getParam("path"));
            downloadFile(word, path)
        }
    }).on('click', '#checkDownloadFile', function (e) {
        checkDownloadFile();
    });
});

function requestFile(path) {
    var request = $.ajax({
        url: location.pathname + "/query",
        data: {
            "path": path
        },
        beforeSend: function () {
        },
        complete: function () {
        }
    });
    request.then(function (data) {
        if (1 == data.state) {
            handFileData(data.data);
        } else {
            handFileData({});
        }
        if (1 != data.state && data.info) {
            showErr(data.info);
        }
    }, function (jqXHR, textStatus, errorThrown) {
        handErr(textStatus);
        handFileData({});
    });
}

// 处理列表数据
function handFileData(list) {
    var oldPath = decodeURI(getParam("path"));
    oldPath = oldPath + ("" != oldPath ? "/" : "");
    for (let i = 0; i < list.length; i++) {
        list[i].path = list[i].name.replace(/\[/g, "%5B").replace(/]/g, "%5D").replace(/{/g, "%7B").replace(/}/g, "%7D")
    }
    var temp_file = template('temp_file', {
        datas: list,
        dirPath: location.pathname + "?path=" + oldPath,
        downloadPath: location.pathname + "/download?path=" + oldPath,
        openPath: location.pathname + "/open?path=" + oldPath
    });
    $('.content').html(temp_file);
    setDeleteDir();
}

function setDeleteDir() {
    if ($('.content').find("li").length != 0) {
        $("#deleteDir").hide();
    } else {
        $("#deleteDir").show();
    }
}

function initUpload(path) {
    $("#deleteDir").hide();
    if (path == "") {
        $("#back").hide();
        $("#dir").text("全部文件");
    } else {
        $("#back").show();
        $("#dir").text(path);
    }
    requestFile(path);
    //初始化上传插件
    $('#fileupload').fileupload({
        url: location.pathname + '/upload',
        formData: {
            "path": decodeURI(getParam("path"))
        },
        dataType: 'text',
        progressall: function (e, data) {
            $('#progress').text(data.loaded + "/" + data.total);
        },
        start: function (e) {
            $("#progress").show();
            $("#result").text("");
            $("#result").show();
        },
        done: function (e, data) {
            $('#result').text($('#result').text() + "done>>" + data.result + "\n");
        },
        fail: function (e, data) {
            $('#result').text($('#result').text() + "fail>>" + data.result + "\n");
        },
        stop: function (e) {
            var path = decodeURI(getParam("path"));
            requestFile(path);
        },
    });
}

function deleteFile(path, obj) {
    var params = getRequest();
    var request = $.ajax({
        url: location.pathname + "/delete",
        data: {
            "path": path
        }
    });
    request.then(function (data) {
        if (1 == data.state) {
            showSucc("删除成功");
            obj.parent().parent().remove();
            setDeleteDir();
        } else if (data.info) {
            showErr(data.info);
        }
    }, function (jqXHR, textStatus, errorThrown) {
        handErr(textStatus);
    });
}

function deleteDir(path) {
    var params = getRequest();
    var request = $.ajax({
        url: location.pathname + "/deleteDir",
        data: {
            "path": path
        }
    });
    request.then(function (data) {
        if (1 == data.state) {
            showSucc("删除成功");
            goBack(path)
        } else if (data.info) {
            showErr(data.info);
        }
    }, function (jqXHR, textStatus, errorThrown) {
        handErr(textStatus);
    });
}

function createDir(dirName, path) {
    var params = getRequest();
    var request = $.ajax({
        url: location.pathname + "/createDir",
        data: {
            "path": path,
            "dirName": dirName
        }
    });
    request.then(function (data) {
        if (1 == data.state) {
            showSucc("创建成功");
            var path = decodeURI(getParam("path"));
            requestFile(path);
        } else if (data.info) {
            showErr(data.info);
        }
    }, function (jqXHR, textStatus, errorThrown) {
        handErr(textStatus);
    });
}

function downloadFile(downloadUrl, path) {
    var params = getRequest();
    var request = $.ajax({
        url: location.pathname + "/downloadFile",
        data: {
            "path": path,
            "downloadUrl": downloadUrl
        }
    });
    request.then(function (data) {
        if (data.state == 1) {
            showSucc(data.info);
            requestFile(decodeURI(getParam("path")));
        } else if (data.state == 2) {
            showSucc(data.info);
        } else if (data.info) {
            showErr(data.info);
        }
    }, function (jqXHR, textStatus, errorThrown) {
        handErr(textStatus);
    });
}

function checkDownloadFile() {
    var params = getRequest();
    var request = $.ajax({
        type: 'GET',
        url: location.pathname + "/checkDownloadFile"
    });
    request.then(function (data) {
        if (data.state == 1) {
            showSucc(data.info);
        } else if (data.info) {
            showErr(data.info);
        }
    }, function (jqXHR, textStatus, errorThrown) {
        handErr(textStatus);
    });
}

function goBack(oldPath) {
    if (oldPath != "") {
        var path;
        var url;
        if (oldPath.lastIndexOf("/") != -1) {
            path = oldPath.substring(0, oldPath.lastIndexOf("/"));
            url = location.pathname + "?path=" + path.replace(/\[/g, "%5B").replace(/]/g, "%5D").replace(/{/g, "%7B").replace(/}/g, "%7D");
        } else {
            path = "";
            url = location.pathname;
        }
        history.pushState(null, name, url);
        initUpload(path);
    }
}