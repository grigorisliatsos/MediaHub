public class Movie extends MediaItem {
    public String director;
    public String actors;

    public Movie(String title, int year, String genre, String director, String actors) {
        super(title, year, genre);
        this.director = director;
        this.actors   = actors;
    }

    @Override public String getType() { return "Ταινια"; }

    @Override
    public String getInfo() {
        return "Ταινια: " + title + " | Ετος: " + year + " | Ειδος: " + genre
             + " | Σκηνοθετης: " + director + " | Ηθοποιοι: " + actors;
    }
}
