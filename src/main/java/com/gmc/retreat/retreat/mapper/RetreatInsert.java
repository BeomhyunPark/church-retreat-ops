package com.gmc.retreat.retreat.mapper;

import java.time.LocalDate;

public class RetreatInsert {
    private Long id;
    private final String name;
    private final LocalDate startsOn;
    private final LocalDate endsOn;

    public RetreatInsert(String name, LocalDate startsOn, LocalDate endsOn) {
        this.name = name;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
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

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }
}
