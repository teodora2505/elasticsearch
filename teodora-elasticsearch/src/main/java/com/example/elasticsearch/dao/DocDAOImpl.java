package com.example.elasticsearch.dao;

import com.example.elasticsearch.model.Doc;
import java.util.Date;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Repository
public class DocDAOImpl implements DocDAO {

    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final Integer MAX_DOCS = 10000;

    @Value("${elasticsearch.index.name}")
    private String indexName;

    @Value("${elasticsearch.user.type}")
    private String docTypeName;

    @Autowired
    private ElasticsearchTemplate esTemplate;

    @Override
    public List<Doc> getFirst10Docs() {
        SearchQuery getAllQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .build();
        return esTemplate.queryForList(getAllQuery, Doc.class);
    }

    @Override
    public List<Doc> getAllDocs() {
        SearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .build()
                .setPageable(new PageRequest(0, MAX_DOCS));
        return esTemplate.queryForPage(query, Doc.class).getContent();
    }

    @Override
    public Doc getDocById(String docId) {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.matchQuery("docId", docId))
                .build();
        List<Doc> docs = esTemplate.queryForList(searchQuery, Doc.class);
        if (!docs.isEmpty()) {
            return docs.get(0);
        }
        return null;
    }

    @Override
    public Doc getDocByTitle(String title) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.matchQuery("title", title));
        SearchQuery query = new NativeSearchQueryBuilder().withQuery(boolQuery)
                .build();
        List<Doc> docs = esTemplate.queryForList(query, Doc.class);
        if (!docs.isEmpty()) {
            return docs.get(0);
        }
        return null;
    }

    @Override
    public Doc addNewDoc(Doc doc) {

        IndexQuery docQuery = new IndexQuery();
        docQuery.setIndexName(indexName);
        docQuery.setType(docTypeName);
        docQuery.setObject(doc);

        LOG.info("Doc indexed: {}", esTemplate.index(docQuery));
        esTemplate.refresh(indexName);

        return doc;
    }

    @Override
    public boolean deleteDoc(String docId) {
        if (this.getDocById(docId) != null) {
            esTemplate.delete(Doc.class, docId);
            esTemplate.refresh(indexName);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Page<Doc> getPageOfDocs(int page_number, int page_size) {
        Page<Doc> page = null;
        SearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .build()
                .setPageable(new PageRequest(page_number, page_size));
        page = esTemplate.queryForPage(query, Doc.class);
        return page;
    }

    @Override
    public int getTotalNumberOfDocuments() {
        Page<Doc> page = null;
        SearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .build()
                .setPageable(new PageRequest(0, MAX_DOCS));
        return esTemplate.queryForPage(query, Doc.class).getContent().size();
    }

    public Page<Doc> getPageOfFilteredDocs(int page_number, int page_size, String searchQuery,
            boolean searchContent, boolean searchTitle, boolean searchId, String searchType,
            String category, long sizeMin, long sizeMax, Date dateMin, Date dateMax,
            String sortField, boolean ASCorDESC) {
        //ASCorDESC :asc-true, desc-false
        //searchType: "eksplicitna" | "osnovna" | "frazna"
        Page<Doc> page = null;
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        SortOrder order = (ASCorDESC) ? SortOrder.ASC : SortOrder.DESC;

        if (searchQuery != null && (searchContent || searchTitle || searchId)) {
            searchQuery = searchQuery.toLowerCase();
            if (searchType.equals("eksplicitna")) { // trazi striktno uneti jedan termin (termQuery upiti)
                if (searchContent && searchTitle && searchId) {
                    boolQuery.should(QueryBuilders.termQuery("content", searchQuery))
                            .should(QueryBuilders.termQuery("title", searchQuery))
                            .should(QueryBuilders.termQuery("docId", searchQuery));
                } else if (searchContent && searchTitle && !searchId) {
                    boolQuery.should(QueryBuilders.termQuery("content", searchQuery))
                            .should(QueryBuilders.termQuery("title", searchQuery));
                } else if (searchContent && searchId && !searchTitle) {
                    boolQuery.should(QueryBuilders.termQuery("content", searchQuery))
                            .should(QueryBuilders.termQuery("docId", searchQuery));
                } else if (searchTitle && searchId && !searchContent) {
                    boolQuery.should(QueryBuilders.termQuery("title", searchQuery))
                            .should(QueryBuilders.termQuery("docId", searchQuery));
                } else if (searchContent && !searchTitle && !searchId) { //sadrzaj
                    boolQuery.must(QueryBuilders.termQuery("content", searchQuery));
                } else if (searchTitle && !searchContent && !searchId) { //naslov
                    boolQuery.must(QueryBuilders.termQuery("title", searchQuery));
                } else if (searchId && !searchContent && !searchTitle) { //id
                    boolQuery.must(QueryBuilders.termQuery("docId", searchQuery));
                }
            } else if (searchType.equals("osnovna")) { //trazi pojavu vise unetih termina, moze da se javi jedan od njih, vise njih, svi... a moze i da nadje pojavu 1. termina u jednom dokumentu, pojavu 2. termina u 2. dokumentu...
                if (searchContent && searchTitle && searchId) {
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("content").field("title").field("docId"));
                } else if (searchContent && searchTitle && !searchId) {
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("content").field("title"));
                } else if (searchContent && searchId && !searchTitle) {
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("content").field("docId"));
                } else if (searchTitle && searchId && !searchContent) {
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("docId").field("title"));
                } else if (searchContent && !searchTitle && !searchId) { //sadrzaj
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("content"));
                } else if (searchTitle && !searchContent && !searchId) { //naslov
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("title"));
                } else if (searchId && !searchContent && !searchTitle) { //id
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("docId"));
                }
            } else if (searchType.equals("frazna")) { //trazi unetu frazu "veliki grad" nacice "Beograd je veliki grad", ali nece naci "Beograd, veliki je to grad"...
                if (searchContent && searchTitle && searchId) {
                    boolQuery.should(QueryBuilders.matchPhraseQuery("content", searchQuery))
                            .should(QueryBuilders.matchPhraseQuery("title", searchQuery))
                            .should(QueryBuilders.matchPhraseQuery("docId", searchQuery));
                } else if (searchContent && searchTitle && !searchId) {
                    boolQuery.should(QueryBuilders.matchPhraseQuery("content", searchQuery))
                            .should(QueryBuilders.matchPhraseQuery("title", searchQuery));
                } else if (searchContent && searchId && !searchTitle) {
                    boolQuery.should(QueryBuilders.matchPhraseQuery("content", searchQuery))
                            .should(QueryBuilders.matchPhraseQuery("docId", searchQuery));
                } else if (searchTitle && searchId && !searchContent) {
                    boolQuery.should(QueryBuilders.matchPhraseQuery("title", searchQuery))
                            .should(QueryBuilders.matchPhraseQuery("docId", searchQuery));
                } else if (searchContent && !searchTitle && !searchId) { //sadrzaj
                    boolQuery.should(QueryBuilders.matchPhraseQuery("content", searchQuery));
                } else if (searchTitle && !searchContent && !searchId) { //naslov
                    boolQuery.should(QueryBuilders.matchPhraseQuery("title", searchQuery));
                } else if (searchId && !searchContent && !searchTitle) { //id
                    boolQuery.should(QueryBuilders.matchPhraseQuery("docId", searchQuery));
                }
            }
        }

        boolQuery.must(QueryBuilders.rangeQuery("size").gte(sizeMin * 1024))
                .must(QueryBuilders.rangeQuery("size").lte(sizeMax * 1024));

        if (dateMin != null) {
            boolQuery.must(QueryBuilders.rangeQuery("creationDate").gte(dateMin.getTime()));
        }

        if (dateMax != null) {
            boolQuery.must(QueryBuilders.rangeQuery("creationDate").lte(dateMax.getTime()));
        }

        if (category != null && !category.equals("sve")) {
            boolQuery.filter(QueryBuilders.termQuery("category", category.split("\\s+")[0]));
        }

        SearchQuery query;
        if (sortField != "") {
            query = new NativeSearchQueryBuilder().withQuery(boolQuery)
                    .withSort(SortBuilders.fieldSort(sortField).order(order))
                    .withFilter(boolQuery).build().setPageable(new PageRequest(page_number, page_size));
        } else {
            query = new NativeSearchQueryBuilder().withQuery(boolQuery)
                    .withFilter(boolQuery).build().setPageable(new PageRequest(page_number, page_size));
        }

        page = esTemplate.queryForPage(query, Doc.class);
        return page;
    }

    @Override
    public int getTotalNumberOfFilteredDocs(String searchQuery, boolean searchContent,
            boolean searchTitle, boolean searchId, String searchType, String category,
            long sizeMin, long sizeMax, Date dateMin, Date dateMax,
            String sortField, boolean ASCorDESC) { //asc-true, desc-false

        Page<Doc> page = null;
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        SortOrder order = (ASCorDESC) ? SortOrder.ASC : SortOrder.DESC;

        if (searchQuery != null && (searchContent || searchTitle || searchId)) {
            searchQuery = searchQuery.toLowerCase();
            if (searchType.equals("eksplicitna")) { // trazi striktno uneti jedan termin (termQuery upiti)
                if (searchContent && searchTitle && searchId) {
                    boolQuery.should(QueryBuilders.termQuery("content", searchQuery))
                            .should(QueryBuilders.termQuery("title", searchQuery))
                            .should(QueryBuilders.termQuery("docId", searchQuery));
                } else if (searchContent && searchTitle && !searchId) {
                    boolQuery.should(QueryBuilders.termQuery("content", searchQuery))
                            .should(QueryBuilders.termQuery("title", searchQuery));
                } else if (searchContent && searchId && !searchTitle) {
                    boolQuery.should(QueryBuilders.termQuery("content", searchQuery))
                            .should(QueryBuilders.termQuery("docId", searchQuery));
                } else if (searchTitle && searchId && !searchContent) {
                    boolQuery.should(QueryBuilders.termQuery("title", searchQuery))
                            .should(QueryBuilders.termQuery("docId", searchQuery));
                } else if (searchContent && !searchTitle && !searchId) { //sadrzaj
                    boolQuery.must(QueryBuilders.termQuery("content", searchQuery));
                } else if (searchTitle && !searchContent && !searchId) { //naslov
                    boolQuery.must(QueryBuilders.termQuery("title", searchQuery));
                } else if (searchId && !searchContent && !searchTitle) { //id
                    boolQuery.must(QueryBuilders.termQuery("docId", searchQuery));
                }
            } else if (searchType.equals("osnovna")) { //trazi pojavu vise unetih termina, moze da se javi jedan od njih, vise njih, svi... a moze i da nadje pojavu 1. termina u jednom dokumentu, pojavu 2. termina u 2. dokumentu...
                if (searchContent && searchTitle && searchId) {
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("content").field("title").field("docId"));
                } else if (searchContent && searchTitle && !searchId) {
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("content").field("title"));
                } else if (searchContent && searchId && !searchTitle) {
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("content").field("docId"));
                } else if (searchTitle && searchId && !searchContent) {
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("docId").field("title"));
                } else if (searchContent && !searchTitle && !searchId) { //sadrzaj
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("content"));
                } else if (searchTitle && !searchContent && !searchId) { //naslov
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("title"));
                } else if (searchId && !searchContent && !searchTitle) { //id
                    boolQuery.should(QueryBuilders.simpleQueryStringQuery(searchQuery).field("docId"));
                }
            } else if (searchType.equals("frazna")) { //trazi unetu frazu "veliki grad" nacice "Beograd je veliki grad", ali nece naci "Beograd, veliki je to grad"...
                if (searchContent && searchTitle && searchId) {
                    boolQuery.should(QueryBuilders.matchPhraseQuery("content", searchQuery))
                            .should(QueryBuilders.matchPhraseQuery("title", searchQuery))
                            .should(QueryBuilders.matchPhraseQuery("docId", searchQuery));
                } else if (searchContent && searchTitle && !searchId) {
                    boolQuery.should(QueryBuilders.matchPhraseQuery("content", searchQuery))
                            .should(QueryBuilders.matchPhraseQuery("title", searchQuery));
                } else if (searchContent && searchId && !searchTitle) {
                    boolQuery.should(QueryBuilders.matchPhraseQuery("content", searchQuery))
                            .should(QueryBuilders.matchPhraseQuery("docId", searchQuery));
                } else if (searchTitle && searchId && !searchContent) {
                    boolQuery.should(QueryBuilders.matchPhraseQuery("title", searchQuery))
                            .should(QueryBuilders.matchPhraseQuery("docId", searchQuery));
                } else if (searchContent && !searchTitle && !searchId) { //sadrzaj
                    boolQuery.should(QueryBuilders.matchPhraseQuery("content", searchQuery));
                } else if (searchTitle && !searchContent && !searchId) { //naslov
                    boolQuery.should(QueryBuilders.matchPhraseQuery("title", searchQuery));
                } else if (searchId && !searchContent && !searchTitle) { //id
                    boolQuery.should(QueryBuilders.matchPhraseQuery("docId", searchQuery));
                }
            }
        }

        boolQuery.must(QueryBuilders.rangeQuery("size").gte(sizeMin * 1024))
                .must(QueryBuilders.rangeQuery("size").lte(sizeMax * 1024));

        if (dateMin != null) {
            boolQuery.must(QueryBuilders.rangeQuery("creationDate").gte(dateMin.getTime()));
        }

        if (dateMax != null) {
            boolQuery.must(QueryBuilders.rangeQuery("creationDate").lte(dateMax.getTime()));
        }

        if (category != null && !category.equals("sve")) {
            boolQuery.filter(QueryBuilders.termQuery("category", category.split("\\s+")[0]));
        }

        SearchQuery query;
        if (sortField != "") {
            query = new NativeSearchQueryBuilder().withQuery(boolQuery)
                    .withSort(SortBuilders.fieldSort(sortField).order(order))
                    .withFilter(boolQuery).build().setPageable(new PageRequest(0, MAX_DOCS));
        } else {
            query = new NativeSearchQueryBuilder().withQuery(boolQuery)
                    .withFilter(boolQuery).build().setPageable(new PageRequest(0, MAX_DOCS));
        }

        return esTemplate.queryForPage(query, Doc.class).getContent().size();
    }

    @Override
    public List<Doc> sortDocsBy(String field, boolean ASCorDESC) { //asc-true, desc-false
        SortOrder order = (ASCorDESC) ? SortOrder.ASC : SortOrder.DESC;
        SearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withSort(SortBuilders.fieldSort(field).order(order))
                .build()
                .setPageable(new PageRequest(0, MAX_DOCS));
        return esTemplate.queryForPage(query, Doc.class).getContent();
    }

    @Override
    public int getNumberOfDocsByCategory(String category) {

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (category != null) {
            boolQuery.filter(QueryBuilders.termQuery("category", category.split("\\s+")[0]));
        }

        SearchQuery query = new NativeSearchQueryBuilder().withQuery(boolQuery)
                .withFilter(boolQuery).build().setPageable(new PageRequest(0, MAX_DOCS));

        return esTemplate.queryForPage(query, Doc.class).getContent().size();
    }

}
