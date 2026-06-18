import java.util.*;

public abstract class MediaItem {
    private String title;
    private int year;
    private String genre;
    private int rating = 0;
    private String watchDate = "";
    private String imagePath = "";
    private String notes = "";
    private Status status = Status.NONE;
    private boolean favorite = false;
    private List<String> tags = new ArrayList<>();
    private long added = 0L;

    public MediaItem(String title, int year, String genre) {
        setTitle(title);
        setYear(year);
        setGenre(genre);
        this.added = System.currentTimeMillis();
    }

    public abstract String getInfo();
    public abstract String getType();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null ? "" : title.trim(); }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre == null ? "" : genre.trim(); }

    public int getRating() { return rating; }
    public void setRating(int rating) {
        if (rating < 0) rating = 0;
        if (rating > 5) rating = 5;
        this.rating = rating;
    }

    public String getWatchDate() { return watchDate; }
    public void setWatchDate(String watchDate) { this.watchDate = watchDate == null ? "" : watchDate.trim(); }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath == null ? "" : imagePath.trim(); }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes == null ? "" : notes.trim(); }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status == null ? Status.NONE : status; }
    public String getStatusKey() { return status.getKey(); }
    public void setStatusKey(String key) { this.status = Status.fromKey(key); }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public void toggleFavorite() { this.favorite = !this.favorite; }

    public List<String> getTags() { return new ArrayList<>(tags); }
    public void setTags(List<String> tags) {
        this.tags = new ArrayList<>();
        if (tags == null) return;
        for (String tag : tags) addTag(tag);
    }
    public void addTag(String tag) {
        if (tag == null) return;
        String cleaned = tag.trim();
        if (!cleaned.isEmpty() && !tags.contains(cleaned)) tags.add(cleaned);
    }
    public void removeTag(String tag) {
        if (tag == null) return;
        tags.remove(tag.trim());
    }
    public void clearTags() { tags.clear(); }

    public long getAdded() { return added; }
    public void setAdded(long added) { this.added = added; }

    @Override
    public String toString() {
        return getType() + ": " + title + " (" + year + ")";
    }
}
