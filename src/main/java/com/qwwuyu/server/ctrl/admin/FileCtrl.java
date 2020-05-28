package com.qwwuyu.server.ctrl.admin;

import com.qwwuyu.server.bean.FileBean;
import com.qwwuyu.server.bean.FileResultEntity;
import com.qwwuyu.server.bean.User;
import com.qwwuyu.server.configs.Constant;
import com.qwwuyu.server.configs.SecretConfig;
import com.qwwuyu.server.filter.AuthRequired;
import com.qwwuyu.server.utils.CommUtil;
import com.qwwuyu.server.utils.FileUtil;
import com.qwwuyu.server.utils.J2EEUtil;
import com.qwwuyu.server.utils.MultipartFileSender;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
@RequestMapping("/ad/file/")
@AuthRequired(permit = Constant.PERMIT_ADMIN, code = HttpServletResponse.SC_UNAUTHORIZED)
public class FileCtrl {
    private String hand(String path) {
        return Constant.PREFIX + "file/" + path;
    }

    @RequestMapping(value = "query", method = RequestMethod.POST)
    public void query(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path) {
        User user = J2EEUtil.getUser(request);
        File file = FileUtil.getFile(path);
        if (!FileUtil.isDirectory(file)) {
            J2EEUtil.renderInfo(response, "目录未符合");
            return;
        }
        List<FileBean> list = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File cf : files) {
                list.add(new FileBean(cf.getName(), cf.isDirectory()));
            }
        }
        J2EEUtil.render(response, J2EEUtil.getSuccessBean().setData(list));
    }

    @RequestMapping(value = "upload", method = RequestMethod.POST)
    public void upload(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path) throws IOException {
        User user = J2EEUtil.getUser(request);
        File parent = FileUtil.getFile(path);
        if (!FileUtil.isDirectory(parent)) {
            J2EEUtil.renderInfo(response, "目录未符合");
            return;
        }
        // 将当前上下文初始化给 CommonsMutipartResolver
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());
        // 检查form中是否有enctype="multipart/form-data"
        FileResultEntity entity = new FileResultEntity();
        if (multipartResolver.isMultipart(request)) {
            MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
            // 获取multiRequest 中所有的文件名
            Iterator<String> iterator = multiRequest.getFileNames();
            while (iterator.hasNext()) {
                MultipartFile multipartFile = multiRequest.getFile(iterator.next());
                //非文件
                if (multipartFile == null) continue;
                String oFilename = multipartFile.getOriginalFilename();
                String fileName = oFilename == null ? multipartFile.getName() : oFilename;
                //未获取文件名
                if (CommUtil.isEmpty(fileName)) continue;
                File file = FileUtil.getFile(parent, fileName);
                if (file == null) {
                    entity.setFailureFile(fileName);
                } else if (file.exists() && !fileName.equals(SecretConfig.cs[0])) {
                    entity.setExistFile(fileName);
                } else {
                    multipartFile.transferTo(file);
                    entity.setSuccessFile(fileName);
                }
            }
        }
        J2EEUtil.render(response, J2EEUtil.getSuccessBean().setInfo(null).setData(entity));
    }

    @RequestMapping(value = "delete", method = RequestMethod.POST)
    public void delete(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path) {
        User user = J2EEUtil.getUser(request);
        File file = FileUtil.getFile(path);
        if (!FileUtil.isFile(file)) {
            J2EEUtil.renderInfo(response, "文件未符合");
            return;
        }
        if (file.delete()) {
            J2EEUtil.render(response, J2EEUtil.getSuccessBean().setInfo("删除成功"));
        } else {
            J2EEUtil.render(response, J2EEUtil.getErrorBean().setInfo("删除失败"));
        }
    }

    @RequestMapping(value = "deleteDir", method = RequestMethod.POST)
    public void deleteDir(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path) {
        User user = J2EEUtil.getUser(request);
        File file = FileUtil.getFile(path);
        if (!FileUtil.isDirectory(file)) {
            J2EEUtil.renderInfo(response, "目录未符合");
            return;
        }
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            J2EEUtil.render(response, J2EEUtil.getErrorBean().setInfo("无法删除非空目录"));
        } else if (file.delete()) {
            J2EEUtil.render(response, J2EEUtil.getSuccessBean().setInfo("删除成功"));
        } else {
            J2EEUtil.render(response, J2EEUtil.getErrorBean().setInfo("删除失败"));
        }
    }

    @RequestMapping({"open", "open/*"})
    public String toFileManager(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path) throws IOException {
        User user = J2EEUtil.getUser(request);
        String suffix = path.toLowerCase();
        if (suffix.endsWith(".mp4") || suffix.endsWith(".flv")) {
            return hand("video.html");
        } /*else if (suffix.endsWith(".jpg") || suffix.endsWith(".png") || suffix.endsWith(".bmp") || suffix.endsWith(".gif") || suffix.endsWith(".jpeg") || suffix.endsWith(".webp")) {
            return hand("pic.html");
        }*/
        transferFile(request, response, path, false);
        return null;
    }

    @RequestMapping(path = {"download", "download/*"})
    public void download(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path) throws IOException {
        transferFile(request, response, path, true);
    }

    private void transferFile(HttpServletRequest request, HttpServletResponse response, String path, boolean download) throws IOException {
        User user = J2EEUtil.getUser(request);
        String range = request.getHeader("range");
        File file = FileUtil.getFile(path);
        if (!FileUtil.isFile(file)) {
            J2EEUtil.renderInfo(response, "文件未符合", HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        new MultipartFileSender(file.toPath(), request, response, download).serveResource();
    }

    @RequestMapping(value = "rename", method = RequestMethod.POST)
    public void rename(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path, @RequestParam("dest") String newPath) {
        User user = J2EEUtil.getUser(request);
        File file = FileUtil.getFile(path);
        if (!FileUtil.exists(file)) {
            J2EEUtil.renderInfo(response, "文件未符合");
            return;
        }
        File dest = FileUtil.getFile(newPath);
        if (dest == null || dest.exists()) {
            J2EEUtil.renderInfo(response, "文件未符合");
            return;
        }
        if (file.renameTo(dest)) {
            J2EEUtil.render(response, J2EEUtil.getSuccessBean().setInfo("操作成功"));
        } else {
            J2EEUtil.render(response, J2EEUtil.getErrorBean().setInfo("操作失败"));
        }
    }

    @RequestMapping(value = "createDir", method = RequestMethod.POST)
    public void createDir(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path, @RequestParam("dirName") String dirName) {
        User user = J2EEUtil.getUser(request);
        if (dirName.matches(".*[\\\\/:*?\"<>|]+.*")) {
            J2EEUtil.renderInfo(response, "不能包含特殊字符\\/:*?\"<>|");
            return;
        }
        File file = FileUtil.getFile(path);
        if (file == null || !file.isDirectory()) {
            J2EEUtil.renderInfo(response, "文件夹不存在");
            return;
        }
        File dirFile = new File(file, dirName);
        if (dirFile.exists()) {
            J2EEUtil.renderInfo(response, "文件夹已存在");
            return;
        }
        if (dirFile.mkdir()) {
            J2EEUtil.render(response, J2EEUtil.getSuccessBean().setInfo("创建文件夹成功"));
        } else {
            J2EEUtil.render(response, J2EEUtil.getErrorBean().setInfo("创建文件夹失败"));
        }
    }

    @RequestMapping(value = "downloadFile", method = RequestMethod.POST)
    public void downloadFile(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path,
                             @RequestParam("downloadUrl") String downloadUrl) throws IOException {
        User user = J2EEUtil.getUser(request);
        File downloadDir = FileUtil.getFile(path);
        if (downloadDir == null || !downloadDir.isDirectory()) {
            J2EEUtil.renderInfo(response, "文件夹不存在");
            return;
        }
        URL url = new URL(downloadUrl);
        URLConnection con = url.openConnection();
        String fieldValue = con.getHeaderField("Content-Disposition");
        String filename = null;
        if (fieldValue != null) {
            filename = fieldValue.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
        }
        if (CommUtil.isEmpty(filename)) {
            filename = FilenameUtils.getName(url.getPath());
        }
        filename = URLDecoder.decode(filename, "UTF-8");
        File file = new File(downloadDir, filename);
        if (file.exists()) {
            J2EEUtil.renderInfo(response, "文件已存在");
            return;
        }
        ReadableByteChannel rbc = Channels.newChannel(con.getInputStream());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        J2EEUtil.render(response, J2EEUtil.getSuccessBean().setInfo("下载结束"));
    }
}
