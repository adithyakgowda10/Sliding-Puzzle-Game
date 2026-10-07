import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * SlidingPuzzle.java
 * 
 * Main entry point and GUI window for the "Sliding Puzzle Game".
 * 
 * Key Features:
 * - Selectable 3x3 through 6x6 sliding puzzle boards.
 * - Real-time Move Counter, Live Timer, and Session Best Score tracking.
 * - Shuffle, Reset, and New Game controls with a consistent medium difficulty.
 * - Sound effects synthesis with mute toggle.
 * - Visual win congratulations dialog with run statistics.
 * - Keyboard support (Arrow keys and WASD).
 * - Clean, modern dark slate aesthetic designed with Swing.
 */
public class SlidingPuzzle extends JFrame implements GameBoard.TileClickListener {

    private static final int SHUFFLE_MOVES = 30;
    private static final int MOVE_GOAL = 40;

    // Core Game Components
    private final PuzzleLogic puzzleLogic;
    private final GameServices.GameTimer gameTimer;
    private final GameBoard gameBoard;
    private final GameServices.SoundManager soundManager;
    private CardLayout screenLayout;
    private JPanel screenCards;
    private boolean gameStarted;
    private boolean pictureMode;
    private JLabel selectedImageLabel;
    private JLabel selectionSummaryLabel;
    private JRadioButton numberModeOption;
    private JRadioButton pictureModeOption;
    private JButton continueButton;

    // Session Best Records
    private int bestMoves = -1;
    private int bestSeconds = -1;
    private final Preferences progress = Preferences.userNodeForPackage(SlidingPuzzle.class);
    private final List<GalleryImage> imageGallery = new ArrayList<>();
    private int cluesRemaining = 3;
    private boolean dailyMode;

    // UI Labels and Controls
    private JLabel movesValueLabel;
    private JLabel timerValueLabel;
    private JLabel bestValueLabel;
    private JLabel challengeValueLabel;
    private JLabel subtitleLabel;
    private JMenuItem soundToggleMenuItem;
    private JMenuItem previewMenuItem;
    private JButton clueButton;
    private JButton pauseResumeButton;
    private JButton autoSolveButton;
    private JMenuItem dailyModeMenuItem;
    private JLabel modeLabel;
    private boolean paused;
    private boolean solverBusy;
    private boolean solverPlaying;
    private SwingWorker<java.util.List<Integer>, Void> solverWorker;
    private Timer solverPlaybackTimer;
    private java.util.List<Integer> solverMoves = java.util.Collections.emptyList();
    private int solverMoveIndex;
    private int solverGeneration;
    private BufferedImage puzzleImage;

    // UI Theme Palette
    private static final Color BG_DARK = new Color(235, 241, 247);
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BORDER_CARD = new Color(213, 222, 232);
    private static final Color TEXT_PRIMARY = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(93, 108, 126);
    private static final Color ACCENT_EMERALD = new Color(25, 107, 84);
    private static final Color ACCENT_AMBER = new Color(184, 104, 47);
    private static final Color ACCENT_SLATE = new Color(78, 103, 94);

    public SlidingPuzzle() {
        super("Slide Puzzle | 8 & 15 Puzzle");

        // Initialize core models
        this.puzzleLogic = new PuzzleLogic();
        this.gameTimer = new GameServices.GameTimer();
        this.soundManager = GameServices.SoundManager.getInstance();
        this.gameBoard = new GameBoard(puzzleLogic);
        loadImageGallery();

        // Register listeners
        this.gameBoard.setTileClickListener(this);
        this.gameTimer.setListener(new GameServices.GameTimer.TimerListener() {
            @Override
            public void onTimeTick(int totalSeconds, String formattedTime) {
                timerValueLabel.setText(formattedTime);
            }
        });

        // Setup Window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(BG_DARK);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());
        setResizable(false);

        screenLayout = new CardLayout();
        screenCards = new JPanel(screenLayout);
        JPanel gameScreen = createGameScreen();
        screenCards.add(createLoadingScreen(), "loading");
        screenCards.add(createWelcomeScreen(), "welcome");
        screenCards.add(createSetupScreen(), "setup");
        screenCards.add(gameScreen, "game");

        add(screenCards, BorderLayout.CENTER);

        // Keep every screen at the same dimensions to avoid layout jumps.
        pack();
        setLocationRelativeTo(null);
        startNewGame();
        Timer loadingTimer = new Timer(1250, e -> showScreen("welcome"));
        loadingTimer.setRepeats(false);
        loadingTimer.start();
    }

    private JPanel createGameScreen() {
        JPanel game = new JPanel(new BorderLayout(0, 6));
        game.setBackground(BG_DARK);
        game.add(createHeaderPanel(), BorderLayout.NORTH);
        game.add(createCenterPanel(), BorderLayout.CENTER);
        game.add(createBottomPanel(), BorderLayout.SOUTH);
        return game;
    }

    private JPanel createLoadingScreen() {
        LaunchBackgroundPanel loading = new LaunchBackgroundPanel();
        loading.setLayout(new BoxLayout(loading, BoxLayout.Y_AXIS));
        loading.setBorder(new EmptyBorder(250, 40, 40, 40));
        JLabel mark = new JLabel("SLIDE / SHIFT", SwingConstants.CENTER);
        mark.setFont(new Font("Segoe UI", Font.BOLD, 32));
        mark.setForeground(Color.WHITE);
        mark.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel status = new JLabel("SETTING THE BOARD", SwingConstants.CENTER);
        status.setFont(new Font("Segoe UI", Font.BOLD, 11));
        status.setForeground(new Color(126, 245, 218));
        status.setAlignmentX(Component.CENTER_ALIGNMENT);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorderPainted(false);
        progressBar.setForeground(new Color(255, 116, 96));
        progressBar.setBackground(new Color(255, 255, 255, 45));
        progressBar.setMaximumSize(new Dimension(190, 5));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        loading.add(mark);
        loading.add(Box.createRigidArea(new Dimension(0, 10)));
        loading.add(status);
        loading.add(Box.createRigidArea(new Dimension(0, 26)));
        loading.add(progressBar);
        loading.setPreferredSize(new Dimension(580, 700));
        return loading;
    }

