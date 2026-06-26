package com.gmc.retreat.retreat.mapper;

public class RetreatGroupUpsert {
    private Long id;
    private final String name;
    private final String description;
    private final Integer displayOrder;

    public RetreatGroupUpsert(Long id, String name, String description, Integer displayOrder) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }
}
