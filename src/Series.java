public class Series extends MediaItem {
    private int seasons;
    private String actors;
    private String startDate = "";
    private String endDate = "";

    public Series(String title, int year, String genre, int seasons, String actors) {
        super(title, year, genre);
        setSeasons(seasons);
        setActors(actors);
    }

    public int getSeasons() { return seasons; }
    public void setSeasons(int seasons) { this.seasons = Math.max(0, seasons); }

    public String getActors() { return actors; }
    public void setActors(String actors) { this.actors = actors == null ? "" : actors.trim(); }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate == null ? "" : startDate.trim(); }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate == null ? "" : endDate.trim(); }

    @Override public String getType() { return "Σειρα"; }

    @Override
    public String getInfo() {
        return "Σειρα: " + getTitle() + " | Ετος: " + getYear() + " | Ειδος: " + getGenre()
             + " | Σεζον: " + seasons + " | Ηθοποιοι: " + actors;
    }
}
