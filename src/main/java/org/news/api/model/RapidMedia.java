package org.news.api.model;

import java.util.List;

public class RapidMedia {

    private String description;
    private List<Recommendations> recommendations;
    private String title;

    public RapidMedia() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Recommendations> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recommendations> recommendations) {
        this.recommendations = recommendations;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "RapidMedia{" +
                "description='" + description + '\'' +
                ", recommendations=" + recommendations +
                ", title='" + title + '\'' +
                '}';
    }
}
