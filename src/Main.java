import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main extends JFrame {

    // ── Auto-save path ────────────────────────────────────────────────────────
    private static final File DEFAULT_SAVE_FILE =
            new File(System.getProperty("user.home"), "mediavault_collection.csv");

    // ── Palette ───────────────────────────────────────────────────────────────
    static boolean darkMode = false;

    static Color bg()       { return darkMode ? new Color(18,18,22)    : new Color(248,248,246); }
    static Color surface()  { return darkMode ? new Color(26,26,32)    : Color.WHITE; }
    static Color surface2() { return darkMode ? new Color(34,34,42)    : new Color(243,242,239); }
    static Color border()   { return darkMode ? new Color(50,50,62)    : new Color(218,216,210); }
    static Color txt()      { return darkMode ? new Color(235,234,230) : new Color(22,22,20); }
    static Color txtMuted() { return darkMode ? new Color(120,118,114) : new Color(115,113,108); }
    static Color accent()   { return new Color(99,102,241); }
    static Color accentHov(){ return new Color(79,82,220); }

    static Color movieCol()  { return darkMode ? new Color(37,99,235)   : new Color(55,138,221); }
    static Color movieBg()   { return darkMode ? new Color(23,37,84)    : new Color(230,241,251); }
    static Color seriesCol() { return darkMode ? new Color(124,58,237)  : new Color(99,60,210); }
    static Color seriesBg()  { return darkMode ? new Color(46,16,101)   : new Color(237,233,254); }
    static Color bookCol()   { return darkMode ? new Color(16,185,129)  : new Color(5,120,85); }
    static Color bookBg()    { return darkMode ? new Color(6,78,59)     : new Color(209,250,229); }
    static Color gameCol()   { return darkMode ? new Color(245,158,11)  : new Color(180,100,0); }
    static Color gameBg()    { return darkMode ? new Color(78,52,4)     : new Color(254,243,199); }

    static Color starOn()   { return new Color(251,191,36); }
    static Color starOff()  { return darkMode ? new Color(60,60,72)    : new Color(209,207,200); }
    static Color danger()   { return new Color(239,68,68); }
    static Color dangerBg() { return darkMode ? new Color(69,10,10)    : new Color(254,226,226); }
    static Color editCol()  { return new Color(16,185,129); }
    static Color editBg()   { return darkMode ? new Color(6,78,59)     : new Color(209,250,229); }

    static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD,  20);
    static final Font FONT_CARD  = new Font("Segoe UI", Font.BOLD,  13);
    static final Font FONT_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD,  10);
    static final Font FONT_SYM   = new Font("Segoe UI Symbol", Font.PLAIN, 13);
    static final Font FONT_SYM_L = new Font("Segoe UI Symbol", Font.PLAIN, 20);

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<MediaItem> collection = new ArrayList<>();
    private String filterType  = "all";
    private String searchQuery = "";

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JPanel      rootPanel, listPanel, statsPanel;
    private JTextField  searchField;
    private JComboBox<String> typeBox, sortBox;
    private JTextField  titleField, yearField, genreField, extraField1, extraField2;
    private JTextField  dateField1, dateField2;
    private JLabel      extraLabel1, extraLabel2, dateLabel1, dateLabel2;
    private JPanel      dateRow1Panel, dateRow2Panel;
    private JLabel      countLabel;
    private JTabbedPane tabs;
    private JPanel      filterBar;
    private RatingPanel ratingPanel;

    // ── Boot ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new Main().setVisible(true);
        });
    }

    public Main() {
        setTitle("MediaVault");
        setSize(1080, 740);
        setMinimumSize(new Dimension(880, 600));
        // Χειριζόμαστε το κλείσιμο μόνοι μας ώστε να δείξουμε μήνυμα
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        buildUI();

        // ── Αυτόματη φόρτωση κατά την εκκίνηση ──────────────────────────────
        autoLoad();

        // ── Μήνυμα αποθήκευσης κατά το κλείσιμο ────────────────────────────
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (collection.isEmpty()) {
                    System.exit(0);
                    return;
                }
                int choice = JOptionPane.showOptionDialog(
                        Main.this,
                        "Θελεις να αποθηκευσεις τη συλλογη σου πριν βγεις;",
                        "Αποθηκευση πριν την εξοδο",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{"Αποθηκευση & Εξοδος", "Εξοδος χωρις αποθηκευση", "Ακυρωση"},
                        "Αποθηκευση & Εξοδος"
                );
                switch (choice) {
                    case 0 -> { quickSave(); System.exit(0); }  // Αποθήκευση & Έξοδος
                    case 1 -> System.exit(0);                   // Έξοδος χωρίς αποθήκευση
                    // case 2 / κλείσιμο dialog → παραμένουμε στο πρόγραμμα
                }
            }
        });
    }

    // ── Αυτόματη φόρτωση από default αρχείο ──────────────────────────────────
    private void autoLoad() {
        if (!DEFAULT_SAVE_FILE.exists()) return;
        try {
            List<String> lines = Files.readAllLines(DEFAULT_SAVE_FILE.toPath());
            int loaded = 0;
            for (int idx = 1; idx < lines.size(); idx++) {
                String line = lines.get(idx).trim();
                if (line.isEmpty()) continue;
                String[] p = parseCsvLine(line);
                if (p.length < 7) continue;
                String type = p[0].trim(), title = p[1];
                int year = Integer.parseInt(p[2].trim());
                String genre = p[3], e1 = p[4], e2 = p[5];
                int rating = Integer.parseInt(p[6].trim());
                String d1 = p.length > 7 ? p[7] : "";
                String d2 = p.length > 8 ? p[8] : "";
                MediaItem item;
                switch (type) {
                    case "movie"  -> { Movie m = new Movie(title,year,genre,e1,e2);                           m.watchDate=d1; item=m; }
                    case "series" -> { Series s = new Series(title,year,genre,Integer.parseInt(e1.trim()),e2); s.watchDate=d1; item=s; }
                    case "book"   -> { Book b = new Book(title,year,genre,e1,Integer.parseInt(e2.trim()));     b.startDate=d1; b.endDate=d2; item=b; }
                    case "game"   -> { VideoGame g = new VideoGame(title,year,genre,e1,e2);                    g.startDate=d1; g.endDate=d2; item=g; }
                    default       -> { continue; }
                }
                item.rating = rating;
                collection.add(item); loaded++;
            }
            if (loaded > 0) {
                refreshList();
                showToast("Φορτωθηκαν " + loaded + " εγγραφες!", true);
            }
        } catch (Exception ex) {
            // Σιωπηλό fail — αν το αρχείο είναι κατεστραμμένο απλώς ξεκινάμε με άδεια λίστα
            System.err.println("AutoLoad failed: " + ex.getMessage());
        }
    }

    // ── Γρήγορη αποθήκευση στο default αρχείο ────────────────────────────────
    private void quickSave() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(DEFAULT_SAVE_FILE))) {
            pw.println("type,title,year,genre,extra1,extra2,rating,date1,date2");
            for (MediaItem i : collection) {
                String type, e1, e2, d1 = "", d2 = "";
                if (i instanceof Movie m)        { type="movie";  e1=m.director; e2=m.actors; d1=m.watchDate; }
                else if (i instanceof Series s)  { type="series"; e1=String.valueOf(s.seasons); e2=s.actors; d1=s.watchDate; }
                else if (i instanceof Book b)    { type="book";   e1=b.author; e2=String.valueOf(b.pages); d1=b.startDate; d2=b.endDate; }
                else { VideoGame g=(VideoGame)i;  type="game";   e1=g.developer; e2=g.platform; d1=g.startDate; d2=g.endDate; }
                pw.printf("%s,\"%s\",%d,\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\"%n",
                        type,i.title,i.year,i.genre,e1,e2,i.rating,d1,d2);
            }
        } catch (IOException ex) {
            System.err.println("QuickSave failed: " + ex.getMessage());
        }
    }

    // ── Build UI ──────────────────────────────────────────────────────────────
    private void buildUI() {
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(bg());
        setContentPane(rootPanel);
        rootPanel.add(buildSidebar(),  BorderLayout.WEST);
        rootPanel.add(buildMainArea(), BorderLayout.CENTER);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(surface());
        side.setBorder(new CompoundBorder(
                new MatteBorder(0,0,0,1,border()),
                new EmptyBorder(24,20,20,20)));
        side.setPreferredSize(new Dimension(285, 0));

        JLabel title = new JLabel("MediaVault");
        title.setFont(FONT_TITLE); title.setForeground(txt());
        title.setAlignmentX(LEFT_ALIGNMENT);
        side.add(title); side.add(vgap(4));

        JLabel sub = new JLabel("Η προσωπικη σου συλλογη");
        sub.setFont(FONT_SMALL); sub.setForeground(txtMuted());
        sub.setAlignmentX(LEFT_ALIGNMENT);
        side.add(sub); side.add(vgap(20));
        side.add(separator()); side.add(vgap(18));

        // Type selector
        side.add(formLabel("ΤΥΠΟΣ")); side.add(vgap(5));
        typeBox = styledCombo(new String[]{"Ταινια","Σειρα","Βιβλιο","Video Game"});
        typeBox.addActionListener(e -> updateDynamicFields());
        side.add(typeBox); side.add(vgap(12));

        // Title
        side.add(formLabel("ΤΙΤΛΟΣ")); side.add(vgap(5));
        titleField = styledField(""); side.add(titleField); side.add(vgap(12));

        // Year + Genre
        JPanel yg = new JPanel(new GridLayout(1,2,8,0));
        yg.setOpaque(false); yg.setAlignmentX(LEFT_ALIGNMENT);
        yg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        JPanel py = new JPanel(new BorderLayout(0,4)); py.setOpaque(false);
        py.add(formLabel("ΕΤΟΣ"), BorderLayout.NORTH);
        yearField = styledField("2024"); py.add(yearField, BorderLayout.CENTER);
        JPanel pg = new JPanel(new BorderLayout(0,4)); pg.setOpaque(false);
        pg.add(formLabel("ΕΙΔΟΣ"), BorderLayout.NORTH);
        genreField = styledField("π.χ. RPG"); pg.add(genreField, BorderLayout.CENTER);
        yg.add(py); yg.add(pg);
        side.add(yg); side.add(vgap(12));

        // Extra 1
        extraLabel1 = formLabel("ΣΚΗΝΟΘΕΤΗΣ"); side.add(extraLabel1); side.add(vgap(5));
        extraField1 = styledField(""); side.add(extraField1); side.add(vgap(12));

        // Extra 2
        extraLabel2 = formLabel("ΗΘΟΠΟΙΟΙ"); side.add(extraLabel2); side.add(vgap(5));
        extraField2 = styledField(""); side.add(extraField2); side.add(vgap(12));

        // Date 1
        dateRow1Panel = new JPanel(new BorderLayout(0,4));
        dateRow1Panel.setOpaque(false); dateRow1Panel.setAlignmentX(LEFT_ALIGNMENT);
        dateRow1Panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        dateLabel1 = formLabel("ΗΜΕΡΟΜΗΝΙΑ ΠΑΡΑΚΟΛΟΥΘΗΣΗΣ");
        dateField1 = styledField("π.χ. 15/06/2024");
        dateRow1Panel.add(dateLabel1, BorderLayout.NORTH);
        dateRow1Panel.add(dateField1, BorderLayout.CENTER);
        side.add(dateRow1Panel); side.add(vgap(12));

        // Date 2 (βιβλια & games)
        dateRow2Panel = new JPanel(new BorderLayout(0,4));
        dateRow2Panel.setOpaque(false); dateRow2Panel.setAlignmentX(LEFT_ALIGNMENT);
        dateRow2Panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        dateLabel2 = formLabel("ΗΜΕΡΟΜΗΝΙΑ ΟΛΟΚΛΗΡΩΣΗΣ");
        dateField2 = styledField("π.χ. 20/07/2024");
        dateRow2Panel.add(dateLabel2, BorderLayout.NORTH);
        dateRow2Panel.add(dateField2, BorderLayout.CENTER);
        side.add(dateRow2Panel); side.add(vgap(12));

        // Rating
        side.add(formLabel("ΒΑΘΜΟΛΟΓΙΑ")); side.add(vgap(6));
        ratingPanel = new RatingPanel();
        ratingPanel.setAlignmentX(LEFT_ALIGNMENT);
        side.add(ratingPanel); side.add(vgap(20));

        // Add button
        JButton addBtn = new JButton("+ Καταχωρηση");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addBtn.setBackground(accent()); addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false); addBtn.setBorderPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        addBtn.setAlignmentX(LEFT_ALIGNMENT); addBtn.setBorder(new EmptyBorder(0,0,0,0));
        addBtn.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){ addBtn.setBackground(accentHov()); }
            public void mouseExited(MouseEvent e) { addBtn.setBackground(accent()); }
        });
        addBtn.addActionListener(e -> addItem());
        side.add(addBtn);
        side.add(Box.createVerticalGlue());
        side.add(vgap(12)); side.add(separator()); side.add(vgap(12));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
        toolbar.setOpaque(false); toolbar.setAlignmentX(LEFT_ALIGNMENT);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JButton darkBtn = smallBtn("Dark");
        darkBtn.addActionListener(e -> { darkMode=!darkMode; darkBtn.setText(darkMode?"Light":"Dark"); refreshTheme(); });
        JButton saveBtn = smallBtn("Αποθηκευση");
        saveBtn.addActionListener(e -> saveToFile());
        JButton loadBtn = smallBtn("Φορτωση");
        loadBtn.addActionListener(e -> loadFromFile());
        toolbar.add(darkBtn); toolbar.add(saveBtn); toolbar.add(loadBtn);
        side.add(toolbar);

        updateDynamicFields();
        return side;
    }

    // ── Main area ─────────────────────────────────────────────────────────────
    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(bg());

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout(12,0));
        topBar.setBackground(surface());
        topBar.setBorder(new CompoundBorder(
                new MatteBorder(0,0,1,0,border()),
                new EmptyBorder(12,20,12,20)));

        JPanel searchWrap = new JPanel(new BorderLayout(6,0));
        searchWrap.setBackground(surface2());
        searchWrap.setBorder(new CompoundBorder(new LineBorder(border(),1,true), new EmptyBorder(0,10,0,10)));
        searchWrap.setPreferredSize(new Dimension(260,34));
        JLabel si = new JLabel("Αναζητηση...");
        si.setFont(FONT_SMALL); si.setForeground(txtMuted());
        searchField = new JTextField();
        searchField.setFont(FONT_BODY); searchField.setBackground(surface2());
        searchField.setForeground(txt()); searchField.setCaretColor(txt());
        searchField.setBorder(BorderFactory.createEmptyBorder()); searchField.setOpaque(false);
        searchField.setPreferredSize(new Dimension(200,34));
        searchWrap.add(si, BorderLayout.WEST); searchWrap.add(searchField, BorderLayout.CENTER);
        searchField.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e)  { searchQuery=searchField.getText(); refreshList(); }
            public void removeUpdate(DocumentEvent e)  { searchQuery=searchField.getText(); refreshList(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        JPanel sortWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
        sortWrap.setOpaque(false);
        JLabel sl = new JLabel("Ταξινομηση:");
        sl.setFont(FONT_SMALL); sl.setForeground(txtMuted());
        sortBox = styledCombo(new String[]{"Τιτλος A-Z","Τιτλος Z-A","Ετος (παλαιο)","Ετος (νεο)","Βαθμολογια"});
        sortBox.setPreferredSize(new Dimension(170,30));
        sortBox.addActionListener(e -> refreshList());
        sortWrap.add(sl); sortWrap.add(sortBox);
        topBar.add(searchWrap, BorderLayout.WEST);
        topBar.add(sortWrap,   BorderLayout.EAST);
        main.add(topBar, BorderLayout.NORTH);

        // Tabs
        tabs = new JTabbedPane();
        tabs.setFont(FONT_BODY); tabs.setBackground(bg()); tabs.setForeground(txt());

        // Tab 1 – Collection
        JPanel collTab = new JPanel(new BorderLayout());
        collTab.setBackground(bg());

        filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT,6,10));
        filterBar.setBackground(bg()); filterBar.setBorder(new EmptyBorder(0,12,0,12));
        String[] filters = {"Ολα","Ταινιες","Σειρες","Βιβλια","Games"};
        String[] fKeys   = {"all","movie","series","book","game"};
        for (int i=0;i<filters.length;i++) {
            final String key=fKeys[i];
            ChipButton chip = new ChipButton(filters[i], key.equals(filterType));
            chip.addActionListener(e -> {
                filterType=key;
                for (Component c : filterBar.getComponents())
                    if (c instanceof ChipButton) ((ChipButton)c).setChipSelected(((ChipButton)c).key.equals(key));
                filterBar.repaint(); refreshList();
            });
            filterBar.add(chip);
        }
        countLabel = new JLabel("0 αποτελεσματα");
        countLabel.setFont(FONT_SMALL); countLabel.setForeground(txtMuted());
        JPanel filterRow = new JPanel(new BorderLayout()); filterRow.setOpaque(false);
        JPanel filterRight = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10));
        filterRight.setOpaque(false); filterRight.add(countLabel);
        filterRow.add(filterBar, BorderLayout.WEST); filterRow.add(filterRight, BorderLayout.EAST);
        collTab.add(filterRow, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(bg()); listPanel.setBorder(new EmptyBorder(4,16,16,16));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.setBackground(bg());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        collTab.add(scroll, BorderLayout.CENTER);
        tabs.addTab("Συλλογη", collTab);

        // Tab 2 – Stats
        statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBackground(bg()); statsPanel.setBorder(new EmptyBorder(20,20,20,20));
        JScrollPane statsScroll = new JScrollPane(statsPanel);
        statsScroll.setBorder(BorderFactory.createEmptyBorder()); statsScroll.setBackground(bg());
        tabs.addTab("Στατιστικα", statsScroll);

        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex()==1) refreshStats(); });
        main.add(tabs, BorderLayout.CENTER);
        refreshList();
        return main;
    }

    // ── Dynamic fields ────────────────────────────────────────────────────────
    private void updateDynamicFields() {
        String t = (String) typeBox.getSelectedItem();
        switch (t) {
            case "Ταινια" -> {
                extraLabel1.setText("ΣΚΗΝΟΘΕΤΗΣ");    extraLabel2.setText("ΗΘΟΠΟΙΟΙ");
                dateLabel1.setText("ΗΜ. ΠΑΡΑΚΟΛΟΥΘΗΣΗΣ");
                dateRow1Panel.setVisible(true); dateRow2Panel.setVisible(false);
            }
            case "Σειρα" -> {
                extraLabel1.setText("ΣΕΖΟΝ");          extraLabel2.setText("ΗΘΟΠΟΙΟΙ");
                dateLabel1.setText("ΗΜ. ΠΑΡΑΚΟΛΟΥΘΗΣΗΣ");
                dateRow1Panel.setVisible(true); dateRow2Panel.setVisible(false);
            }
            case "Βιβλιο" -> {
                extraLabel1.setText("ΣΥΓΓΡΑΦΕΑΣ");     extraLabel2.setText("ΣΕΛΙΔΕΣ");
                dateLabel1.setText("ΗΜ. ΕΝΑΡΞΗΣ");    dateLabel2.setText("ΗΜ. ΟΛΟΚΛΗΡΩΣΗΣ");
                dateRow1Panel.setVisible(true); dateRow2Panel.setVisible(true);
            }
            case "Video Game" -> {
                extraLabel1.setText("DEVELOPER");       extraLabel2.setText("PLATFORM");
                dateLabel1.setText("ΗΜ. ΕΝΑΡΞΗΣ");    dateLabel2.setText("ΗΜ. ΟΛΟΚΛΗΡΩΣΗΣ");
                dateRow1Panel.setVisible(true); dateRow2Panel.setVisible(true);
            }
        }
        dateField1.setText(""); dateField2.setText("");
        dateRow1Panel.revalidate(); dateRow2Panel.revalidate();
    }

    // ── Add item ──────────────────────────────────────────────────────────────
    private void addItem() {
        try {
            String type   = (String) typeBox.getSelectedItem();
            String title  = titleField.getText().trim();
            String yearTx = yearField.getText().trim();
            String genre  = genreField.getText().trim();
            String extra1 = extraField1.getText().trim();
            String extra2 = extraField2.getText().trim();
            String date1  = dateField1.getText().trim();
            String date2  = dateField2.getText().trim();

            if (title.isEmpty()||yearTx.isEmpty()||genre.isEmpty()||extra1.isEmpty()||extra2.isEmpty()) {
                showToast("Συμπληρωσε ολα τα υποχρεωτικα πεδια!", false); return;
            }
            int year = Integer.parseInt(yearTx);
            MediaItem item;
            switch (type) {
                case "Ταινια" -> {
                    Movie m = new Movie(title,year,genre,extra1,extra2);
                    m.watchDate=date1; item=m;
                }
                case "Σειρα" -> {
                    Series s = new Series(title,year,genre,Integer.parseInt(extra1),extra2);
                    s.watchDate=date1; item=s;
                }
                case "Βιβλιο" -> {
                    Book b = new Book(title,year,genre,extra1,Integer.parseInt(extra2));
                    b.startDate=date1; b.endDate=date2; item=b;
                }
                case "Video Game" -> {
                    VideoGame g = new VideoGame(title,year,genre,extra1,extra2);
                    g.startDate=date1; g.endDate=date2; item=g;
                }
                default -> { showToast("Αγνωστος τυπος!", false); return; }
            }
            item.rating = ratingPanel.getRating();
            collection.add(item);
            clearForm(); refreshList();
            showToast("\"" + title + "\" προστεθηκε!", true);
        } catch (NumberFormatException ex) {
            showToast("Ετος / σεζον / σελιδες: μονο αριθμοι!", false);
        }
    }

    private void clearForm() {
        titleField.setText(""); yearField.setText(""); genreField.setText("");
        extraField1.setText(""); extraField2.setText("");
        dateField1.setText(""); dateField2.setText("");
        ratingPanel.reset(); updateDynamicFields();
    }

    // ── Color/label helpers per type ──────────────────────────────────────────
    private Color typeCol(MediaItem i) {
        if (i instanceof Movie)     return movieCol();
        if (i instanceof Series)    return seriesCol();
        if (i instanceof Book)      return bookCol();
        if (i instanceof VideoGame) return gameCol();
        return accent();
    }
    private Color typeBg(MediaItem i) {
        if (i instanceof Movie)     return movieBg();
        if (i instanceof Series)    return seriesBg();
        if (i instanceof Book)      return bookBg();
        if (i instanceof VideoGame) return gameBg();
        return surface2();
    }
    private String typeLabel(MediaItem i) {
        if (i instanceof Movie)     return "Ταινια";
        if (i instanceof Series)    return "Σειρα";
        if (i instanceof Book)      return "Βιβλιο";
        if (i instanceof VideoGame) return "Game";
        return "?";
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────
    private void openEditDialog(MediaItem item) {
        JDialog dlg = new JDialog(this, "Επεξεργασια", true);
        dlg.setSize(430, 600); dlg.setLocationRelativeTo(this); dlg.setResizable(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(surface()); root.setBorder(new EmptyBorder(20,24,20,24));

        JLabel header = new JLabel("Επεξεργασια: " + item.title);
        header.setFont(new Font("Segoe UI",Font.BOLD,15));
        header.setForeground(txt()); header.setAlignmentX(LEFT_ALIGNMENT);
        root.add(header); root.add(vgap(16));

        JTextField eTitleF = dialogField(item.title);
        JTextField eYearF  = dialogField(String.valueOf(item.year));
        JTextField eGenreF = dialogField(item.genre);
        root.add(dialogRow("ΤΙΤΛΟΣ", eTitleF)); root.add(vgap(10));
        root.add(dialogRow("ΕΤΟΣ",   eYearF));  root.add(vgap(10));
        root.add(dialogRow("ΕΙΔΟΣ",  eGenreF)); root.add(vgap(10));

        JTextField eEx1, eEx2, eD1, eD2 = null;

        if (item instanceof Movie m) {
            eEx1=dialogField(m.director); eEx2=dialogField(m.actors); eD1=dialogField(m.watchDate);
            root.add(dialogRow("ΣΚΗΝΟΘΕΤΗΣ",      eEx1)); root.add(vgap(10));
            root.add(dialogRow("ΗΘΟΠΟΙΟΙ",         eEx2)); root.add(vgap(10));
            root.add(dialogRow("ΗΜ. ΠΑΡΑΚΟΛ.",     eD1));  root.add(vgap(10));
        } else if (item instanceof Series s) {
            eEx1=dialogField(String.valueOf(s.seasons)); eEx2=dialogField(s.actors); eD1=dialogField(s.watchDate);
            root.add(dialogRow("ΣΕΖΟΝ",            eEx1)); root.add(vgap(10));
            root.add(dialogRow("ΗΘΟΠΟΙΟΙ",         eEx2)); root.add(vgap(10));
            root.add(dialogRow("ΗΜ. ΠΑΡΑΚΟΛ.",     eD1));  root.add(vgap(10));
        } else if (item instanceof Book b) {
            eEx1=dialogField(b.author); eEx2=dialogField(String.valueOf(b.pages));
            eD1=dialogField(b.startDate); eD2=dialogField(b.endDate);
            root.add(dialogRow("ΣΥΓΓΡΑΦΕΑΣ",       eEx1)); root.add(vgap(10));
            root.add(dialogRow("ΣΕΛΙΔΕΣ",          eEx2)); root.add(vgap(10));
            root.add(dialogRow("ΗΜ. ΕΝΑΡΞΗΣ",      eD1));  root.add(vgap(10));
            root.add(dialogRow("ΗΜ. ΟΛΟΚΛΗΡΩΣΗΣ", eD2));  root.add(vgap(10));
        } else {
            VideoGame g = (VideoGame) item;
            eEx1=dialogField(g.developer); eEx2=dialogField(g.platform);
            eD1=dialogField(g.startDate); eD2=dialogField(g.endDate);
            root.add(dialogRow("DEVELOPER",        eEx1)); root.add(vgap(10));
            root.add(dialogRow("PLATFORM",         eEx2)); root.add(vgap(10));
            root.add(dialogRow("ΗΜ. ΕΝΑΡΞΗΣ",      eD1));  root.add(vgap(10));
            root.add(dialogRow("ΗΜ. ΟΛΟΚΛΗΡΩΣΗΣ", eD2));  root.add(vgap(10));
        }

        root.add(formLabel("ΒΑΘΜΟΛΟΓΙΑ")); root.add(vgap(6));
        RatingPanel eRating = new RatingPanel(); eRating.setRating(item.rating);
        eRating.setAlignmentX(LEFT_ALIGNMENT); root.add(eRating); root.add(vgap(20));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        btnRow.setOpaque(false); btnRow.setAlignmentX(LEFT_ALIGNMENT);
        JButton cancelBtn = smallBtn("Ακυρωση");
        cancelBtn.addActionListener(e -> dlg.dispose());
        JButton saveBtn2 = new JButton("Αποθηκευση");
        saveBtn2.setFont(new Font("Segoe UI",Font.BOLD,13));
        saveBtn2.setBackground(accent()); saveBtn2.setForeground(Color.WHITE);
        saveBtn2.setFocusPainted(false); saveBtn2.setBorderPainted(false);
        saveBtn2.setBorder(new EmptyBorder(6,16,6,16));
        saveBtn2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        final JTextField fEx1=eEx1, fEx2=eEx2, fD1=eD1, fD2=eD2;
        saveBtn2.addActionListener(e -> {
            try {
                String nt=eTitleF.getText().trim(), ng=eGenreF.getText().trim();
                String ne1=fEx1.getText().trim(), ne2=fEx2.getText().trim();
                String nd1=fD1.getText().trim();
                int ny=Integer.parseInt(eYearF.getText().trim());
                if (nt.isEmpty()||ng.isEmpty()||ne1.isEmpty()||ne2.isEmpty()) {
                    showToast("Ολα τα πεδια ειναι υποχρεωτικα!", false); return;
                }
                item.title=nt; item.year=ny; item.genre=ng; item.rating=eRating.getRating();
                if (item instanceof Movie m)       { m.director=ne1; m.actors=ne2; m.watchDate=nd1; }
                else if (item instanceof Series s) { s.seasons=Integer.parseInt(ne1); s.actors=ne2; s.watchDate=nd1; }
                else if (item instanceof Book b)   { b.author=ne1; b.pages=Integer.parseInt(ne2); b.startDate=nd1; b.endDate=fD2!=null?fD2.getText().trim():""; }
                else if (item instanceof VideoGame g){ g.developer=ne1; g.platform=ne2; g.startDate=nd1; g.endDate=fD2!=null?fD2.getText().trim():""; }
                refreshList(); showToast("\""+nt+"\" ενημερωθηκε!", true); dlg.dispose();
            } catch (NumberFormatException ex) { showToast("Ετος / σεζον / σελιδες: μονο αριθμοι!", false); }
        });
        btnRow.add(cancelBtn); btnRow.add(saveBtn2);
        root.add(btnRow);

        JScrollPane sp = new JScrollPane(root);
        sp.setBorder(BorderFactory.createEmptyBorder()); sp.setBackground(surface());
        dlg.setContentPane(sp); dlg.setVisible(true);
    }

    private JPanel dialogRow(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0,4));
        p.setOpaque(false); p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        p.add(formLabel(label), BorderLayout.NORTH); p.add(field, BorderLayout.CENTER);
        return p;
    }
    private JTextField dialogField(String val) {
        JTextField f = new JTextField(val); f.setFont(FONT_BODY);
        f.setBackground(surface2()); f.setForeground(txt()); f.setCaretColor(txt());
        f.setBorder(new CompoundBorder(new LineBorder(border(),1,true), new EmptyBorder(5,8,5,8)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,34)); f.setAlignmentX(LEFT_ALIGNMENT);
        return f;
    }

    // ── Refresh list ──────────────────────────────────────────────────────────
    private void refreshList() {
        List<MediaItem> filtered = collection.stream()
                .filter(i -> switch(filterType) {
                    case "movie"  -> i instanceof Movie;
                    case "series" -> i instanceof Series;
                    case "book"   -> i instanceof Book;
                    case "game"   -> i instanceof VideoGame;
                    default       -> true;
                })
                .filter(i -> searchQuery.isEmpty()
                        || i.title.toLowerCase().contains(searchQuery.toLowerCase())
                        || i.genre.toLowerCase().contains(searchQuery.toLowerCase()))
                .sorted(comparator())
                .collect(Collectors.toList());

        listPanel.removeAll();
        if (filtered.isEmpty()) {
            JPanel empty = new JPanel(new BorderLayout());
            empty.setOpaque(false); empty.setBorder(new EmptyBorder(60,0,0,0));
            JLabel lbl = new JLabel(
                    collection.isEmpty() ? "Δεν εχεις προσθεσει κατι ακομα" : "Δεν βρεθηκαν αποτελεσματα",
                    SwingConstants.CENTER);
            lbl.setFont(FONT_BODY); lbl.setForeground(txtMuted());
            empty.add(lbl, BorderLayout.CENTER); listPanel.add(empty);
        } else {
            for (MediaItem item : filtered) { listPanel.add(makeCard(item)); listPanel.add(vgap(8)); }
        }
        countLabel.setText(filtered.size() + " αποτελεσματα");
        listPanel.revalidate(); listPanel.repaint();
    }

    private Comparator<MediaItem> comparator() {
        int idx = sortBox==null ? 0 : sortBox.getSelectedIndex();
        return switch(idx) {
            case 1  -> Comparator.comparing((MediaItem i)->i.title).reversed();
            case 2  -> Comparator.comparingInt((MediaItem i)->i.year);
            case 3  -> Comparator.comparingInt((MediaItem i)->i.year).reversed();
            case 4  -> Comparator.comparingInt((MediaItem i)->i.rating).reversed();
            default -> Comparator.comparing(i->i.title);
        };
    }

    // ── Card ──────────────────────────────────────────────────────────────────
    private JPanel makeCard(MediaItem item) {
        Color col   = typeCol(item);
        Color colBg = typeBg(item);
        String tStr = typeLabel(item);

        // detail + date lines
        String detail, dateInfo="";
        if (item instanceof Movie m) {
            detail="Σκηνοθετης: "+m.director+"  |  Ηθοποιοι: "+m.actors;
            if (!m.watchDate.isEmpty()) dateInfo="Παρακολ.: "+m.watchDate;
        } else if (item instanceof Series s) {
            detail="Σεζον: "+s.seasons+"  |  Ηθοποιοι: "+s.actors;
            if (!s.watchDate.isEmpty()) dateInfo="Παρακολ.: "+s.watchDate;
        } else if (item instanceof Book b) {
            detail="Συγγραφεας: "+b.author+"  |  Σελιδες: "+b.pages;
            if (!b.startDate.isEmpty()||!b.endDate.isEmpty())
                dateInfo="Αναγνωση: "+(b.startDate.isEmpty()?"-":b.startDate)+" -> "+(b.endDate.isEmpty()?"σε εξελιξη":b.endDate);
        } else {
            VideoGame g=(VideoGame)item;
            detail="Developer: "+g.developer+"  |  Platform: "+g.platform;
            if (!g.startDate.isEmpty()||!g.endDate.isEmpty())
                dateInfo="Παιξιμο: "+(g.startDate.isEmpty()?"-":g.startDate)+" -> "+(g.endDate.isEmpty()?"σε εξελιξη":g.endDate);
        }

        HoverCard card = new HoverCard();
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setLayout(new BorderLayout(10,0));
        card.setBorder(new CompoundBorder(new LineBorder(border(),1,true), new EmptyBorder(12,14,12,14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, dateInfo.isEmpty() ? 80 : 96));

        // Accent stripe
        JPanel stripe = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),6,6));
            }
        };
        stripe.setOpaque(false); stripe.setPreferredSize(new Dimension(5,0));
        card.add(stripe, BorderLayout.WEST);

        // Type icon
        JLabel icLbl = new JLabel(tStr.substring(0,1), SwingConstants.CENTER);
        icLbl.setFont(new Font("Segoe UI",Font.BOLD,13));
        icLbl.setForeground(col); icLbl.setOpaque(true); icLbl.setBackground(colBg);
        icLbl.setPreferredSize(new Dimension(36,36)); icLbl.setBorder(new EmptyBorder(0,4,0,8));
        card.add(icLbl, BorderLayout.LINE_START);

        // Center info
        JPanel info = new JPanel();
        info.setOpaque(false); info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0)); row1.setOpaque(false);
        JLabel badge=new JLabel(tStr); badge.setFont(FONT_LABEL); badge.setForeground(col);
        badge.setBackground(colBg); badge.setOpaque(true); badge.setBorder(new EmptyBorder(2,6,2,6));
        JLabel tl=new JLabel(item.title); tl.setFont(FONT_CARD); tl.setForeground(txt());
        JLabel yl=new JLabel("· "+item.year); yl.setFont(FONT_BODY); yl.setForeground(txtMuted());
        JLabel gl=new JLabel("· "+item.genre); gl.setFont(FONT_BODY); gl.setForeground(txtMuted());
        row1.add(badge); row1.add(tl); row1.add(yl); row1.add(gl); info.add(row1);

        JPanel row2=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); row2.setOpaque(false);
        JLabel dl=new JLabel(detail); dl.setFont(FONT_SMALL); dl.setForeground(txtMuted());
        row2.add(dl); info.add(row2);

        if (!dateInfo.isEmpty()) {
            JPanel row3=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); row3.setOpaque(false);
            JLabel dtl=new JLabel("\u25A6 "+dateInfo); dtl.setFont(FONT_SMALL); dtl.setForeground(col);
            row3.add(dtl); info.add(row3);
        }

        JPanel row4=new JPanel(new FlowLayout(FlowLayout.LEFT,2,1)); row4.setOpaque(false);
        for (int s=1;s<=5;s++) {
            JLabel star=new JLabel(s<=item.rating?"\u2605":"\u2606");
            star.setFont(FONT_SYM); star.setForeground(s<=item.rating?starOn():starOff());
            row4.add(star);
        }
        info.add(row4);
        card.add(info, BorderLayout.CENTER);

        // Buttons
        JPanel btnCol = new JPanel(); btnCol.setOpaque(false);
        btnCol.setLayout(new BoxLayout(btnCol, BoxLayout.Y_AXIS));
        JButton editBtn=new JButton("..."); editBtn.setFont(new Font("Segoe UI",Font.BOLD,11));
        editBtn.setForeground(editCol()); editBtn.setBackground(editBg());
        editBtn.setBorderPainted(false); editBtn.setFocusPainted(false);
        editBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editBtn.setPreferredSize(new Dimension(30,26)); editBtn.setMaximumSize(new Dimension(30,26));
        editBtn.addActionListener(e -> openEditDialog(item));

        JButton delBtn=new JButton("x"); delBtn.setFont(new Font("Segoe UI",Font.BOLD,11));
        delBtn.setForeground(txtMuted()); delBtn.setBackground(surface());
        delBtn.setBorderPainted(false); delBtn.setFocusPainted(false);
        delBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delBtn.setPreferredSize(new Dimension(30,26)); delBtn.setMaximumSize(new Dimension(30,26));
        delBtn.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){ delBtn.setForeground(danger());   delBtn.setBackground(dangerBg()); }
            public void mouseExited(MouseEvent e) { delBtn.setForeground(txtMuted()); delBtn.setBackground(surface()); }
        });
        delBtn.addActionListener(e -> { collection.remove(item); refreshList(); });
        btnCol.add(editBtn); btnCol.add(Box.createVerticalStrut(4)); btnCol.add(delBtn);
        card.add(btnCol, BorderLayout.EAST);
        return card;
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    private void refreshStats() {
        statsPanel.removeAll();
        long movies = collection.stream().filter(i->i instanceof Movie).count();
        long series = collection.stream().filter(i->i instanceof Series).count();
        long books  = collection.stream().filter(i->i instanceof Book).count();
        long games  = collection.stream().filter(i->i instanceof VideoGame).count();
        double avg  = collection.stream().mapToInt(i->i.rating).filter(r->r>0).average().orElse(0);

        JPanel cards=new JPanel(new GridLayout(1,5,10,0));
        cards.setOpaque(false); cards.setAlignmentX(LEFT_ALIGNMENT);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE,100));
        cards.add(statCard("Συνολο",  String.valueOf(collection.size()), accent()));
        cards.add(statCard("Ταινιες", String.valueOf(movies),  movieCol()));
        cards.add(statCard("Σειρες",  String.valueOf(series),  seriesCol()));
        cards.add(statCard("Βιβλια",  String.valueOf(books),   bookCol()));
        cards.add(statCard("Games",   String.valueOf(games),   gameCol()));
        statsPanel.add(cards); statsPanel.add(vgap(20));

        JPanel avgRow=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        avgRow.setOpaque(false); avgRow.setAlignmentX(LEFT_ALIGNMENT);
        JLabel avgLbl=new JLabel("Μεση βαθμολογια: "); avgLbl.setFont(FONT_BODY); avgLbl.setForeground(txt());
        JLabel avgVal=new JLabel(String.format("%.1f / 5",avg));
        avgVal.setFont(new Font("Segoe UI",Font.BOLD,14)); avgVal.setForeground(starOn());
        avgRow.add(avgLbl); avgRow.add(avgVal);
        statsPanel.add(avgRow); statsPanel.add(vgap(20));

        JLabel ct=new JLabel("Κατανομη ανα τυπο");
        ct.setFont(new Font("Segoe UI",Font.BOLD,14)); ct.setForeground(txt()); ct.setAlignmentX(LEFT_ALIGNMENT);
        statsPanel.add(ct); statsPanel.add(vgap(10));
        int total=Math.max(collection.size(),1);
        statsPanel.add(barRow("Ταινιες",(int)movies,total,movieCol())); statsPanel.add(vgap(6));
        statsPanel.add(barRow("Σειρες", (int)series,total,seriesCol())); statsPanel.add(vgap(6));
        statsPanel.add(barRow("Βιβλια", (int)books, total,bookCol()));   statsPanel.add(vgap(6));
        statsPanel.add(barRow("Games",  (int)games, total,gameCol()));   statsPanel.add(vgap(20));

        if (!collection.isEmpty()) {
            JLabel topT=new JLabel("Top αξιολογησεις");
            topT.setFont(new Font("Segoe UI",Font.BOLD,14)); topT.setForeground(txt()); topT.setAlignmentX(LEFT_ALIGNMENT);
            statsPanel.add(topT); statsPanel.add(vgap(8));
            collection.stream().filter(i->i.rating>0)
                    .sorted(Comparator.comparingInt((MediaItem i)->i.rating).reversed()).limit(5)
                    .forEach(i -> {
                        JLabel lbl=new JLabel(starsStr(i.rating)+"  "+i.title+"  ("+i.year+")  ["+typeLabel(i)+"]");
                        lbl.setFont(FONT_SYM); lbl.setForeground(txt()); lbl.setAlignmentX(LEFT_ALIGNMENT);
                        statsPanel.add(lbl); statsPanel.add(vgap(4));
                    });
        }
        statsPanel.add(Box.createVerticalGlue());
        statsPanel.revalidate(); statsPanel.repaint();
    }

    private String starsStr(int r) {
        StringBuilder sb=new StringBuilder();
        for(int i=1;i<=5;i++) sb.append(i<=r?"\u2605":"\u2606");
        return sb.toString();
    }
    private JPanel statCard(String label, String value, Color col) {
        JPanel p=new JPanel(new BorderLayout(0,6)); p.setBackground(surface());
        p.setBorder(new CompoundBorder(new LineBorder(border(),1,true),new EmptyBorder(14,16,14,16)));
        JLabel vl=new JLabel(value,SwingConstants.LEFT); vl.setFont(new Font("Segoe UI",Font.BOLD,28)); vl.setForeground(col);
        JLabel nl=new JLabel(label); nl.setFont(FONT_SMALL); nl.setForeground(txtMuted());
        p.add(vl,BorderLayout.CENTER); p.add(nl,BorderLayout.SOUTH); return p;
    }
    private JPanel barRow(String label, int count, int total, Color col) {
        JPanel row=new JPanel(new BorderLayout(10,0)); row.setOpaque(false); row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));
        JLabel lbl=new JLabel(label); lbl.setFont(FONT_BODY); lbl.setForeground(txt()); lbl.setPreferredSize(new Dimension(80,20));
        float pct=(float)count/total;
        JPanel barBg=new JPanel(new BorderLayout()); barBg.setBackground(surface2()); barBg.setBorder(new LineBorder(border(),1,true));
        JPanel barFg=new JPanel(){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),4,4));
            }
        };
        barFg.setOpaque(false); barFg.setPreferredSize(new Dimension((int)(400*pct),20));
        barBg.add(barFg,BorderLayout.WEST);
        JLabel cl=new JLabel(count+" ",SwingConstants.RIGHT); cl.setFont(FONT_SMALL); cl.setForeground(txtMuted()); cl.setPreferredSize(new Dimension(28,20));
        row.add(lbl,BorderLayout.WEST); row.add(barBg,BorderLayout.CENTER); row.add(cl,BorderLayout.EAST);
        return row;
    }

    // ── Save / Load ───────────────────────────────────────────────────────────
    private void saveToFile() {
        JFileChooser fc=new JFileChooser();
        fc.setDialogTitle("Αποθηκευση συλλογης");
        fc.addChoosableFileFilter(new FileNameExtensionFilter("CSV αρχειο","csv"));
        fc.setSelectedFile(new File("collection.csv"));
        if (fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter pw=new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
            pw.println("type,title,year,genre,extra1,extra2,rating,date1,date2");
            for (MediaItem i : collection) {
                String type,e1,e2,d1="",d2="";
                if (i instanceof Movie m)       { type="movie";   e1=m.director; e2=m.actors; d1=m.watchDate; }
                else if (i instanceof Series s) { type="series";  e1=String.valueOf(s.seasons); e2=s.actors; d1=s.watchDate; }
                else if (i instanceof Book b)   { type="book";    e1=b.author; e2=String.valueOf(b.pages); d1=b.startDate; d2=b.endDate; }
                else { VideoGame g=(VideoGame)i; type="game";    e1=g.developer; e2=g.platform; d1=g.startDate; d2=g.endDate; }
                pw.printf("%s,\"%s\",%d,\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\"%n",type,i.title,i.year,i.genre,e1,e2,i.rating,d1,d2);
            }
            showToast("Αποθηκευτηκε: "+fc.getSelectedFile().getName(),true);
        } catch (IOException ex) { showToast("Σφαλμα αποθηκευσης!",false); }
    }

    private void loadFromFile() {
        JFileChooser fc=new JFileChooser();
        fc.setDialogTitle("Φορτωση συλλογης");
        fc.addChoosableFileFilter(new FileNameExtensionFilter("CSV αρχειο","csv"));
        if (fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        try {
            List<String> lines=Files.readAllLines(fc.getSelectedFile().toPath());
            int loaded=0;
            for (int idx=1;idx<lines.size();idx++) {
                String line=lines.get(idx).trim(); if (line.isEmpty()) continue;
                String[] p=parseCsvLine(line); if (p.length<7) continue;
                String type=p[0].trim(),title=p[1]; int year=Integer.parseInt(p[2].trim());
                String genre=p[3],e1=p[4],e2=p[5]; int rating=Integer.parseInt(p[6].trim());
                String d1=p.length>7?p[7]:"", d2=p.length>8?p[8]:"";
                MediaItem item;
                switch(type) {
                    case "movie"  -> { Movie m=new Movie(title,year,genre,e1,e2); m.watchDate=d1; item=m; }
                    case "series" -> { Series s=new Series(title,year,genre,Integer.parseInt(e1.trim()),e2); s.watchDate=d1; item=s; }
                    case "book"   -> { Book b=new Book(title,year,genre,e1,Integer.parseInt(e2.trim())); b.startDate=d1; b.endDate=d2; item=b; }
                    case "game"   -> { VideoGame g=new VideoGame(title,year,genre,e1,e2); g.startDate=d1; g.endDate=d2; item=g; }
                    default       -> { continue; }
                }
                item.rating=rating; collection.add(item); loaded++;
            }
            refreshList(); showToast("Φορτωθηκαν "+loaded+" εγγραφες!",true);
        } catch (Exception ex) { showToast("Σφαλμα: "+ex.getMessage(),false); }
    }

    private String[] parseCsvLine(String line) {
        List<String> result=new ArrayList<>(); boolean inQ=false; StringBuilder sb=new StringBuilder();
        for (char c:line.toCharArray()) {
            if (c=='"') inQ=!inQ;
            else if (c==','&&!inQ) { result.add(sb.toString()); sb.setLength(0); }
            else sb.append(c);
        }
        result.add(sb.toString()); return result.toArray(new String[0]);
    }

    // ── Theme / Toast ─────────────────────────────────────────────────────────
    private void refreshTheme() {
        SwingUtilities.invokeLater(()->{getContentPane().removeAll();buildUI();revalidate();repaint();});
    }
    private void showToast(String msg, boolean success) {
        JWindow toast=new JWindow(this);
        JLabel lbl=new JLabel(msg,SwingConstants.CENTER); lbl.setFont(FONT_BODY); lbl.setForeground(Color.WHITE);
        lbl.setBackground(success?new Color(22,163,74):new Color(220,38,38)); lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(10,20,10,20)); toast.add(lbl); toast.pack();
        Point loc=getLocationOnScreen();
        toast.setLocation(loc.x+getWidth()/2-toast.getWidth()/2,loc.y+getHeight()-60);
        toast.setVisible(true);
        new javax.swing.Timer(2200,e->toast.dispose()).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JLabel formLabel(String t) {
        JLabel l=new JLabel(t); l.setFont(FONT_LABEL); l.setForeground(txtMuted()); l.setAlignmentX(LEFT_ALIGNMENT); return l;
    }
    private JTextField styledField(String ph) {
        JTextField f=new JTextField(); f.setFont(FONT_BODY);
        f.setBackground(surface2()); f.setForeground(txt()); f.setCaretColor(txt());
        f.setBorder(new CompoundBorder(new LineBorder(border(),1,true),new EmptyBorder(5,8,5,8)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,34)); f.setAlignmentX(LEFT_ALIGNMENT); return f;
    }
    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb=new JComboBox<>(items); cb.setFont(FONT_BODY);
        cb.setBackground(surface2()); cb.setForeground(txt());
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE,34)); cb.setAlignmentX(LEFT_ALIGNMENT); return cb;
    }
    private JButton smallBtn(String text) {
        JButton b=new JButton(text); b.setFont(FONT_SMALL);
        b.setBackground(surface2()); b.setForeground(txt());
        b.setBorder(new CompoundBorder(new LineBorder(border(),1,true),new EmptyBorder(4,8,4,8)));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    private JSeparator separator() {
        JSeparator s=new JSeparator(); s.setForeground(border());
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE,1)); s.setAlignmentX(LEFT_ALIGNMENT); return s;
    }
    private Component vgap(int h) { return Box.createRigidArea(new Dimension(0,h)); }

    // ── Inner classes ─────────────────────────────────────────────────────────

    static class HoverCard extends JPanel {
        private float alpha=0f;
        HoverCard() {
            setOpaque(true);
            addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){alpha=1;repaint();}
                public void mouseExited(MouseEvent e) {alpha=0;repaint();}
            });
        }
        protected void paintComponent(Graphics g) {
            setBackground(darkMode?(alpha>0?new Color(38,38,48):new Color(26,26,32)):(alpha>0?new Color(245,244,241):Color.WHITE));
            super.paintComponent(g);
        }
    }

    static class ChipButton extends JButton {
        final String key;
        private boolean chipSelected;
        ChipButton(String label, boolean sel) {
            super(label); this.key=label; this.chipSelected=sel;
            setFont(FONT_BODY); setFocusPainted(false); setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setOpaque(true); updateStyle();
        }
        void setChipSelected(boolean s){chipSelected=s;updateStyle();repaint();}
        void updateStyle(){
            if(chipSelected){setBackground(accent());setForeground(Color.WHITE);}
            else{setBackground(surface2());setForeground(txt());}
            setBorder(new EmptyBorder(5,14,5,14));
        }
        protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground()); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),20,20));
            g2.dispose(); super.paintComponent(g);
        }
    }

    static class RatingPanel extends JPanel {
        private int rating=0;
        private final JLabel[] stars=new JLabel[5];
        RatingPanel(){
            setOpaque(false); setLayout(new FlowLayout(FlowLayout.LEFT,4,0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE,28));
            for(int i=1;i<=5;i++){
                final int val=i; JLabel s=new JLabel("\u2606");
                s.setFont(FONT_SYM_L); s.setForeground(starOff());
                s.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                s.addMouseListener(new MouseAdapter(){
                    public void mouseClicked(MouseEvent e){rating=val;updateStars();}
                    public void mouseEntered(MouseEvent e){highlight(val);}
                    public void mouseExited(MouseEvent e){updateStars();}
                });
                stars[i-1]=s; add(s);
            }
            JLabel hint=new JLabel("  κλικ για βαθμολογηση"); hint.setFont(FONT_SMALL); hint.setForeground(txtMuted()); add(hint);
        }
        private void highlight(int u){for(int i=0;i<5;i++){stars[i].setText(i<u?"\u2605":"\u2606");stars[i].setForeground(i<u?starOn():starOff());}}
        private void updateStars(){for(int i=0;i<5;i++){stars[i].setText(i<rating?"\u2605":"\u2606");stars[i].setForeground(i<rating?starOn():starOff());}}
        void setRating(int r){rating=r;updateStars();}
        int  getRating(){return rating;}
        void reset(){rating=0;updateStars();}
    }
}