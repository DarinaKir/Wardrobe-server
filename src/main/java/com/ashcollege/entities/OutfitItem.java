package com.ashcollege.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OutfitItem {
    private int id;
    private User user;
    private String name;
    private String type;
    private String style;
    private String color;
    private String season;
    private String description;

    public OutfitItem() {
    }

    public OutfitItem(int id) {
        this.id = id;
    }

    public OutfitItem(int id, String type, String style, String color, String season, String description) {
        this.id = id;
        this.type = type;
        this.style = style;
        this.color = color;
        this.season = season;
        this.description = description;
    }

    public OutfitItem(User user, String name, String type, String style, String color, String season, String description) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.style = style;
        this.color = color;
        this.season = season;
        this.description = description;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }


    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> getSeasonArray(String season) {
        List<String> seasons = new ArrayList<>();
        if (season.equals("all") || season.equals("all season")) {
            seasons = List.of("winter", "spring", "summer", "fall");
        }else {
            String[] splitSeasons = season.split("/");
            seasons = Arrays.asList(splitSeasons);
        }
        return seasons;
    }

    @Override
    public String toString() {
        return "OutfitItem{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", style='" + style + '\'' +
                ", color='" + color + '\'' +
                ", season=" + season +
                ", description='" + description + '\'' +
                '}';
    }
}