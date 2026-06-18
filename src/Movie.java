public class Movie extends MediaItem {
    private String director;
    private String actors;

    public Movie(String title, int year, String genre, String director, String actors) {
        super(title, year, genre);
        setDirector(director);
        setActors(actors);
    }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director == null ? "" : director.trim(); }

    public String getActors() { return actors; }
    public void setActors(String actors) { this.actors = actors == null ? "" : actors.trim(); }

    @Override public String getType() { return "Ταινια"; }

    @Override
    public String getInfo() {
        return "Ταινια: " + getTitle() + " | Ετος: " + getYear() + " | Ειδος: " + getGenre()
             + " | Σκηνοθετης: " + director + " | Ηθοποιοι: " + actors;
    }
}
