package org.news.api.rss;

public class TrailerItem {

    String  id;
    String title;
    String imgUrl;
    String trailerUrl;
    String description;
    public TrailerItem() {}
    public TrailerItem(String title, String imgUrl, String trailerUrl, String description) {
        this.title = title;
        this.imgUrl = imgUrl;
        this.trailerUrl = trailerUrl;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Title: " + title +
                "\nImage: " + imgUrl +
                "\nTrailer: " + trailerUrl +
                "\nDescription: " + description + "\n";
    }
}