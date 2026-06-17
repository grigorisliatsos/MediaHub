public class VideoGame extends MediaItem {
    public String developer;
    public String platform;   // π.χ. PC, PS5, Xbox
    public String startDate = "";
    public String endDate   = "";
    public boolean hasPlatinum = false;
    public String platinumDate = "";

    public VideoGame(String title, int year, String genre, String developer, String platform) {
        super(title, year, genre);
        this.developer = developer;
        this.platform  = platform;
    }

    @Override public String getType() { return "Game"; }

    @Override
    public String getInfo() {
        String platInfo = hasPlatinum ? " | Platinum: Ναι (" + platinumDate + ")" : "";
        return "Game: " + title + " | Ετος: " + year + " | Ειδος: " + genre
                + " | Developer: " + developer + " | Platform: " + platform + platInfo;
    }
}