    private JPanel createWelcomeScreen() {
        LaunchBackgroundPanel welcome = new LaunchBackgroundPanel();
        welcome.setLayout(new BorderLayout());
        welcome.setBorder(new EmptyBorder(42, 38, 34, 38));

        JLabel eyebrow = new JLabel("A SMALL GAME FOR A CLEAR MIND");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 11));
        eyebrow.setForeground(new Color(126, 245, 218));
        JLabel title = new JLabel("Make space\nfor a little play.");
        title.setText("<html>Make space<br>for a little play.</html>");
        title.setFont(new Font("Segoe UI", Font.BOLD, 39));
        title.setForeground(Color.WHITE);
        JLabel detail = new JLabel("A classic sliding puzzle, with a fresh point of view.");
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        detail.setForeground(new Color(218, 231, 238));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(eyebrow);
        copy.add(Box.createRigidArea(new Dimension(0, 15)));
        copy.add(title);
        copy.add(Box.createRigidArea(new Dimension(0, 12)));
        copy.add(detail);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        ModernButton start = new ModernButton("START PLAYING  →", new Color(255, 116, 96), Color.WHITE);
        start.setFont(new Font("Segoe UI", Font.BOLD, 15));
        start.setPreferredSize(new Dimension(250, 50));
        start.setMaximumSize(new Dimension(250, 50));
        start.setAlignmentX(Component.LEFT_ALIGNMENT);
        start.addActionListener(e -> showScreen("setup"));
        JLabel note = new JLabel("NUMBER PUZZLES  ·  3×3 TO 6×6  ·  IMAGE MODE");
        note.setFont(new Font("Segoe UI", Font.BOLD, 10));
        note.setForeground(new Color(193, 211, 220));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.add(start);
        actions.add(Box.createRigidArea(new Dimension(0, 14)));
        actions.add(note);

        welcome.add(copy, BorderLayout.NORTH);
        welcome.add(new PuzzleArtwork(), BorderLayout.CENTER);
        welcome.add(actions, BorderLayout.SOUTH);
        welcome.setPreferredSize(new Dimension(580, 700));
        return welcome;
    }

    private JPanel createSetupScreen() {
        JPanel setup = new JPanel(new BorderLayout(0, 10));
        setup.setBackground(new Color(20, 91, 91));
        setup.setBorder(new EmptyBorder(18, 28, 22, 28));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(setupHeading("Game Type"));
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        content.add(createModeOptions());
        selectedImageLabel = new JLabel("", SwingConstants.CENTER);
        selectedImageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        selectedImageLabel.setForeground(new Color(188, 226, 220));
        selectedImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(selectedImageLabel);
        content.add(Box.createRigidArea(new Dimension(0, 16)));
        content.add(setupHeading("Board Size"));
        content.add(Box.createRigidArea(new Dimension(0, 5)));
        content.add(createSizeOptions());
        content.add(Box.createRigidArea(new Dimension(0, 15)));
        content.add(setupHeading("Type"));
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        SetupBoardPreview preview = new SetupBoardPreview();
        preview.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(preview);
        content.add(Box.createRigidArea(new Dimension(0, 5)));
        JLabel typeName = new JLabel("Classic", SwingConstants.CENTER);
        typeName.setFont(new Font("Segoe UI", Font.BOLD, 20));
        typeName.setForeground(Color.WHITE);
        typeName.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(typeName);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        ModernButton newGame = new ModernButton("NEW GAME", new Color(236, 105, 76), Color.WHITE);
        newGame.setFont(new Font("Segoe UI", Font.BOLD, 17));
        newGame.setPreferredSize(new Dimension(260, 48));
        newGame.setMaximumSize(new Dimension(260, 48));
        newGame.setAlignmentX(Component.CENTER_ALIGNMENT);
        newGame.addActionListener(e -> startFromSetup(true));
        continueButton = new JButton("CONTINUE");
        continueButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        continueButton.setForeground(Color.WHITE);
        continueButton.setContentAreaFilled(false);
        continueButton.setBorder(BorderFactory.createLineBorder(new Color(244, 151, 105), 2));
        continueButton.setFocusPainted(false);
        continueButton.setEnabled(false);
        continueButton.setPreferredSize(new Dimension(260, 44));
        continueButton.setMaximumSize(new Dimension(260, 44));
        continueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        continueButton.addActionListener(e -> startFromSetup(false));
        selectionSummaryLabel = new JLabel("", SwingConstants.CENTER);
        selectionSummaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        selectionSummaryLabel.setForeground(new Color(221, 241, 236));
        selectionSummaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        actions.add(newGame);
        actions.add(Box.createRigidArea(new Dimension(0, 8)));
        actions.add(continueButton);
        actions.add(Box.createRigidArea(new Dimension(0, 7)));
        actions.add(selectionSummaryLabel);
        updateSetupSummary();

        setup.add(content, BorderLayout.CENTER);
        setup.add(actions, BorderLayout.SOUTH);
        setup.setPreferredSize(new Dimension(580, 700));
        return setup;
    }

    private JLabel setupHeading(String text) {
        JLabel heading = new JLabel(text, SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 27));
        heading.setForeground(Color.WHITE);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        return heading;
    }

    private JPanel createModeOptions() {
        JPanel options = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 0));
        options.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        numberModeOption = createSetupRadio("Number", !pictureMode);
        pictureModeOption = createSetupRadio("Picture", pictureMode);
        group.add(numberModeOption);
        group.add(pictureModeOption);
        numberModeOption.addActionListener(e -> {
            pictureMode = false;
            selectedImageLabel.setText("");
        });
        pictureModeOption.addActionListener(e -> {
            pictureMode = true;
            if (puzzleImage == null) {
                selectImageForSetup();
                if (puzzleImage == null) {
                    pictureMode = false;
                    numberModeOption.setSelected(true);
                }
            } else {
                selectedImageLabel.setText("Picture ready");
            }
        });
        options.add(numberModeOption);
        options.add(pictureModeOption);
        return options;
    }

    private JPanel createSizeOptions() {
        JPanel options = new JPanel(new FlowLayout(FlowLayout.CENTER, 13, 0));
        options.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (int size = 3; size <= 6; size++) {
            final int selectedSize = size;
            JRadioButton option = createSetupRadio(size + "×" + size, puzzleLogic.getSize() == size);
            group.add(option);
            option.addActionListener(e -> {
                changeBoardSize(selectedSize);
                updateSetupSummary();
            });
            options.add(option);
        }
        return options;
    }

    private void updateSetupSummary() {
        if (selectionSummaryLabel != null) {
            selectionSummaryLabel.setText(puzzleLogic.getSize() + "×" + puzzleLogic.getSize() + " · Classic");
        }
    }

    private JRadioButton createSetupRadio(String text, boolean selected) {
        JRadioButton option = new JRadioButton(text, selected);
        option.setFont(new Font("Segoe UI", Font.BOLD, 14));
        option.setForeground(Color.WHITE);
        option.setOpaque(false);
        option.setFocusPainted(false);
        option.setIcon(UIManager.getIcon("RadioButton.icon"));
        option.setSelectedIcon(UIManager.getIcon("RadioButton.icon"));
        return option;
    }

    private void selectImageForSetup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose an image for the sliding puzzle");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Image files (PNG, JPG, GIF, BMP)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            BufferedImage image = ImageIO.read(chooser.getSelectedFile());
            if (image == null) {
                throw new IOException("The selected file is not a supported image.");
            }
            addGalleryImage(chooser.getSelectedFile(), image);
            puzzleImage = image;
            gameBoard.setPuzzleImage(image);
            selectedImageLabel.setText(chooser.getSelectedFile().getName());
            pictureMode = true;
            showScreen("setup");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Could Not Load Image", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startFromSetup(boolean freshGame) {
        if (pictureMode && puzzleImage == null) {
            selectImageForSetup();
            if (puzzleImage == null) {
                return;
            }
        }
        if (!pictureMode) {
            puzzleImage = null;
            gameBoard.setPuzzleImage(null);
            previewMenuItem.setEnabled(false);
            dailyMode = false;
            dailyModeMenuItem.setText("Daily Challenge");
            updateAutoSolveButton();
        } else {
            gameBoard.setPuzzleImage(puzzleImage);
            previewMenuItem.setEnabled(true);
        }
        boolean needsFreshGame = freshGame || !gameStarted;
        boolean resumeGame = !needsFreshGame && paused;
        gameStarted = true;
        continueButton.setEnabled(true);
        showScreen("game");

        if (needsFreshGame) {
            startNewGame();
        } else if (resumeGame) {
            paused = false;
            pauseResumeButton.setText("PAUSE");
            refreshModeLabel();
            if ((puzzleLogic.getMoveCount() > 0 || solverBusy || solverPlaying) && !puzzleLogic.isSolved()) {
                gameTimer.start();
            }
            if (solverPlaying && solverPlaybackTimer != null) {
                solverPlaybackTimer.start();
            }
        }
    }

    private void showScreen(String screen) {
        screenLayout.show(screenCards, screen);
        screenCards.revalidate();
        screenCards.repaint();
    }

    private static class LaunchBackgroundPanel extends JPanel {
        LaunchBackgroundPanel() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(17, 39, 61), width, height,
                new Color(20, 83, 88)));
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(10, 27, 45, 95));
            for (int x = 0; x < width; x += 34) {
                g.drawLine(x, 0, x, height);
            }
            for (int y = 0; y < height; y += 34) {
                g.drawLine(0, y, width, y);
            }
            Path2D.Float ridge = new Path2D.Float();
            ridge.moveTo(0, height * 0.70);
            ridge.lineTo(width * 0.36, height * 0.47);
            ridge.lineTo(width * 0.69, height * 0.72);
            ridge.lineTo(width, height * 0.48);
            ridge.lineTo(width, height);
            ridge.lineTo(0, height);
            ridge.closePath();
            g.setColor(new Color(11, 45, 61, 180));
            g.fill(ridge);
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(new Color(126, 245, 218, 70));
            for (int y = (int) (height * 0.76); y < height; y += 14) {
                g.drawLine(0, y, width, y - 18);
            }
            g.dispose();
        }
    }

    private static class SetupBoardPreview extends JPanel {
        private final Color[] tiles = {
            new Color(76, 174, 164), new Color(83, 184, 173), new Color(76, 174, 164),
            new Color(83, 184, 173), new Color(242, 132, 91), new Color(83, 184, 173),
            new Color(76, 174, 164), new Color(83, 184, 173)
        };

        SetupBoardPreview() {
            setOpaque(false);
            setPreferredSize(new Dimension(174, 174));
            setMaximumSize(new Dimension(174, 174));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int gap = 3;
            int cell = (Math.min(getWidth(), getHeight()) - 12 - gap * 2) / 3;
            int boardSize = cell * 3 + gap * 2;
            int left = (getWidth() - boardSize) / 2;
            int top = (getHeight() - boardSize) / 2;
            g.setColor(new Color(244, 164, 60));
            g.fillRoundRect(left - 4, top - 4, boardSize + 8, boardSize + 8, 14, 14);
            for (int value = 0; value < 9; value++) {
                int x = left + (value % 3) * (cell + gap);
                int y = top + (value / 3) * (cell + gap);
                g.setColor(value == 8 ? new Color(12, 49, 55) : tiles[value]);
                g.fillRoundRect(x, y, cell, cell, 7, 7);
                if (value < 8) {
                    String number = String.valueOf(value + 1);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 26));
                    FontMetrics metrics = g.getFontMetrics();
                    g.setColor(Color.WHITE);
                    g.drawString(number, x + (cell - metrics.stringWidth(number)) / 2,
                        y + (cell + metrics.getAscent()) / 2 - 3);
                }
            }
            g.dispose();
        }
    }

    private static class PuzzleArtwork extends JPanel {
        PuzzleArtwork() {
            setOpaque(false);
            setPreferredSize(new Dimension(430, 350));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int tile = Math.min(76, Math.max(48, getWidth() / 6));
            int gap = 5;
            int board = tile * 3 + gap * 2;
            int x = (getWidth() - board) / 2;
            int y = Math.max(25, (getHeight() - board) / 2);
            Color[] colors = {new Color(36, 145, 142), new Color(44, 113, 139),
                new Color(255, 116, 96), new Color(50, 132, 145),
                new Color(68, 168, 148), new Color(255, 153, 102),
                new Color(37, 119, 132), new Color(83, 176, 153)};
            for (int i = 0; i < 9; i++) {
                int col = i % 3;
                int row = i / 3;
                int tx = x + col * (tile + gap);
                int ty = y + row * (tile + gap);
                g.setColor(i == 8 ? new Color(9, 31, 47, 220) : colors[i]);
                g.fillRoundRect(tx, ty, tile, tile, 10, 10);
                g.setColor(new Color(255, 255, 255, 55));
                g.drawRoundRect(tx, ty, tile, tile, 10, 10);
                if (i < 8) {
                    String number = String.valueOf(i + 1);
                    g.setFont(new Font("Segoe UI", Font.BOLD, Math.max(22, tile / 2)));
                    FontMetrics metrics = g.getFontMetrics();
                    g.setColor(Color.WHITE);
                    g.drawString(number, tx + (tile - metrics.stringWidth(number)) / 2,
                        ty + (tile + metrics.getAscent()) / 2 - 3);
                }
            }
            g.dispose();
        }
    }

    /**
     * Builds the top section: Header title, subtitle, and stats cards.
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(BG_DARK);
        headerPanel.setBorder(new EmptyBorder(9, 24, 5, 24));

        JButton setupButton = createSmallButton("← SETUP", e -> {
            if (gameStarted && !paused) {
                paused = true;
                gameTimer.stop();
                if (solverPlaybackTimer != null) {
                    solverPlaybackTimer.stop();
                }
                pauseResumeButton.setText("RESUME");
                refreshModeLabel();
            }
            showScreen("setup");
        });
        setupButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(setupButton);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel titleLabel = new JLabel("SLIDE PUZZLE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 21));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        subtitleLabel = new JLabel("Arrange tiles 1 to 8 in numerical order");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel statsBar = new JPanel(new GridLayout(1, 4, 8, 0));
        statsBar.setOpaque(false);
        statsBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        movesValueLabel = new JLabel("0", SwingConstants.CENTER);
        timerValueLabel = new JLabel("00:00", SwingConstants.CENTER);
        bestValueLabel = new JLabel("—", SwingConstants.CENTER);
        challengeValueLabel = new JLabel("≤ 24", SwingConstants.CENTER);

        statsBar.add(createStatCard("MOVES", movesValueLabel));
        statsBar.add(createStatCard("TIME", timerValueLabel));
        statsBar.add(createStatCard("BEST", bestValueLabel));
        statsBar.add(createStatCard("MOVE GOAL", challengeValueLabel));

        headerPanel.add(statsBar);
        return headerPanel;
    }

    /**
     * Helper to create a stylish stat card box.
     */
    private JPanel createStatCard(String labelText, JLabel valueLabel) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(7, 5, 7, 5));

        JLabel title = new JLabel(labelText, SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 10));
        title.setForeground(TEXT_MUTED);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, labelText.equals("BEST") ? 14 : 18));
        valueLabel.setForeground(TEXT_PRIMARY);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 2)));
        card.add(valueLabel);
        return card;
    }

    /**
     * Builds the center section containing the settings bar and the puzzle board.
     */
    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(0, 26, 0, 26));

        JPanel controlsBar = new JPanel();
        controlsBar.setLayout(new BoxLayout(controlsBar, BoxLayout.Y_AXIS));
        controlsBar.setOpaque(false);

        gameBoard.setTheme(GameBoard.BoardTheme.OCEAN);
        modeLabel = new JLabel("Puzzle", SwingConstants.CENTER);
        modeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        modeLabel.setForeground(TEXT_MUTED);

        JMenu menu = new JMenu("☰ Menu");
        menu.setFont(new Font("Segoe UI", Font.BOLD, 12));
        menu.setForeground(TEXT_PRIMARY);
        JMenuBar menuBar = new JMenuBar();
        menuBar.setOpaque(false);
        menuBar.setBorderPainted(false);
        menuBar.add(menu);

        soundToggleMenuItem = new JMenuItem("Sound: On");
        soundToggleMenuItem.addActionListener(e -> toggleSound());
        JMenuItem imageMenuItem = new JMenuItem("Choose Image...");
        imageMenuItem.addActionListener(e -> choosePuzzleImage());
        JMenuItem galleryMenuItem = new JMenuItem("Image Gallery...");
        galleryMenuItem.addActionListener(e -> showImageGallery());
        previewMenuItem = new JMenuItem("Preview Image");
        previewMenuItem.setEnabled(false);
        previewMenuItem.addActionListener(e -> showImagePreview());
        dailyModeMenuItem = new JMenuItem("Daily Challenge");
        dailyModeMenuItem.addActionListener(e -> {
            if (dailyMode) {
                dailyMode = false;
                puzzleImage = null;
                gameBoard.setPuzzleImage(null);
                previewMenuItem.setEnabled(false);
                updateAutoSolveButton();
                modeLabel.setText("Puzzle");
                dailyModeMenuItem.setText("Daily Challenge");
                startNewGame();
            } else {
                startDailyChallenge();
            }
        });
        JMenuItem rulesMenuItem = new JMenuItem("Game Rules");
        rulesMenuItem.addActionListener(e -> showHelpDialog());
        menu.add(soundToggleMenuItem);
        menu.addSeparator();
        menu.add(imageMenuItem);
        menu.add(galleryMenuItem);
        menu.add(previewMenuItem);
        menu.addSeparator();
        menu.add(dailyModeMenuItem);
        menu.addSeparator();
        menu.add(rulesMenuItem);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightControls.setOpaque(false);
        rightControls.add(modeLabel);
        rightControls.add(menuBar);
        JPanel topControls = new JPanel(new BorderLayout());
        topControls.setOpaque(false);
        topControls.add(rightControls, BorderLayout.EAST);
        controlsBar.add(topControls);

        JPanel boardStage = new JPanel(new GridBagLayout());
        boardStage.setOpaque(false);
        boardStage.add(gameBoard);

        center.add(controlsBar, BorderLayout.NORTH);
        center.add(boardStage, BorderLayout.CENTER);
        return center;
    }

    private void changeBoardSize(int size) {
        if (puzzleLogic.getSize() == size) {
            return;
        }
        puzzleLogic.setBoardSize(size);
        gameBoard.setBoardSize(size);
        int highestTile = size * size - 1;
        subtitleLabel.setText("Arrange tiles 1 to " + highestTile + " in numerical order");
        startNewGame();
    }

    /**
     * Builds the bottom control buttons: New Game, Shuffle, and Reset.
     */
    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new GridLayout(2, 3, 10, 7));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(6, 26, 10, 26));

        ModernButton newGameBtn = new ModernButton("NEW GAME", ACCENT_EMERALD, Color.WHITE);
        ModernButton shuffleBtn = new ModernButton("SHUFFLE", ACCENT_AMBER, Color.WHITE);
        ModernButton resetBtn = new ModernButton("RESET", ACCENT_SLATE, Color.WHITE);
        clueButton = new ModernButton("CLUE (3)", new Color(14, 116, 144), Color.WHITE);
        pauseResumeButton = new ModernButton("PAUSE", new Color(71, 94, 85), Color.WHITE);
        autoSolveButton = new ModernButton("AI SOLVE", new Color(51, 112, 151), Color.WHITE);
        autoSolveButton.setEnabled(false);

        newGameBtn.setToolTipText("Starts a fresh puzzle");
        shuffleBtn.setToolTipText("Re-shuffles the current puzzle");
        resetBtn.setToolTipText("Resets puzzle back to starting arrangement");
        clueButton.setToolTipText("Spend a token to reveal a tile's correct destination briefly");
        pauseResumeButton.setToolTipText("Pause or resume the game clock and board");
        autoSolveButton.setToolTipText("Have the AI solve the current puzzle");

        newGameBtn.addActionListener(e -> {
            startNewGame();
        });

        shuffleBtn.addActionListener(e -> {
            startNewGame();
        });

        resetBtn.addActionListener(e -> {
            cancelAutoSolve();
            paused = false;
            pauseResumeButton.setText("PAUSE");
            refreshModeLabel();
            puzzleLogic.resetGame();
            gameTimer.reset();
            cluesRemaining = 3;
            updateStats();
            updateClueButton();
            gameBoard.clearClue();
            gameBoard.updateBoard();
            soundManager.playMoveSound();
        });

        clueButton.addActionListener(e -> useClue());
        pauseResumeButton.addActionListener(e -> togglePause());
        autoSolveButton.addActionListener(e -> startAutoSolve());

        bottom.add(newGameBtn);
        bottom.add(shuffleBtn);
        bottom.add(resetBtn);
        bottom.add(pauseResumeButton);
        bottom.add(autoSolveButton);
        bottom.add(clueButton);

        return bottom;
    }

    /**
     * Handles mouse clicks on any tile.
     */
    @Override
    public void onTileClicked(int row, int col) {
        processMove(row, col);
    }

    /**
     * Handles arrow key / WASD navigation.
     */
    @Override
    public void onDirectionKeyPressed(PuzzleLogic.Direction direction) {
        if (paused || solverBusy || solverPlaying || puzzleLogic.isSolved()) {
            return;
        }

        // Start timer on first move
        if (puzzleLogic.getMoveCount() == 0 && !gameTimer.isRunning()) {
            gameTimer.start();
        }

        boolean moved = puzzleLogic.moveByDirection(direction);
        if (moved) {
            handleValidMove();
        } else {
            soundManager.playInvalidMoveSound();
        }
    }

    /**
     * Processes a move attempt at (row, col).
     */
    private void processMove(int row, int col) {
        if (paused || solverBusy || solverPlaying || puzzleLogic.isSolved()) {
            return;
        }

        // Start timer when the player initiates gameplay
        if (puzzleLogic.getMoveCount() == 0 && !gameTimer.isRunning()) {
            gameTimer.start();
        }

        boolean moved = puzzleLogic.moveTile(row, col);
        if (moved) {
            handleValidMove();
        } else {
            soundManager.playInvalidMoveSound();
        }
    }

    /**
     * Executed when a valid move occurred.
     */
    private void handleValidMove() {
        soundManager.playMoveSound();
        updateStats();
        gameBoard.updateBoard();
        updateAutoSolveButton();

        // Check for victory condition
        if (puzzleLogic.isSolved()) {
            gameTimer.stop();
            if (solverPlaybackTimer != null) {
                solverPlaybackTimer.stop();
            }
            solverPlaying = false;
            updateAutoSolveButton();
            soundManager.playWinSound();
            handleVictory();
        }
    }

    /**
     * Displays victory celebrations and updates session high scores.
     */
    private void handleVictory() {
        int finalMoves = puzzleLogic.getMoveCount();
        int finalSeconds = gameTimer.getSecondsElapsed();
        String finalTime = gameTimer.getFormattedTime();

        boolean isNewBest = false;
        if (bestMoves == -1 || finalMoves < bestMoves || (finalMoves == bestMoves && finalSeconds < bestSeconds)) {
            bestMoves = finalMoves;
            bestSeconds = finalSeconds;
            bestValueLabel.setText(finalMoves + " (" + finalTime + ")");
            isNewBest = true;
        }

        int target = getMoveTarget();
        int medal = finalMoves <= target ? 3 : finalMoves <= target + 10 ? 2 : 1;
        String medalName = medal == 3 ? "GOLD" : medal == 2 ? "SILVER" : "BRONZE";
        challengeValueLabel.setText(medalName);
        if (dailyMode) {
            saveDailyResult(finalMoves, finalSeconds);
        }
        showWinDialog(finalMoves, finalTime, isNewBest, medalName);
    }

    /**
     * Custom-styled victory dialog.
     */
    private void showWinDialog(int moves, String time, boolean isNewBest, String medalName) {
        JDialog dialog = new JDialog(this, "Victory!", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(BG_CARD);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(24, 30, 20, 30));

        JLabel trophy = new JLabel("🏆", SwingConstants.CENTER);
        trophy.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        trophy.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel congrats = new JLabel("Congratulations!", SwingConstants.CENTER);
        congrats.setFont(new Font("Segoe UI", Font.BOLD, 20));
        congrats.setForeground(ACCENT_EMERALD);
        congrats.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel(dailyMode ? "Daily puzzle complete!" : "Puzzle complete!", SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msg.setForeground(TEXT_PRIMARY);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Stats summary
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 8, 4));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(new EmptyBorder(12, 0, 12, 0));

        JLabel movesLbl = new JLabel("Total Moves:");
        movesLbl.setForeground(TEXT_MUTED);
        JLabel movesVal = new JLabel(String.valueOf(moves), SwingConstants.RIGHT);
        movesVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        movesVal.setForeground(TEXT_PRIMARY);

        JLabel timeLbl = new JLabel("Time Taken:");
        timeLbl.setForeground(TEXT_MUTED);
        JLabel timeVal = new JLabel(time, SwingConstants.RIGHT);
        timeVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        timeVal.setForeground(TEXT_PRIMARY);

        statsPanel.add(movesLbl);
        statsPanel.add(movesVal);
        statsPanel.add(timeLbl);
        statsPanel.add(timeVal);

        content.add(trophy);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(congrats);
        content.add(Box.createRigidArea(new Dimension(0, 4)));
        content.add(msg);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        if (puzzleImage != null) {
            ImageRevealPanel reveal = new ImageRevealPanel(puzzleImage, puzzleLogic.getSize());
            reveal.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(reveal);
            reveal.startReveal();
            content.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        JLabel medalLabel = new JLabel(medalName + " MEDAL", SwingConstants.CENTER);
        medalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        medalLabel.setForeground(medalName.equals("GOLD") ? ACCENT_AMBER
            : medalName.equals("SILVER") ? new Color(203, 213, 225) : new Color(205, 127, 70));
        medalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(medalLabel);
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        content.add(statsPanel);

        if (isNewBest) {
            JLabel recordLabel = new JLabel("★ NEW SESSION BEST! ★", SwingConstants.CENTER);
            recordLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            recordLabel.setForeground(ACCENT_AMBER);
            recordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(recordLabel);
            content.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // Action Buttons: Play Again & Close
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);

        ModernButton playAgainBtn = new ModernButton("PLAY AGAIN", ACCENT_EMERALD, Color.WHITE);
        ModernButton closeBtn = new ModernButton("CLOSE", ACCENT_SLATE, Color.WHITE);

        playAgainBtn.addActionListener(e -> {
            dialog.dispose();
            startNewGame();
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(playAgainBtn);
        btnPanel.add(closeBtn);
        content.add(btnPanel);

        dialog.add(content);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Displays a help/instructions dialog.
     */
    private void showHelpDialog() {
        JDialog dialog = new JDialog(this, "Game Rules & Help", true);
        dialog.getContentPane().setBackground(BG_CARD);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("How to Play", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        String instructionsHtml = "<html>"
            + "<body style='font-family:Segoe UI; font-size:11px; color:#42584f; width:280px;'>"
            + "<p><b>Goal:</b> Arrange tiles 1 to " + (puzzleLogic.getSize() * puzzleLogic.getSize() - 1)
            + " in order, leaving the empty slot in the bottom-right corner.</p><br>"
            + "<p><b>Controls:</b></p>"
            + "<ul>"
            + "<li><b>Click:</b> Click any tile adjacent to the empty space to slide it.</li>"
            + "<li><b>Keyboard:</b> Use <b>Arrow Keys</b> or <b>W/A/S/D</b> to slide tiles.</li>"
            + "</ul>"
            + "<p><b>Buttons:</b></p>"
            + "<ul>"
            + "<li><b>New Game:</b> Generates a fresh solvable puzzle.</li>"
            + "<li><b>Shuffle:</b> Re-scrambles current tiles.</li>"
            + "<li><b>Reset:</b> Restores puzzle back to its starting layout.</li>"
            + "<li><b>Pause:</b> Stops the clock and freezes moves; press Resume to continue.</li>"
            + "<li><b>AI Solve:</b> Searches for a solution on 3×3 through 6×6 boards and animates legal moves. "
            + "Search is bounded, so very difficult boards may exceed its limit.</li>"
            + "</ul>"
            + "<p><b>Board and scoring:</b></p>"
            + "<ul>"
            + "<li>Choose a 3 x 3, 4 x 4, 5 x 5, or 6 x 6 board.</li>"
            + "<li>Meet the move goal for gold; up to ten extra moves earns silver. Every clear earns bronze.</li>"
            + "<li><b>Clue:</b> Spend one of three tokens to briefly mark a tile's correct destination.</li>"
            + "<li><b>Daily:</b> Everyone gets the same UTC-date puzzle; your best daily result is saved.</li>"
            + "<li><b>Gallery:</b> Add your own PNG, JPG, GIF, or BMP pictures and reuse them in image puzzles.</li>"
            + "<li><b>Preview:</b> View the complete image while solving.</li>"
            + "</ul>"
            + "<p style='color:#196b54;'><i>Note: All puzzles generated are mathematically verified to be solvable!</i></p>"
            + "</body></html>";

        JLabel contentLabel = new JLabel(instructionsHtml);
        contentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ModernButton okBtn = new ModernButton("GOT IT", ACCENT_EMERALD, Color.WHITE);
        okBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        okBtn.addActionListener(e -> dialog.dispose());

        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(contentLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 16)));
        panel.add(okBtn);

        dialog.add(panel);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Toggles sound on and off.
     */
    private void toggleSound() {
        soundManager.toggleSound();
        if (soundManager.isSoundEnabled()) {
            soundToggleMenuItem.setText("Sound: On");
            soundManager.playMoveSound();
        } else {
            soundToggleMenuItem.setText("Sound: Off");
        }
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            gameTimer.stop();
            if (solverPlaybackTimer != null) {
                solverPlaybackTimer.stop();
            }
            pauseResumeButton.setText("RESUME");
            modeLabel.setText("Paused");
            updateAutoSolveButton();
            return;
        }

        pauseResumeButton.setText("PAUSE");
        refreshModeLabel();
        updateAutoSolveButton();
        if ((puzzleLogic.getMoveCount() > 0 || solverBusy || solverPlaying) && !puzzleLogic.isSolved()) {
            gameTimer.start();
        }
        if (solverPlaying && solverPlaybackTimer != null) {
            solverPlaybackTimer.start();
        }
    }

    private void startAutoSolve() {
        if (paused || solverBusy || solverPlaying || puzzleLogic.isSolved()) {
            return;
        }
        solverBusy = true;
        updateAutoSolveButton();
        refreshModeLabel();
        if (puzzleLogic.getMoveCount() == 0 && !gameTimer.isRunning()) {
            gameTimer.start();
        }
        java.util.List<Integer> knownSolution = puzzleLogic.getKnownSolutionPath();
        int generation = ++solverGeneration;
        solverWorker = new SwingWorker<java.util.List<Integer>, Void>() {
            @Override
            protected java.util.List<Integer> doInBackground() {
                return knownSolution;
            }

            @Override
            protected void done() {
                if (generation != solverGeneration) {
                    return;
                }
                solverBusy = false;
                if (isCancelled()) {
                    updateAutoSolveButton();
                    return;
                }
                String solverFailureMessage = null;
                try {
                    solverMoves = get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    solverMoves = null;
                    solverFailureMessage = "The AI search was interrupted.";
                } catch (java.util.concurrent.ExecutionException ex) {
                    solverMoves = null;
                    Throwable cause = ex.getCause();
                    solverFailureMessage = cause == null
                        ? "The AI search failed."
                        : "The AI search failed: " + cause.getMessage();
                }
                if (solverMoves == null) {
                    refreshModeLabel();
                    updateAutoSolveButton();
                    JOptionPane.showMessageDialog(SlidingPuzzle.this,
                        solverFailureMessage == null
                            ? "The AI could not find a solution within its search limit. The puzzle is unchanged."
                            : solverFailureMessage + " The puzzle is unchanged.",
                        "AI Solver", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                solverMoveIndex = 0;
                solverPlaying = !solverMoves.isEmpty();
                refreshModeLabel();
                updateAutoSolveButton();
                if (solverPlaying) {
                    solverPlaybackTimer = new Timer(260, e -> playNextSolverMove());
                    solverPlaybackTimer.setCoalesce(true);
                    if (!paused) {
                        solverPlaybackTimer.start();
                    }
                }
            }
        };
        solverWorker.execute();
    }

    private void playNextSolverMove() {
        if (paused || !solverPlaying) {
            return;
        }
        if (solverMoveIndex >= solverMoves.size()) {
            solverPlaybackTimer.stop();
            solverPlaying = false;
            modeLabel.setText("AI stopped before completion");
            updateAutoSolveButton();
            return;
        }

        int targetValue = solverMoves.get(solverMoveIndex++);
        for (int row = 0; row < puzzleLogic.getSize(); row++) {
            for (int col = 0; col < puzzleLogic.getSize(); col++) {
                if (puzzleLogic.getTile(row, col) == targetValue) {
                    if (puzzleLogic.moveTile(row, col)) {
                        handleValidMove();
                    } else {
                        solverPlaybackTimer.stop();
                        solverPlaying = false;
                        modeLabel.setText("AI move unavailable");
                        updateAutoSolveButton();
                    }
                    return;
                }
            }
        }
        solverPlaybackTimer.stop();
        solverPlaying = false;
        modeLabel.setText("AI move unavailable");
        updateAutoSolveButton();
    }

    private void cancelAutoSolve() {
        solverGeneration++;
        if (solverWorker != null && !solverWorker.isDone()) {
            solverWorker.cancel(true);
        }
        if (solverPlaybackTimer != null) {
            solverPlaybackTimer.stop();
        }
        solverBusy = false;
        solverPlaying = false;
        solverMoves = java.util.Collections.emptyList();
        updateAutoSolveButton();
    }

    private void updateAutoSolveButton() {
        if (autoSolveButton == null) {
            return;
        }
        autoSolveButton.setText(solverBusy ? "THINKING..." : solverPlaying ? "SOLVING..." : "AI SOLVE");
        autoSolveButton.setEnabled(!solverBusy && !solverPlaying && !puzzleLogic.isSolved() && !paused);
        autoSolveButton.setToolTipText("Have the AI solve the current 3x3–6x6 puzzle");
    }

    private void refreshModeLabel() {
        if (modeLabel == null) {
            return;
        }
        if (paused) {
            modeLabel.setText("Paused");
        } else if (solverBusy) {
            modeLabel.setText("AI thinking...");
        } else if (solverPlaying) {
            modeLabel.setText("AI solving");
        } else if (puzzleImage != null) {
            modeLabel.setText("Image puzzle");
        } else if (dailyMode) {
            modeLabel.setText("Daily " + dailyDate());
        } else {
            modeLabel.setText("Puzzle");
        }
    }

    private int currentShuffleMoves() {
        int sizeScale = Math.max(1, puzzleLogic.getSize() - 2);
        return sizeScale * (dailyMode ? 40 : SHUFFLE_MOVES);
    }

    private int getMoveTarget() {
        return Math.max(1, puzzleLogic.getSize() - 2) * (dailyMode ? 40 : MOVE_GOAL);
    }

    private void startNewGame() {
        cancelAutoSolve();
        paused = false;
        if (pauseResumeButton != null) {
            pauseResumeButton.setText("PAUSE");
        }
        refreshModeLabel();
        gameBoard.clearClue();
        puzzleLogic.setCurrentDifficulty(PuzzleLogic.Difficulty.MEDIUM);
        if (dailyMode) {
            puzzleLogic.shuffle(currentShuffleMoves(), dailyDate().toEpochDay());
        } else {
            puzzleLogic.shuffle(currentShuffleMoves());
        }
        gameTimer.reset();
        cluesRemaining = 3;
        clueButton.setText("CLUE (" + cluesRemaining + ")");
        clueButton.setEnabled(true);
        updateStats();
        gameBoard.updateBoard();
        updateAutoSolveButton();
        soundManager.playShuffleSound();
    }

    private LocalDate dailyDate() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    private void startDailyChallenge() {
        dailyMode = true;
        puzzleImage = null;
        gameBoard.setPuzzleImage(null);
        previewMenuItem.setEnabled(false);
        modeLabel.setText("Daily " + dailyDate());
        dailyModeMenuItem.setText("Return to Puzzle");
        startNewGame();
        String dailyKey = dailyProgressKey();
        int dailyBestMoves = progress.getInt("daily.moves." + dailyKey, Integer.MAX_VALUE);
        if (dailyBestMoves < Integer.MAX_VALUE) {
            int dailyBestSeconds = progress.getInt("daily.seconds." + dailyKey, 0);
            bestValueLabel.setText(dailyBestMoves + " ("
                + GameServices.GameTimer.formatTime(dailyBestSeconds) + ")");
        } else {
            bestValueLabel.setText("—");
        }
        if (progress.getBoolean("daily.completed." + dailyKey, false)) {
            JOptionPane.showMessageDialog(this, "You have already completed today's shared puzzle. Replay it to improve your score.",
                "Daily Puzzle", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void useClue() {
        if (!paused && !solverBusy && !solverPlaying && cluesRemaining > 0 && gameBoard.showClue()) {
            cluesRemaining--;
            updateClueButton();
        }
    }

    private void updateClueButton() {
        clueButton.setText("CLUE (" + cluesRemaining + ")");
        clueButton.setEnabled(cluesRemaining > 0);
    }

    private void saveDailyResult(int moves, int seconds) {
        String dailyKey = dailyProgressKey();
        int previousMoves = progress.getInt("daily.moves." + dailyKey, Integer.MAX_VALUE);
        int previousSeconds = progress.getInt("daily.seconds." + dailyKey, Integer.MAX_VALUE);
        if (moves < previousMoves || (moves == previousMoves && seconds < previousSeconds)) {
            progress.putInt("daily.moves." + dailyKey, moves);
            progress.putInt("daily.seconds." + dailyKey, seconds);
        }
        progress.putBoolean("daily.completed." + dailyKey, true);
    }

    private String dailyProgressKey() {
        return puzzleLogic.getSize() + "." + dailyDate();
    }

    private void choosePuzzleImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose an image for the sliding puzzle");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Image files (PNG, JPG, GIF, BMP)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            BufferedImage selectedImage = ImageIO.read(chooser.getSelectedFile());
            if (selectedImage == null) {
                throw new IOException("The selected file is not a supported image.");
            }
            addGalleryImage(chooser.getSelectedFile(), selectedImage);
            activateImage(chooser.getSelectedFile(), selectedImage);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Could Not Load Image",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void activateImage(File file, BufferedImage image) {
        dailyMode = false;
        dailyModeMenuItem.setText("Daily Challenge");
        puzzleImage = image;
        gameBoard.setPuzzleImage(image);
        previewMenuItem.setEnabled(true);
        modeLabel.setText("Image puzzle");
        modeLabel.setToolTipText(file.getName());
        startNewGame();
    }

    private void showImageGallery() {
        if (imageGallery.isEmpty()) {
            choosePuzzleImage();
            return;
        }
        Object[] choices = imageGallery.stream().map(item -> item.file.getName()).toArray();
        Object choice = JOptionPane.showInputDialog(this, "Choose a saved puzzle picture:", "Image Gallery",
            JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
        if (choice != null) {
            for (GalleryImage item : imageGallery) {
                if (item.file.getName().equals(choice)) {
                    activateImage(item.file, item.image);
                    return;
                }
            }
        }
    }

    private void addGalleryImage(File file, BufferedImage image) {
        for (int i = 0; i < imageGallery.size(); i++) {
            if (imageGallery.get(i).file.equals(file)) {
                imageGallery.set(i, new GalleryImage(file, image));
                persistImageGallery();
                return;
            }
        }
        imageGallery.add(new GalleryImage(file, image));
        while (imageGallery.size() > 12) {
            imageGallery.remove(0);
        }
        persistImageGallery();
    }

    private void loadImageGallery() {
        String savedPaths = progress.get("image.gallery", "");
        for (String path : savedPaths.split("\\R")) {
            if (path.isEmpty()) {
                continue;
            }
            File file = new File(path);
            if (file.isFile()) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    if (image != null) {
                        imageGallery.add(new GalleryImage(file, image));
                    } else {
                        System.err.println("Unable to load gallery image: unsupported image format: " + file);
                    }
                } catch (IOException ex) {
                    System.err.println("Unable to load gallery image " + file + ": " + ex.getMessage());
                }
            }
        }
    }

    private void persistImageGallery() {
        StringBuilder paths = new StringBuilder();
        for (GalleryImage item : imageGallery) {
            if (paths.length() > 0) {
                paths.append('\n');
            }
            paths.append(item.file.getAbsolutePath());
        }
        progress.put("image.gallery", paths.toString());
    }

    private void showImagePreview() {
        if (puzzleImage == null) {
            return;
        }
        double scale = Math.min(420.0 / puzzleImage.getWidth(), 315.0 / puzzleImage.getHeight());
        int width = Math.max(1, (int) (puzzleImage.getWidth() * scale));
        int height = Math.max(1, (int) (puzzleImage.getHeight() * scale));
        Image scaledImage = puzzleImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        JOptionPane.showMessageDialog(this, new JLabel(new ImageIcon(scaledImage)),
            "Image Puzzle Preview", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Updates moves and timer counters on screen.
     */
    private void updateStats() {
        movesValueLabel.setText(String.valueOf(puzzleLogic.getMoveCount()));
        challengeValueLabel.setText("≤ " + getMoveTarget());
    }

    private static class GalleryImage {
        private final File file;
        private final BufferedImage image;

        GalleryImage(File file, BufferedImage image) {
            this.file = file;
            this.image = image;
        }
    }

    private static class ImageRevealPanel extends JPanel {
        private final BufferedImage image;
        private final int gridSize;
        private int visibleTiles;
        private Timer revealTimer;

        ImageRevealPanel(BufferedImage image, int gridSize) {
            this.image = image;
            this.gridSize = gridSize;
            setPreferredSize(new Dimension(180, 180));
            setBackground(BG_DARK);
            setBorder(BorderFactory.createLineBorder(BORDER_CARD));
            revealTimer = new Timer(120, e -> {
                visibleTiles++;
                repaint();
                if (visibleTiles >= gridSize * gridSize) {
                    revealTimer.stop();
                }
            });
        }

        void startReveal() {
            revealTimer.start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int cellWidth = getWidth() / gridSize;
            int cellHeight = getHeight() / gridSize;
            for (int index = 0; index < visibleTiles; index++) {
                int row = index / gridSize;
                int col = index % gridSize;
                int x = col * cellWidth;
                int y = row * cellHeight;
                g2.setClip(x, y, cellWidth, cellHeight);
                g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                g2.setClip(null);
                g2.setColor(new Color(255, 255, 255, 170));
                g2.drawRect(x, y, cellWidth, cellHeight);
            }
            g2.dispose();
        }
    }

    /**
     * Creates a small styled button for the sub-bar.
     */
    private JButton createSmallButton(String text, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(TEXT_PRIMARY);
        btn.setBackground(BG_CARD);
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_CARD, 1, true), new EmptyBorder(4, 8, 4, 8)));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }

    /**
     * Custom styled button with smooth rounded corners and hover feedback.
     */
    private static class ModernButton extends JButton {
        private final Color baseColor;
        private final Color hoverColor;

        public ModernButton(String text, Color baseColor, Color textColor) {
            super(text);
            this.baseColor = baseColor;
            this.hoverColor = baseColor.brighter();

            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setForeground(textColor);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(110, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color c = getModel().isRollover() ? hoverColor : baseColor;
            if (getModel().isPressed()) {
                c = baseColor.darker();
            }

            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));

            // Text
            FontMetrics fm = g2.getFontMetrics(getFont());
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() + fm.getAscent()) / 2 - 2;

            g2.setColor(getForeground());
            g2.setFont(getFont());
            g2.drawString(getText(), x, y);

            g2.dispose();
        }
    }

    /**
     * Application entry point.
     * Launches GUI safely on Swing Event Dispatch Thread (EDT).
     */
    public static void main(String[] args) {
        // Use system look and feel if available, while preserving custom styling
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | javax.swing.UnsupportedLookAndFeelException ex) {
            System.err.println("Unable to set the system look and feel: " + ex.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            SlidingPuzzle app = new SlidingPuzzle();
            app.setVisible(true);
        });
    }
}
