package com.examly.springapp.dto;

import java.util.List;

public class RiskAssessmentDTO {
    
    private int score;
    private String riskLevel;
    private List<String> explanations;
    
    public RiskAssessmentDTO(int score, String riskLevel, List<String> explanations) {
        this.score = score;
        this.riskLevel = riskLevel;
        this.explanations = explanations;
    }

    public RiskAssessmentDTO() {}

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getExplanations() {
        return explanations;
    }

    public void setExplanations(List<String> explanations) {
        this.explanations = explanations;
    }
}
