package com.qwwuyu.server.ctrl.admin;

import com.qwwuyu.server.bean.FileBean;
import com.qwwuyu.server.bean.User;
import com.qwwuyu.server.configs.Constant;
import com.qwwuyu.server.filter.AuthRequired;
import com.qwwuyu.server.utils.CommUtil;
import com.qwwuyu.server.utils.FileUtil;
import com.qwwuyu.server.utils.J2EEUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
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
            J2EEUtil.renderInfo(response, "目录格式不正确");
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
            J2EEUtil.renderInfo(response, "目录格式不正确");
            return;
        }
        // 将当前上下文初始化给 CommonsMutipartResolver
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());
        // 检查form中是否有enctype="multipart/form-data"
        List<String> list = new ArrayList<>();
        if (multipartResolver.isMultipart(request)) {
            MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
            // 获取multiRequest 中所有的文件名
            Iterator<String> iterator = multiRequest.getFileNames();
            while (iterator.hasNext()) {
                MultipartFile multipartFile = multiRequest.getFile(iterator.next());
                if (multipartFile != null) {
                    String oFilename = multipartFile.getOriginalFilename();
                    String fileName = oFilename == null ? multipartFile.getName() : oFilename;
                    if (CommUtil.isExist(fileName)) {
                        File file = FileUtil.getFile(parent, fileName);
                        if (file != null && !file.exists()) {
                            multipartFile.transferTo(file);
                            list.add(fileName);
                        }
                    }
                }
            }
        }
        J2EEUtil.render(response, J2EEUtil.getSuccessBean().setData(list));
    }

    @RequestMapping(value = "delete", method = RequestMethod.POST)
    public void delete(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path) {
        User user = J2EEUtil.getUser(request);
        File file = FileUtil.getFile(path);
        if (!FileUtil.isFile(file)) {
            J2EEUtil.renderInfo(response, "文件格式不正确");
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
            J2EEUtil.renderInfo(response, "目录格式不正确");
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
            J2EEUtil.renderInfo(response, "文件格式不正确", HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }
        final String name = file.getName();
        final long length = file.length();
        long cLeft = 0, cRight = length;
        if (range != null) {
            try {
                cLeft = Long.parseLong(range.replaceAll("[^=]+=([\\d]+)-([\\d]*)", "$1"));
                cRight = Long.parseLong(range.replaceAll("[^=]+=([\\d]+)-([\\d]*)", "$2")) + 1;
            } catch (Exception ignored) {
            }
        }
        final long left = cLeft;
        final long right = cRight;
        long written = left;

        if (0 > left || right > length || right < left) {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return;
        }
        if (range != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", String.format("bytes %d-%d/%d", left, right - 1, length));
        }
        if (download) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(name, "utf-8") + "\"");
        } else {
            response.setHeader("Content-Disposition", "inline; filename=\"" + URLEncoder.encode(name, "utf-8") + "\"");
        }
        response.setHeader("Content-Length", String.valueOf(right - left));
        response.setHeader("Accept-Ranges", "bytes");
        try (InputStream is = new FileInputStream(file); OutputStream os = response.getOutputStream()) {
            if (left != 0) {
                is.skip(left);
            }
            int read;
            byte[] bytes = new byte[1024 * 1024];
            while ((read = is.read(bytes)) != -1) {
                written += read;
                if (written > right - 1) {
                    read = (int) (right + read - written);
                    os.write(bytes, 0, read);
                    break;
                }
                os.write(bytes, 0, read);
            }
            os.flush();
        }
    }

    @RequestMapping(value = "rename", method = RequestMethod.POST)
    public void rename(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path, @RequestParam("dest") String newPath) {
        User user = J2EEUtil.getUser(request);
        File file = FileUtil.getFile(path);
        if (!FileUtil.exists(file)) {
            J2EEUtil.renderInfo(response, "文件格式不正确");
            return;
        }
        File dest = FileUtil.getFile(newPath);
        if (dest == null || dest.exists()) {
            J2EEUtil.renderInfo(response, "文件格式不正确");
            return;
        }
        if (file.renameTo(dest)) {
            J2EEUtil.render(response, J2EEUtil.getSuccessBean().setInfo("操作成功"));
        } else {
            J2EEUtil.render(response, J2EEUtil.getErrorBean().setInfo("操作失败"));
        }
    }

    @RequestMapping(value = "createDir", method = RequestMethod.POST)
    public void createDir(HttpServletRequest request, HttpServletResponse response, @RequestParam("path") String path) {
        User user = J2EEUtil.getUser(request);
        if (path.matches(".*[\\\\/:*?\"<>|]+.*")) {
            J2EEUtil.renderInfo(response, "不能包含特殊字符\\/:*?\"<>|");
            return;
        }
        File file = FileUtil.getFile(path);
        if (file == null || file.exists() || file.getParentFile() == null || !file.getParentFile().exists()) {
            J2EEUtil.renderInfo(response, "文件格式不正确");
            return;
        }
        if (file.mkdir()) {
            J2EEUtil.render(response, J2EEUtil.getSuccessBean().setInfo("创建文件夹成功"));
        } else {
            J2EEUtil.render(response, J2EEUtil.getErrorBean().setInfo("创建文件夹失败"));
        }
    }
}