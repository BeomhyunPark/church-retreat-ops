package com.gmc.retreat.community.mapper;

public class ChurchCellUpsert {
    private Long id;
    private final Long middleGroupId;
    private final String name;
    private final String cellLeaderName;
    private final String description;
    private final Integer displayOrder;

    public ChurchCellUpsert(
            Long id,
            Long middleGroupId,
            String name,
            String cellLeaderName,
            String description,
            Integer displayOrder
    ) {
        this.id = id;
        this.middleGroupId = middleGroupId;
        this.name = name;
        this.cellLeaderName = cellLeaderName;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMiddleGroupId() {
        return middleGroupId;
    }

    public String getName() {
        return name;
    }

    public String getCellLeaderName() {
        return cellLeaderName;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }
}
