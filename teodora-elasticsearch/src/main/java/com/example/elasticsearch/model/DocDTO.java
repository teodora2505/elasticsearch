package com.example.elasticsearch.model;

public class DocDTO {

    private String docId;
    private String title;
    private String category;
    private String creationDate;
    private long size;

    public DocDTO(String docId, String title, String category, String creationDate, long size) {
        this.docId = docId;
        this.title = title;
        this.category = category;
        this.creationDate = creationDate;
        this.size = size;
    }

    public DocDTO() {

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

    public String getCreationDate() {
        return creationDate;
    }

    public long getSize() {
        return size;
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

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
