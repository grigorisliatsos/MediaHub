import java.util.*;

public abstract class MediaItem {
    protected String title;
    protected int    year;
    protected String genre;
    public    int    rating = 0;
    public    String watchDate = "";   // ταινία / σειρά: ημ. παρακολούθησης
    public    String imagePath = "";   // διαδρομή εικόνας (προαιρετικό, κοινό για όλους τους τύπους)
    public    String notes     = "";   // προσωπικές σημειώσεις / σχόλια / κριτική

    // Νέα κοινά πεδία
    public    String status   = "";          // "", pending, in_progress, completed, abandoned
    public    boolean favorite = false;       // αγαπημένο
    public    List<String> tags = new ArrayList<>();  // ελεύθερες ετικέτες
    public    long    added   = 0L;           // timestamp προσθήκης (για ταξινόμηση/στατιστικά)

    public MediaItem(String title, int year, String genre) {
        this.title = title;
        this.year  = year;
        this.genre = genre;
    }

    public abstract String getInfo();
    public abstract String getType();

    public String getTitle() { return title; }
    public int    getYear()  { return year; }
    public String getGenre() { return genre; }
}
