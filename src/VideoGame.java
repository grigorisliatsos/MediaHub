public class VideoGame extends MediaItem {
    private String developer;
    private String platform;
    private String startDate = "";
    private String endDate = "";
    private boolean hasPlatinum = false;
    private String platinumDate = "";

    public VideoGame(String title, int year, String genre, String developer, String platform) {
        super(title, year, genre);
        setDeveloper(developer);
        setPlatform(platform);
    }

    public String getDeveloper() { return developer; }
    public void setDeveloper(String developer) { this.developer = developer == null ? "" : developer.trim(); }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform == null ? "" : platform.trim(); }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate == null ? "" : startDate.trim(); }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate == null ? "" : endDate.trim(); }

    public boolean hasPlatinum() { return hasPlatinum; }
    public void setHasPlatinum(boolean hasPlatinum) { this.hasPlatinum = hasPlatinum; }

    public String getPlatinumDate() { return platinumDate; }
    public void setPlatinumDate(String platinumDate) { this.platinumDate = platinumDate == null ? "" : platinumDate.trim(); }

    @Override public String getType() { return "Game"; }

    @Override
    public String getInfo() {
        String platInfo = hasPlatinum ? " | Platinum: Ναι (" + platinumDate + ")" : "";
        return "Game: " + getTitle() + " | Ετος: " + getYear() + " | Ειδος: " + getGenre()
                + " | Developer: " + developer + " | Platform: " + platform + platInfo;
    }
}
