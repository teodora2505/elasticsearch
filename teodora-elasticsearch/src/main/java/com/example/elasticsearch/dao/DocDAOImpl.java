package com.example.elasticsearch.dao;

import com.example.elasticsearch.model.Doc;
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

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

@Repository
public class DocDAOImpl implements DocDAO {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Value("${elasticsearch.index.name}")
    private String indexName;

    @Value("${elasticsearch.user.type}")
    private String docTypeName;

    @Autowired
    private ElasticsearchTemplate esTemplate;

    @Override
    public List<Doc> getAllDocs() {
        SearchQuery getAllQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery()).build();
        return esTemplate.queryForList(getAllQuery, Doc.class);
    }

    @Override
    public Doc getDocById(String docId) {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.matchQuery("docId", docId)).build();
        List<Doc> docs = esTemplate.queryForList(searchQuery, Doc.class);
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
}
/*
    @Override
    public Object getAllUserSettings(String name) {

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("name", name)).build();

        List<User> users = esTemplate.queryForList(searchQuery, User.class);
        if(!users.isEmpty()) {
            System.out.println("settings: "+users.get(0).getUserSettings().toString());
            return users.get(0).getUserSettings();
        }

        return null;
    }

    @Override
    public String getUserSetting(String name, String key) {

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("name", name)).build();
        List<User> users = esTemplate.queryForList(searchQuery, User.class);
        if(!users.isEmpty()) {
            return users.get(0).getUserSettings().get(key);
        }

        return null;
    }

    @Override
    public String addUserSetting(String name, String key, String value) {

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("name", name)).build();
        List<User> users = esTemplate.queryForList(searchQuery, User.class);
        if(!users.isEmpty()) {

            User user = users.get(0);
            user.getUserSettings().put(key, value);

            IndexQuery userQuery = new IndexQuery();
            userQuery.setIndexName(indexName);
            userQuery.setType(userTypeName);
            userQuery.setId(user.getUserId());
            userQuery.setObject(user);
            esTemplate.index(userQuery);
            return "Setting added.";
        }

        return null;
    }*/
