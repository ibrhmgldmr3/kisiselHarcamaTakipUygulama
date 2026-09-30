package com.expenseapp.model;

public class Category {

    private Long id;
    private Long userId;
    private String name;

    public Category() {
    }

    public Category(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** ComboBox gibi JavaFX kontrollerinde kategori adının görünmesi için. */
    @Override
    public String toString() {
        return name;
    }
}
