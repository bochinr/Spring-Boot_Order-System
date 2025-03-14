package com.example.demo1.Entity;

import lombok.Data;

@Data
public class food {
    private int id;         // 菜品ID
    private int price;      // 价格
    private int level;      // 等级
    private String name;    // 菜品名称
    private String info;    // 菜品描述

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
