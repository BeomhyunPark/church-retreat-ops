package com.gmc.retreat.community.mapper;

public class ChurchMiddleGroupUpsert {
    private Long id;
    private final String name;
    private final String elderName;
    private final String description;
    private final Integer displayOrder;

    public ChurchMiddleGroupUpsert(
            Long id,
            String name,
            String elderName,
            String description,
            Integer displayOrder
    ) {
        this.id = id;
        this.name = name;
        this.elderName = elderName;
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

    public String getElderName() {
        return elderName;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }
}
