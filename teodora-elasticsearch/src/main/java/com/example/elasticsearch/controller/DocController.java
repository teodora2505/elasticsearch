package com.example.elasticsearch.controller;

import com.example.elasticsearch.dao.DocDAO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import com.example.elasticsearch.model.Doc;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.commons.io.FilenameUtils;

import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class DocController {

    public static String uploadDirectory = System.getProperty("user.dir") + "/uploads";
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private DocDAO docDAO;

    @RequestMapping(value = "/")
    public String UploadPage() {
        return "home";
    }

    @RequestMapping("/upload")
    public String upload(Model model, @RequestParam("files") MultipartFile[] files) throws IOException, ParseException {
        StringBuilder fileNames = new StringBuilder();
        ArrayList<Doc> listaDokumenata = new ArrayList<Doc>();
        for (MultipartFile file : files) {
            Path fileNameAndPath = Paths.get(uploadDirectory, file.getOriginalFilename());
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String title = file.getOriginalFilename();
            long size = file.getSize();
            String fileNameWithOutExtension = FilenameUtils.removeExtension(title);
            String path = fileNameAndPath.toString();
            String category = "books";
            Random random = new Random();
            long rnd = random.nextLong();
            System.out.println(fileNameWithOutExtension);
            System.out.println(rnd);
            String docId = fileNameWithOutExtension + Math.abs(rnd);
            String location = path.replaceAll("\\\\", "/");
            fileNames.append(file.getOriginalFilename() + " ");
            if (new File(uploadDirectory, title).exists()) {
                System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                String extension = FilenameUtils.getExtension(file.getOriginalFilename());
                title = fileNameWithOutExtension + '(' + ((int) (Math.random() * 100)) + ")." + extension;
                file = new MockMultipartFile(FilenameUtils.getBaseName(title), file.getInputStream());
                fileNameAndPath = Paths.get(uploadDirectory, title);
                System.out.println(fileNameAndPath);
                System.out.println(title);
            }
            try {
                Files.write(fileNameAndPath, file.getBytes(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            BasicFileAttributes attr = Files.readAttributes(fileNameAndPath, BasicFileAttributes.class);
            FileTime fileCreationTime = attr.creationTime();
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
            String dateCreated = df.format(fileCreationTime.toMillis());
            Date creationDate = new SimpleDateFormat("dd-MM-yyyy").parse(dateCreated);
            Doc document = new Doc(docId, title, category, content, creationDate, size, location);
            docDAO.addNewDoc(document);
            listaDokumenata.add(document);
        }
        model.addAttribute("listaDokumenata", listaDokumenata);
        return "home";
    }

    @RequestMapping("/download/{documentId}")
    public void downloadDocument(@PathVariable String documentId, HttpServletResponse response) {
        Doc doc = docDAO.getDocById(documentId);
        String fileName = doc.getTitle();
        if (fileName.indexOf(".doc") > -1) {
            response.setContentType("application/msword");
        }
        if (fileName.indexOf(".txt") > -1) {
            response.setContentType("application/txt");
        }
        if (fileName.indexOf(".docx") > -1) {
            response.setContentType("application/msword");
        }
        if (fileName.indexOf(".xls") > -1) {
            response.setContentType("application/vnd.ms-excel");
        }
        if (fileName.indexOf(".csv") > -1) {
            response.setContentType("application/vnd.ms-excel");
        }
        if (fileName.indexOf(".ppt") > -1) {
            response.setContentType("application/ppt");
        }
        if (fileName.indexOf(".pdf") > -1) {
            response.setContentType("application/pdf");
        }
        if (fileName.indexOf(".zip") > -1) {
            response.setContentType("application/zip");
        }
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setHeader("Content-Transfer-Encoding", "binary");
        try {
            BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
            FileInputStream fis = new FileInputStream(uploadDirectory + "\\" + fileName);
            int len;
            byte[] buf = new byte[1024];
            while ((len = fis.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
            bos.close();
            response.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    @RequestMapping("/all")
    public void getAllDocs() {
        List<Doc> allDocs = docDAO.getAllDocs();
        System.out.println(allDocs);
    }

    @RequestMapping("/id/{docId}")
    public Doc getDoc(@PathVariable String docId) {
        Doc doc = docDAO.getDocById(docId);
        return doc;
    }

    @RequestMapping(value = "/new", method = RequestMethod.POST)
    public Doc addNewDoc(@RequestBody Doc doc) {
        docDAO.addNewDoc(doc);
        return doc;
    }

    /*
    @RequestMapping(value = "/settings/{name}", method = RequestMethod.GET)
    public Object getAllUserSettings(@PathVariable String name) {
        return userDAO.getAllUserSettings(name);
    }

    @RequestMapping(value = "/settings/{name}/{key}", method = RequestMethod.GET)
    public String getUserSetting(
            @PathVariable String name, @PathVariable String key) {
        return userDAO.getUserSetting(name, key);
    }

    @RequestMapping(value = "/settings/{name}/{key}/{value}", method = RequestMethod.GET)
    public String addUserSetting(
            @PathVariable String name,
            @PathVariable String key,
            @PathVariable String value) {
        return userDAO.addUserSetting(name, key, value);
    }*/
}
