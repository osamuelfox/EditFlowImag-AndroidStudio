package com.editflowimg.backend.dto.generation;

import java.util.List;

public class AiResult {
    private List<String> analysisItems;
    private List<String> changeItems;
    private String descriptionText;

    public AiResult() {
    }

    public AiResult(List<String> analysisItems, List<String> changeItems, String descriptionText) {
        this.analysisItems = analysisItems;
        this.changeItems = changeItems;
        this.descriptionText = descriptionText;
    }

    public List<String> getAnalysisItems() { return analysisItems; }
    public void setAnalysisItems(List<String> analysisItems) { this.analysisItems = analysisItems; }

    public List<String> getChangeItems() { return changeItems; }
    public void setChangeItems(List<String> changeItems) { this.changeItems = changeItems; }

    public String getDescriptionText() { return descriptionText; }
    public void setDescriptionText(String descriptionText) { this.descriptionText = descriptionText; }
}