public class Series extends MediaItem {
    public int    seasons;
    public String actors;
    public String startDate = "";   // ημερομηνια εναρξης παρακολουθησης
    public String endDate   = "";   // ημερομηνια ολοκληρωσης

    public Series(String title, int year, String genre, int seasons, String actors) {
        super(title, year, genre);
        this.seasons = seasons;
        this.actors  = actors;
    }

    @Override public String getType() { return "Σειρα"; }

    @Override
    public String getInfo() {
        return "Σειρα: " + title + " | Ετος: " + year + " | Ειδος: " + genre
             + " | Σεζον: " + seasons + " | Ηθοποιοι: " + actors;
    }
}
