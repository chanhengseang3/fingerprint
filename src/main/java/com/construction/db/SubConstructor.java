package com.construction.db;

public class SubConstructor {

    private int id;
    private String name;
    private String base64;

    public int getId() {
        return id;
    }

    public SubConstructor setId(int id) {
        this.id = id;
        return this;
    }

    public String getBase64() {
        return base64;
    }

    public SubConstructor setBase64(String base64) {
        this.base64 = base64;
        return this;
    }

    public String getName() {
        return name;
    }

    public SubConstructor setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        return "ID: " + id + ", Name: " + name;
    }
}
