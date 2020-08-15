package com.example.elasticsearch.model;

public class Category {

    private int value;
    private String name;

    public Category(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public Category() {

    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

}
