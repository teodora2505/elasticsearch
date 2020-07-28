package com.example.elasticsearch.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import java.util.Date;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "doc_index", type = "doc")
public class Doc {

    @Id
    private String docId;
    private String title;
    private String category;
    private String content;
    @Field(type = FieldType.Date, store = true, format = DateFormat.custom, pattern = "dd-MM-yyyy")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date creationDate = new Date();
    private long size;
    private String location;

    public Doc(String docId, String title, String category, String content, Date creationDate, long size, String location) {
        this.docId = docId;
        this.title = title;
        this.category = category;
        this.content = content;
        this.creationDate = creationDate;
        this.size = size;
        this.location = location;
    }

    public Doc() {

    }

    public String getDocId() {
        return docId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getContent() {
        return content;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String getLocation() {
        return location;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setLocation(String location) {
        this.location = location;
    }

}
