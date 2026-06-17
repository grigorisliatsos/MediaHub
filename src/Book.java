public class Book extends MediaItem {
    public String author;
    public int    pages;
    public String publisher = "";   // εκδοτης
    public String startDate = "";   // ημερομηνια εναρξης
    public String endDate   = "";   // ημερομηνια τελους

    public Book(String title, int year, String genre, String author, int pages) {
        super(title, year, genre);
        this.author = author;
        this.pages  = pages;
    }

    @Override public String getType() { return "Βιβλιο"; }

    @Override
    public String getInfo() {
        return "Βιβλιο: " + title + " | Ετος: " + year + " | Ειδος: " + genre
                + " | Συγγραφεας: " + author + " | Σελιδες: " + pages;
    }
}