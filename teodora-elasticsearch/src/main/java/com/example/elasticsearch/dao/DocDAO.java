package com.example.elasticsearch.dao;

import com.example.elasticsearch.model.Doc;
import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Page;

public interface DocDAO {

    Doc getDocById(String docId);

    Doc getDocByTitle(String title);

    List<Doc> getFirst10Docs();

    List<Doc> getAllDocs();

    Doc addNewDoc(Doc doc);

    boolean deleteDoc(String docId);

    Page<Doc> getPageOfDocs(int page_number, int page_size);

    int getTotalNumberOfDocuments();

    Page<Doc> getPageOfFilteredDocs(int page_number, int page_size, String searchQuery,
            boolean searchContent, boolean searchTitle, boolean searchId,
            String category, long sizeMin, long sizeMax, Date dateMin, Date dateMax,
            String sortField, boolean ASCorDESC);

    int getTotalNumberOfFilteredDocs(String searchQuery,
            boolean searchContent, boolean searchTitle, boolean searchId,
            String category, long sizeMin, long sizeMax, Date dateMin, Date dateMax,
            String sortField, boolean ASCorDESC);

    List<Doc> sortDocsBy(String field, boolean ASCorDESC);

    int getNumberOfDocsByCategory(String category);
}
