package com.example.elasticsearch.dao;

import com.example.elasticsearch.model.Doc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocRepository extends ElasticsearchRepository<Doc, String> {
}
