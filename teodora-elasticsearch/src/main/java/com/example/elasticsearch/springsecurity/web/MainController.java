package com.example.elasticsearch.springsecurity.web;

import com.example.elasticsearch.dao.DocDAO;
import com.example.elasticsearch.model.Category;
import com.example.elasticsearch.model.Doc;
import com.example.elasticsearch.model.DocDTO;
import com.example.elasticsearch.springsecurity.model.Role;
import com.example.elasticsearch.springsecurity.repository.RoleRepository;
import com.example.elasticsearch.springsecurity.repository.UserRepository;
import com.example.elasticsearch.springsecurity.service.UserService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class MainController {

    public static String uploadDirectory = System.getProperty("user.dir") + "/uploads";
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final Integer DOCUMENTS_PER_PAGE = 10;

    @Autowired
    private DocDAO docDAO;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @GetMapping("/user")
    public String userIndex() {
        return "user/index";
    }

       @GetMapping("/")
    public String root(Model model, HttpServletRequest request) {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN");
        if(userService.findByEmail(request.getUserPrincipal().getName()).getRoles().contains(adminRole)){
            model.addAttribute("adminLogged", true);
        }
        model.addAttribute("categories", dodajKategorije());
        model.addAttribute("c1", docDAO.getNumberOfDocsByCategory("knjige"));
        model.addAttribute("c2", docDAO.getNumberOfDocsByCategory("laboratorijske"));
        model.addAttribute("c3", docDAO.getNumberOfDocsByCategory("blanketi"));
        model.addAttribute("c4", docDAO.getNumberOfDocsByCategory("seminarski"));
        model.addAttribute("c5", docDAO.getNumberOfDocsByCategory("diplomski"));
        model.addAttribute("c6", docDAO.getNumberOfDocsByCategory("master"));
        model.addAttribute("showDocsCount", true);
        return "home";
    }

    public String readDocxFile(String filepathabs) {
        try {
            FileInputStream fis = new FileInputStream(filepathabs);
            XWPFDocument document = new XWPFDocument(fis);
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            String content = "";
            for (XWPFParagraph para : paragraphs) {
                content += para.getText();
                System.out.println(para.getText());
            }
            fis.close();
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping("/upload")
    public String upload(Model model, @RequestParam("files") MultipartFile[] files, @RequestParam("category") String selectedCategory) throws IOException, ParseException {
        model.addAttribute("categories", dodajKategorije());
        model.addAttribute("showDocsCount", false);
        StringBuilder fileNames = new StringBuilder();
        ArrayList<Doc> listaDokumenata = new ArrayList<Doc>();
        for (MultipartFile file : files) {
            Path fileNameAndPath = Paths.get(uploadDirectory, file.getOriginalFilename());
            String ext = FilenameUtils.getExtension(file.getOriginalFilename());
            String content = null;
            System.out.println(ext);
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String title = file.getOriginalFilename();
            long size = file.getSize();
            String fileNameWithOutExtension = FilenameUtils.removeExtension(title);
            String path = fileNameAndPath.toString();
            String category = selectedCategory;
            Random random = new Random();
            String docId = fileNameWithOutExtension + ((int) (Math.random() * 1000));
            //long rnd = random.nextLong();
            //String docId = fileNameWithOutExtension + Math.abs(rnd);
            System.out.println(fileNameWithOutExtension);
            //System.out.println(rnd);
            String location = path.replaceAll("\\\\", "/");
            fileNames.append(file.getOriginalFilename() + " ");
            // u slucaju da postoji kopija, rename-uj je 🙂
            if (new File(uploadDirectory, title).exists()) {
                String extension = FilenameUtils.getExtension(file.getOriginalFilename());
                title = fileNameWithOutExtension + '(' + ((int) (Math.random() * 100)) + ")." + extension;
                file = new MockMultipartFile(FilenameUtils.getBaseName(title), file.getInputStream());
                fileNameAndPath = Paths.get(uploadDirectory, title);
                path = fileNameAndPath.toString();
                location = path.replaceAll("\\\\", "/");
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
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date creationDate = df.parse(df.format(fileCreationTime.toMillis()));
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -2);
            Date yesterday = calendar.getTime();
            if (ext.equals("docx")) {
                content = readDocxFile(fileNameAndPath.toString());
            } else if (ext.equals("pdf")) {
                content = getPdfContent(fileNameAndPath.toString());
            }
            Doc document = new Doc(docId, title, category, content, creationDate, size, location);
            docDAO.addNewDoc(document);
            listaDokumenata.add(document);
        }
        ArrayList<DocDTO> docs = new ArrayList<>();
        for (Doc doc : listaDokumenata) {
            String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(doc.getCreationDate());
            docs.add(new DocDTO(doc.getDocId(), doc.getTitle(), doc.getCategory(), formattedDate, String.format("%.2f", ((double)doc.getSize()/1024)).toString()));
        }
        model.addAttribute("listaDokumenata", docs);
        return "home";
    }

    public String getPdfContent(String abspath) throws IOException {
        PDDocument document = PDDocument.load(new File(abspath));
        String text = null;
        if (!document.isEncrypted()) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(document);
            return text;
        }
        document.close();
        return text;
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
    public List<Doc> getAllDocs() {
        List<Doc> allDocs = docDAO.getAllDocs();
        return allDocs;
    }

    @RequestMapping("/id/{docId}")
    public Doc getDoc(@PathVariable String docId) {
        Doc doc = docDAO.getDocById(docId);
        System.out.println(doc.getCreationDate());
        return doc;
    }

    @RequestMapping(value = "/new", method = RequestMethod.POST)
    public Doc addNewDoc(@RequestBody Doc doc) {
        docDAO.addNewDoc(doc);
        return doc;
    }

    @RequestMapping("/search/{page}")
    public String search(Model model, @PathVariable int page,
            @RequestParam("searchQuery") Optional<String> paramSearchQuery,
            @RequestParam("searchContent") Optional<String> paramSearchContent,
            @RequestParam("searchTitle") Optional<String> paramSearchTitle,
            @RequestParam("searchId") Optional<String> paramSearchId,
            @RequestParam("type") Optional<String> paramType,
            @RequestParam("category") Optional<String> paramCategory,
            @RequestParam("minSize") Optional<Integer> paramMinSize,
            @RequestParam("maxSize") Optional<Integer> paramMaxSize,
            @RequestParam("minDate") Optional<String> paramMinDate,
            @RequestParam("maxDate") Optional<String> paramMaxDate,
            @RequestParam("sort") Optional<Integer> paramSort,
            HttpServletRequest request) throws ParseException {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN");
        if(userService.findByEmail(request.getUserPrincipal().getName()).getRoles().contains(adminRole)){
            model.addAttribute("adminLogged", true);
        }
        model.addAttribute("types", this.dodajTipovePretrageZaSearchStranu());
        model.addAttribute("categories", this.dodajKategorijeZaSearchStranu());
        model.addAttribute("sorts", this.dodajSortiranjaZaSearchStranu());
        String searchQuery = null;
        boolean searchContent = false;
        boolean searchTitle = false;
        boolean searchId = false;
        String type = null;
        String category = null;
        long minSize = new Integer(0).longValue();
        long maxSize = new Integer(15360).longValue();
        Date minDate = null;
        Date maxDate = null;
        String sortField = "";
        Boolean sortASCorDESC = false; //asc-true, desc-false
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Page<Doc> pageWithDocuments = null;
        List<Doc> documentsFromPage = null;
        int totalPages;
        String urlQueryParams = "";
        String poruka = "";
        boolean prikaziPoruku = false;
        if (!paramSearchQuery.isPresent()
                && !paramSearchContent.isPresent()
                && !paramSearchTitle.isPresent()
                && !paramSearchId.isPresent()
                && !paramType.isPresent()
                && !paramCategory.isPresent()
                && !paramMinSize.isPresent()
                && !paramMaxSize.isPresent()
                && !paramMinDate.isPresent()
                && !paramMaxDate.isPresent()
                && !paramSort.isPresent()) {
            pageWithDocuments = docDAO.getPageOfDocs(page - 1, DOCUMENTS_PER_PAGE);
            documentsFromPage = pageWithDocuments.getContent();

            if (docDAO.getTotalNumberOfDocuments() % 10 == 0) {
                totalPages = docDAO.getTotalNumberOfDocuments() / DOCUMENTS_PER_PAGE;
            } else {
                totalPages = docDAO.getTotalNumberOfDocuments() / DOCUMENTS_PER_PAGE + 1;
            }

        } else {
            searchQuery = paramSearchQuery.isPresent() && !paramSearchQuery.get().equals("") ? paramSearchQuery.get() : null;
            searchContent = paramSearchContent.isPresent();
            searchTitle = paramSearchTitle.isPresent();
            searchId = paramSearchId.isPresent();
            type = paramType.isPresent() && !paramType.get().equals("") ? paramType.get() : null;
            category = paramCategory.isPresent() && !paramCategory.get().equals("") ? paramCategory.get() : null;
            minSize = paramMinSize.isPresent() ? Long.valueOf(paramMinSize.get()) : Long.valueOf(0);
            maxSize = paramMaxSize.isPresent() ? Long.valueOf(paramMaxSize.get()) : Long.valueOf(15360);
            minDate = paramMinDate.isPresent() && !paramMinDate.get().equals("") ? sdf.parse(paramMinDate.get()) : null;
            maxDate = paramMaxDate.isPresent() && !paramMaxDate.get().equals("") ? sdf.parse(paramMaxDate.get()) : null;
            if (paramSort.isPresent() && 1 <= paramSort.get() && paramSort.get() <= 6) {
                switch (paramSort.get()) {
                    case 1:
                        sortField = "size";
                        sortASCorDESC = true; //size ASC
                        break;
                    case 2:
                        sortField = "size";
                        sortASCorDESC = false; //size DESC
                        break;
                    case 3:
                        sortField = "creationDate";
                        sortASCorDESC = true; //creationDate ASC
                        break;
                    case 4:
                        sortField = "creationDate";
                        sortASCorDESC = false; //creationDate DESC
                        break;
                    case 5:
                        sortField = "category";
                        sortASCorDESC = true; //category ASC
                        break;
                    case 6:
                        sortField = "category";
                        sortASCorDESC = false; //category DESC
                        break;
                    default:
                        sortField = "";
                        sortASCorDESC = false;
                        break;
                }
            } else {
                sortField = "";
                sortASCorDESC = false;
            }

            if (minSize > maxSize) {
                poruka = "Filteri za veličinu dokumenata su neadekvatni. Pokušajte ponovo.";
                prikaziPoruku = true;
                model.addAttribute("poruka", poruka);
            }
            if (minDate != null && maxDate != null && minDate.after(maxDate)) {
                poruka = "Filteri za datum su neadekvatni. Pokušajte ponovo.";
                prikaziPoruku = true;
                model.addAttribute("poruka", poruka);
            }

            urlQueryParams += "?searchQuery=" + paramSearchQuery.get() + "&";
            if (searchContent) {
                urlQueryParams += "searchContent=on" + "&";
            }
            if (searchTitle) {
                urlQueryParams += "searchTitle=on" + "&";
            }
            if (searchId) {
                urlQueryParams += "searchId=on" + "&";
            }
            urlQueryParams += "type=" + paramType.get() + "&";
            urlQueryParams += "category=" + paramCategory.get() + "&";
            if (paramMinSize.isPresent()) {
                urlQueryParams += "minSize=" + paramMinSize.get() + "&";
            } else {
                urlQueryParams += "minSize=&";
            }
            if (paramMaxSize.isPresent()) {
                urlQueryParams += "maxSize=" + paramMaxSize.get() + "&";
            } else {
                urlQueryParams += "maxSize=&";
            }
            urlQueryParams += "minDate=" + paramMinDate.get() + "&";
            urlQueryParams += "maxDate=" + paramMaxDate.get() + "&";
            urlQueryParams += "sort=" + paramSort.get();

            model = this.setModelParamAttributesForSearchPage(model, paramSearchQuery, paramSearchContent, paramSearchTitle, paramSearchId,
                    paramType, type, paramCategory, category, paramMinSize, paramMaxSize, paramMinDate, paramMaxDate, paramSort);
            pageWithDocuments = docDAO.getPageOfFilteredDocs(page - 1, DOCUMENTS_PER_PAGE, searchQuery, searchContent, searchTitle,
                    searchId, type, category, minSize, maxSize, minDate, maxDate, sortField, sortASCorDESC);
            documentsFromPage = pageWithDocuments.getContent();

            int totalNumberOfFilteredDocs = docDAO.getTotalNumberOfFilteredDocs(searchQuery, searchContent, searchTitle, searchId,
                    type, category, minSize, maxSize, minDate, maxDate, sortField, sortASCorDESC);

            if (totalNumberOfFilteredDocs % 10 == 0) {
                totalPages = totalNumberOfFilteredDocs / DOCUMENTS_PER_PAGE;
            } else {
                totalPages = totalNumberOfFilteredDocs / DOCUMENTS_PER_PAGE + 1;
            }

        }

        model.addAttribute("urlQueryParams", urlQueryParams);
        ArrayList<Integer> pages = new ArrayList<Integer>();
        int startPage;
        if (page % 10 == 0) {
            startPage = page - 10 + 1;
        } else {
            startPage = page - page % 10 + 1;
        }
        int endPage = startPage + DOCUMENTS_PER_PAGE - 1;
        if (endPage > totalPages) {
            endPage = totalPages;
        }
        for (int i = startPage; i <= endPage; i++) {
            pages.add(i);
        }
        model.addAttribute("pages", pages);
        model.addAttribute("currentPage", page);
        if (startPage - 1 > 0) {
            model.addAttribute("previousRange", startPage - 1);
        } else {
            model.addAttribute("previousRange", -1);
        }
        if (endPage < totalPages) {
            model.addAttribute("nextRange", endPage + 1);
        } else {
            model.addAttribute("nextRange", -1);
        }
        ArrayList<DocDTO> docsFromPage = new ArrayList<>();
        for (Doc doc : documentsFromPage) {
            String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(doc.getCreationDate());
            docsFromPage.add(new DocDTO(doc.getDocId(), doc.getTitle(), doc.getCategory(), formattedDate, String.format("%.2f", ((double)doc.getSize()/1024)).toString()));
        }
        model.addAttribute("listaDokumenata", docsFromPage);
        if (docsFromPage.size() == 0 && poruka == "") {
            poruka = "Nema dokumenata za prikaz.";
            model.addAttribute("poruka", poruka);
            prikaziPoruku = true;
        }
        model.addAttribute("prikaziPoruku", prikaziPoruku);
        return "searchpage";
    }

    @RequestMapping("/delete/{docId}")
    public String deleteDoc(Model model, @PathVariable String docId) {
        Doc doc = this.getDoc(docId);
        docDAO.deleteDoc(docId);
        try {
            System.out.println(Paths.get(doc.getLocation().replace("/", "\\")));
            Files.deleteIfExists(Paths.get(doc.getLocation().replace("/", "\\")));
        } catch (NoSuchFileException e) {
            System.out.println("No such file/directory exists");
        } catch (DirectoryNotEmptyException e) {
            System.out.println("Directory is not empty.");
        } catch (IOException e) {
            System.out.println("Invalid permissions.");
        }
        List<Doc> documentsAll = docDAO.getAllDocs();
        model.addAttribute("sorts", this.dodajSortiranja());
        model.addAttribute("listaDokumenata", documentsAll);
        return "redirect:/manage";
    }

    @RequestMapping("/deleteAll")
    public String deleteAllDocs(Model model) {
        List<Doc> documentsAll = this.getAllDocs();
        for (Doc document : documentsAll) {
            docDAO.deleteDoc(document.getDocId());
            try {
                Files.deleteIfExists(Paths.get(document.getLocation().replace("/", "\\")));
            } catch (NoSuchFileException e) {
                System.out.println("No such file/directory exists");
            } catch (DirectoryNotEmptyException e) {
                System.out.println("Directory is not empty.");
            } catch (IOException e) {
                System.out.println("Invalid permissions.");
            }
        }
        model.addAttribute("sorts", this.dodajSortiranja());
        model.addAttribute("listaDokumenata", null);
        return "redirect:/manage";
    }

    @RequestMapping("/manage")
    public String manageDocs(Model model,HttpServletRequest request, @RequestParam("sort") Optional<Integer> sort,
            @RequestParam("docTitle") Optional<String> docTitle) throws ParseException {
        model.addAttribute("sorts", this.dodajSortiranja());
        List<Doc> documentsAll = null;
        ArrayList<DocDTO> docs = new ArrayList<>();
        if (sort.isPresent() && 1 <= sort.get() && sort.get() <= 6) {
            switch (sort.get()) {
                case 1:
                    documentsAll = docDAO.sortDocsBy("size", true); //size ASC
                    break;
                case 2:
                    documentsAll = docDAO.sortDocsBy("size", false); //size DESC
                    break;
                case 3:
                    documentsAll = docDAO.sortDocsBy("creationDate", true); //creationDate ASC
                    break;
                case 4:
                    documentsAll = docDAO.sortDocsBy("creationDate", false); //creationDate DESC
                    break;
                case 5:
                    documentsAll = docDAO.sortDocsBy("category", true); //category ASC
                    break;
                case 6:
                    documentsAll = docDAO.sortDocsBy("category", true); //category DESC
                    break;
                default:
                    break;
            }
            for (Doc doc : documentsAll) {
                String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(doc.getCreationDate());
                docs.add(new DocDTO(doc.getDocId(), doc.getTitle(), doc.getCategory(), formattedDate,  String.format("%.2f", ((double)doc.getSize()/1024)).toString()));
            }
        } else if (docTitle.isPresent() && !docTitle.get().equals("")) {
            documentsAll = new ArrayList<Doc>();
            System.out.println(docTitle.get());
            Doc foundDoc = docDAO.getDocByTitle(docTitle.get().replaceAll("\\+", " "));
            if (foundDoc != null) {
                documentsAll.add(foundDoc);
                String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(foundDoc.getCreationDate());
                docs.add(new DocDTO(foundDoc.getDocId(), foundDoc.getTitle(), foundDoc.getCategory(), formattedDate,  String.format("%.2f", ((double)foundDoc.getSize()/1024)).toString()));
            } else {
                documentsAll = null;
            }
        } else {
            documentsAll = docDAO.getAllDocs();
            for (Doc doc : documentsAll) {
                String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(doc.getCreationDate());
                docs.add(new DocDTO(doc.getDocId(), doc.getTitle(), doc.getCategory(), formattedDate,  String.format("%.2f", ((double)doc.getSize()/1024)).toString()));
            }
        }
        model.addAttribute("listaDokumenata", docs);
        return "manage";
    }

    @RequestMapping("/sort")
    public String sortDocsBy() {
        List<Doc> documentsAll = docDAO.sortDocsBy("category", true);
        for (Doc doc : documentsAll) {
            //System.out.println(doc.getCategory());
        }
        return "home";
    }

    @RequestMapping("/filter")
    public String filterDocs() {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DATE, -3);
        Date prekjuce = calendar1.getTime();
        Calendar calendar2 = Calendar.getInstance();
        calendar2.add(Calendar.DATE, -1);
        Date juce = calendar2.getTime();
        //List<Doc> documentsAll = docDAO.filterDocs("crossing", true, true, true, "knjige", Long.valueOf(1000), Long.valueOf(1500), prekjuce, juce);
        return "home";
    }

    public Model setModelParamAttributesForSearchPage(Model model, Optional<String> paramSearchQuery, Optional<String> paramSearchContent, Optional<String> paramSearchTitle,
            Optional<String> paramSearchId,Optional<String> paramType, String type, Optional<String> paramCategory, String category, Optional<Integer> paramMinSize, Optional<Integer> paramMaxSize,
            Optional<String> paramMinDate, Optional<String> paramMaxDate, Optional<Integer> paramSort) {

        if (paramSearchQuery.isPresent()) {
            model.addAttribute("searchQuery", paramSearchQuery.get());
        }
        if (paramSearchContent.isPresent()) {
            model.addAttribute("searchContent", true);
        }
        if (paramSearchTitle.isPresent()) {
            model.addAttribute("searchTitle", true);
        }
        if (paramSearchId.isPresent()) {
            model.addAttribute("searchId", true);
        }
        if (paramType.isPresent()) {
            model.addAttribute("type", type);
        }
        if (paramCategory.isPresent()) {
            if(category.equals("diplomski") || category.equals("seminarski") ||category.equals("master")){
            category += " radovi";
            } else if (category.equals("laboratorijske")){
                category += " vežbe";
            }
            model.addAttribute("category", category);
        }
        if (paramMinSize.isPresent()) {
            model.addAttribute("minSize", paramMinSize.get());
        }
        if (paramMaxSize.isPresent()) {
            model.addAttribute("maxSize", paramMaxSize.get());
        }
        if (paramMinDate.isPresent()) {
            model.addAttribute("minDate", paramMinDate.get());
        }
        if (paramMaxDate.isPresent()) {
            model.addAttribute("maxDate", paramMaxDate.get());
        }
        if (paramSort.isPresent()) {
            model.addAttribute("sort", paramSort.get());
        }
        return model;
    }

    public ArrayList<Category> dodajKategorije() {
        ArrayList<Category> kategorije = new ArrayList<Category>();
        kategorije.add(new Category(0, "knjige"));
        kategorije.add(new Category(1, "laboratorijske vežbe"));
        kategorije.add(new Category(2, "blanketi"));
        kategorije.add(new Category(3, "seminarski radovi"));
        kategorije.add(new Category(4, "diplomski radovi"));
        kategorije.add(new Category(5, "master radovi"));
        return kategorije;
    }

    public ArrayList<Category> dodajKategorijeZaSearchStranu() {
        ArrayList<Category> kategorije = new ArrayList<Category>();
        kategorije.add(new Category(0, "sve"));
        kategorije.add(new Category(1, "knjige"));
        kategorije.add(new Category(2, "laboratorijske vežbe"));
        kategorije.add(new Category(3, "blanketi"));
        kategorije.add(new Category(4, "seminarski radovi"));
        kategorije.add(new Category(5, "diplomski radovi"));
        kategorije.add(new Category(6, "master radovi"));
        return kategorije;
    }

      public ArrayList<Category> dodajTipovePretrageZaSearchStranu() {
        ArrayList<Category> kategorije = new ArrayList<Category>();
        kategorije.add(new Category(0, "eksplicitna"));
        kategorije.add(new Category(1, "osnovna"));
        kategorije.add(new Category(2, "frazna"));
        return kategorije;
    }

    public ArrayList<Category> dodajSortiranja() {
        ArrayList<Category> sortiranja = new ArrayList<Category>();
        sortiranja.add(new Category(1, "veličini dokumenta - rastuće"));
        sortiranja.add(new Category(2, "veličini dokumenta - opadajuće"));
        sortiranja.add(new Category(3, "datumu kreiranja - rastuće"));
        sortiranja.add(new Category(4, "datumu kreiranja - opadajuće"));
        sortiranja.add(new Category(5, "kategoriji dokumenta - rastuće"));
        sortiranja.add(new Category(6, "kategoriji dokumenta - opadajuće"));
        return sortiranja;
    }

    public ArrayList<Category> dodajSortiranjaZaSearchStranu() {
        ArrayList<Category> sortiranja = new ArrayList<Category>();
        sortiranja.add(new Category(0, "nesortirani prikaz"));
        sortiranja.add(new Category(1, "veličini dokumenta - rastuće"));
        sortiranja.add(new Category(2, "veličini dokumenta - opadajuće"));
        sortiranja.add(new Category(3, "datumu kreiranja - rastuće"));
        sortiranja.add(new Category(4, "datumu kreiranja - opadajuće"));
        sortiranja.add(new Category(5, "kategoriji dokumenta - rastuće"));
        sortiranja.add(new Category(6, "kategoriji dokumenta - opadajuće"));
        return sortiranja;
    }
}
