package com.example.elasticsearch.dao;

import com.example.elasticsearch.model.Doc;
import java.util.List;

public interface DocDAO {

    List<Doc> getAllDocs();

    Doc getDocById(String docId);

    Doc addNewDoc(Doc doc);
    //Object getAllUserSettings(String userId);
    //String getUserSetting(String userId, String key);
    //String addUserSetting(String userId, String key, String value);
}
