public abstract class MediaItem {
    protected String title;
    protected int    year;
    protected String genre;
    public    int    rating = 0;
    public    String watchDate = "";   // ταινία / σειρά: ημ. παρακολούθησης

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
