package com.ashcollege.entities;

import java.util.List;

public class OutfitSuggestion {
    private List<OutfitItem> outfit;
    private String explanation;

    public OutfitSuggestion(List<OutfitItem> outfit, String explanation) {
        this.outfit = outfit;
        this.explanation = explanation;
    }

    public List<OutfitItem> getOutfit() {
        return outfit;
    }

    public void setOutfits(List<OutfitItem> outfit) {
        this.outfit = outfit;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }


    @Override
    public String toString() {
        return "OutfitSuggestion{" +
                "outfit=" + outfit +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
