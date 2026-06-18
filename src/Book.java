public class Book extends MediaItem {
    private String author;
    private int pages;
    private String publisher = "";
    private String startDate = "";
    private String endDate = "";

    public Book(String title, int year, String genre, String author, int pages) {
        super(title, year, genre);
        setAuthor(author);
        setPages(pages);
    }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author == null ? "" : author.trim(); }

    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = Math.max(0, pages); }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher == null ? "" : publisher.trim(); }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate == null ? "" : startDate.trim(); }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate == null ? "" : endDate.trim(); }

    @Override public String getType() { return "Βιβλιο"; }

    @Override
    public String getInfo() {
        return "Βιβλιο: " + getTitle() + " | Ετος: " + getYear() + " | Ειδος: " + getGenre()
                + " | Συγγραφεας: " + author + " | Σελιδες: " + pages
                + (publisher.isEmpty() ? "" : " | Εκδοτης: " + publisher);
    }
}
