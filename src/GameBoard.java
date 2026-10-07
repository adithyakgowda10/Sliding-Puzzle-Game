import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * GameBoard.java
 * 
 * The visual 3x3 through 6x6 puzzle board component for the Sliding Puzzle game.
 * 
 * Key Responsibilities:
 * - Renders a compact, edge-aligned grid of custom-styled tile buttons.
 * - Handles tile click events and maps them to grid coordinates (row, col).
 * - Implements keyboard navigation (Arrow Keys and W/A/S/D).
 * - Distinguishes movable tiles with hand cursors and hover effects.
 * - Visually highlights tiles that are currently in their correct solved position.
 * - Renders the empty slot with a modern recessed card aesthetic.
 */
public class GameBoard extends JPanel {

    public enum BoardTheme {
        OCEAN("Ocean", new Color(229, 239, 234), new Color(213, 224, 218), new Color(154, 174, 162),
            new Color(43, 116, 105), new Color(55, 139, 125), new Color(201, 119, 59), new Color(220, 143, 81)),
        GARDEN("Garden", new Color(231, 238, 243), new Color(215, 226, 233), new Color(151, 173, 185),
            new Color(55, 112, 151), new Color(70, 134, 174), new Color(173, 91, 82), new Color(197, 112, 99)),
        SUNSET("Sunset", new Color(245, 235, 224), new Color(231, 216, 198), new Color(183, 157, 130),
            new Color(181, 99, 61), new Color(205, 119, 77), new Color(53, 121, 109), new Color(69, 146, 131));

        private final String displayName;
        private final Color board;
        private final Color empty;
        private final Color emptyBorder;
        private final Color tile;
        private final Color tileHover;
        private final Color correct;
        private final Color correctHover;

        BoardTheme(String displayName, Color board, Color empty, Color emptyBorder,
                Color tile, Color tileHover, Color correct, Color correctHover) {
            this.displayName = displayName;
            this.board = board;
            this.empty = empty;
            this.emptyBorder = emptyBorder;
            this.tile = tile;
            this.tileHover = tileHover;
            this.correct = correct;
            this.correctHover = correctHover;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public interface TileClickListener {
        void onTileClicked(int row, int col);
        void onDirectionKeyPressed(PuzzleLogic.Direction direction);
    }

    private final PuzzleLogic puzzleLogic;
    private TileButton[][] tileButtons;
    private TileClickListener listener;
    private BufferedImage puzzleImage;

    private BoardTheme theme = BoardTheme.OCEAN;
    private int clueTileValue = -1;
    private Timer clueTimer;
    private static final Color TILE_TEXT_COLOR = Color.WHITE;
    private static final Color CORRECT_IMAGE_BORDER = new Color(144, 238, 144);

    /**
     * Constructs the GameBoard panel.
     *
     * @param puzzleLogic the shared game logic instance
     */
    public GameBoard(PuzzleLogic puzzleLogic) {
        this.puzzleLogic = puzzleLogic;

        setLayout(new GridLayout(puzzleLogic.getSize(), puzzleLogic.getSize(), 0, 0));
        setBackground(theme.board);
        setBorder(BorderFactory.createLineBorder(theme.emptyBorder, 2));
        setPreferredSize(new Dimension(360, 360));

        tileButtons = new TileButton[puzzleLogic.getSize()][puzzleLogic.getSize()];
        initializeButtons();
        setupKeyBindings();
        updateBoard();
    }

    /**
     * Registers the listener for tile click and keyboard events.
     */
    public void setTileClickListener(TileClickListener listener) {
        this.listener = listener;
    }

    public void setPuzzleImage(BufferedImage image) {
        puzzleImage = image;
        updateBoard();
    }

    public void setTheme(BoardTheme theme) {
        this.theme = theme;
        setBackground(theme.board);
        setBorder(BorderFactory.createLineBorder(theme.emptyBorder, 2));
        updateBoard();
    }

    public void setBoardSize(int size) {
        if (clueTimer != null) {
            clueTimer.stop();
        }
        clueTileValue = -1;
        removeAll();
        tileButtons = new TileButton[size][size];
        setLayout(new GridLayout(size, size, 0, 0));
        initializeButtons();
        updateBoard();
        revalidate();
        repaint();
    }

    public boolean showClue() {
        for (int row = 0; row < puzzleLogic.getSize(); row++) {
            for (int col = 0; col < puzzleLogic.getSize(); col++) {
                int value = puzzleLogic.getTile(row, col);
                if (value != PuzzleLogic.EMPTY_TILE && !puzzleLogic.isTileInCorrectPosition(row, col)) {
                    clueTileValue = value;
                    updateBoard();
                    if (clueTimer != null) {
                        clueTimer.stop();
                    }
                    clueTimer = new Timer(2200, e -> {
                        clueTileValue = -1;
                        updateBoard();
                    });
                    clueTimer.setRepeats(false);
                    clueTimer.start();
                    return true;
                }
            }
        }
        return false;
    }

    public void clearClue() {
        clueTileValue = -1;
        if (clueTimer != null) {
            clueTimer.stop();
        }
        updateBoard();
    }

    /**
    * Initializes a square array of custom TileButtons for the selected board size.
     */
    private void initializeButtons() {
        for (int r = 0; r < puzzleLogic.getSize(); r++) {
            for (int c = 0; c < puzzleLogic.getSize(); c++) {
                final int row = r;
                final int col = c;
                TileButton btn = new TileButton();

                btn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (listener != null) {
                            listener.onTileClicked(row, col);
                        }
                    }
                });

                tileButtons[r][c] = btn;
                add(btn);
            }
        }
    }

    /**
     * Configures keyboard shortcuts for Arrow keys and WASD.
     * Uses Swing's WHEN_IN_FOCUSED_WINDOW input map so keys work
     * regardless of which control currently has focus.
     */
    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        bindKey(im, am, KeyEvent.VK_UP, "moveUp", PuzzleLogic.Direction.UP);
        bindKey(im, am, KeyEvent.VK_W, "moveUpW", PuzzleLogic.Direction.UP);

        bindKey(im, am, KeyEvent.VK_DOWN, "moveDown", PuzzleLogic.Direction.DOWN);
        bindKey(im, am, KeyEvent.VK_S, "moveDownS", PuzzleLogic.Direction.DOWN);

        bindKey(im, am, KeyEvent.VK_LEFT, "moveLeft", PuzzleLogic.Direction.LEFT);
        bindKey(im, am, KeyEvent.VK_A, "moveLeftA", PuzzleLogic.Direction.LEFT);

        bindKey(im, am, KeyEvent.VK_RIGHT, "moveRight", PuzzleLogic.Direction.RIGHT);
        bindKey(im, am, KeyEvent.VK_D, "moveRightD", PuzzleLogic.Direction.RIGHT);
    }

    private void bindKey(InputMap im, ActionMap am, int keyCode, String actionKey, PuzzleLogic.Direction dir) {
        im.put(KeyStroke.getKeyStroke(keyCode, 0), actionKey);
        am.put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (listener != null) {
                    listener.onDirectionKeyPressed(dir);
                }
            }
        });
    }

    /**
    * Refreshes the visual state of every tile on the board.
     * Called immediately after any move, shuffle, or reset.
     */
    public void updateBoard() {
        for (int r = 0; r < puzzleLogic.getSize(); r++) {
            for (int c = 0; c < puzzleLogic.getSize(); c++) {
                int tileValue = puzzleLogic.getTile(r, c);
                TileButton btn = tileButtons[r][c];
                int clueRow = clueTileValue < 1 ? -1 : (clueTileValue - 1) / puzzleLogic.getSize();
                int clueCol = clueTileValue < 1 ? -1 : (clueTileValue - 1) % puzzleLogic.getSize();
                btn.setClue(r == clueRow && c == clueCol ? clueTileValue : -1);

                if (tileValue == PuzzleLogic.EMPTY_TILE) {
                    // Empty space
                    btn.setText("");
                    btn.setEmpty(true);
                    btn.setTheme(theme);
                    btn.setEnabled(false);
                    btn.setCursor(Cursor.getDefaultCursor());
                    btn.setCorrect(false);
                } else {
                    // The solved position determines which image fragment belongs on this tile.
                    btn.setTileImage(puzzleImage, tileValue, puzzleLogic.getSize());
                    btn.setTheme(theme);
                    btn.setText(puzzleImage == null ? String.valueOf(tileValue) : "");
                    btn.setEmpty(false);
                    btn.setEnabled(true);

                    boolean canMove = puzzleLogic.canMove(r, c);
                    boolean isCorrect = puzzleLogic.isTileInCorrectPosition(r, c);

                    btn.setMovable(canMove);
                    btn.setCorrect(isCorrect);
                    btn.setCursor(canMove ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                }
                btn.repaint();
            }
        }
    }

    /**
     * Custom JButton with rounded corners, smooth hover effects,
     * status highlights, and clean typography.
     */
    private static class TileButton extends JButton {

        private boolean isEmpty = false;
        private boolean isMovable = false;
        private boolean isCorrect = false;
        private boolean isHovered = false;
        private BufferedImage tileImage;
        private int tileValue;
        private int gridSize = PuzzleLogic.SIZE;
        private int clueLabelValue = -1;
        private BoardTheme theme = BoardTheme.OCEAN;

        public TileButton() {
            super();
            setFont(new Font("Segoe UI", Font.BOLD, 36));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isEmpty && isMovable) {
                        isHovered = true;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        public void setEmpty(boolean empty) {
            this.isEmpty = empty;
        }

        public void setMovable(boolean movable) {
            this.isMovable = movable;
        }

        public void setCorrect(boolean correct) {
            this.isCorrect = correct;
        }

        public void setTileImage(BufferedImage image, int value, int gridSize) {
            tileImage = image;
            tileValue = value;
            this.gridSize = gridSize;
        }

        public void setClue(int value) {
            clueLabelValue = value;
        }

        public void setTheme(BoardTheme theme) {
            this.theme = theme;
        }

        private void paintClue(Graphics2D g2, int width) {
            if (clueLabelValue < 1) {
                return;
            }
            int diameter = 34;
            g2.setColor(new Color(15, 23, 42, 220));
            g2.fillOval(width - diameter - 8, 8, diameter, diameter);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
            String label = String.valueOf(clueLabelValue);
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(label, width - diameter / 2 - metrics.stringWidth(label) / 2 - 8,
                8 + diameter / 2 + metrics.getAscent() / 2 - 2);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            // Enable crisp anti-aliasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            if (isEmpty) {
                g2.setColor(theme.empty);
                g2.fillRect(0, 0, width, height);
            } else {
                if (tileImage != null) {
                    int sourceX = (tileValue - 1) % gridSize;
                    int sourceY = (tileValue - 1) / gridSize;
                    int imageX1 = sourceX * tileImage.getWidth() / gridSize;
                    int imageY1 = sourceY * tileImage.getHeight() / gridSize;
                    int imageX2 = (sourceX + 1) * tileImage.getWidth() / gridSize;
                    int imageY2 = (sourceY + 1) * tileImage.getHeight() / gridSize;
                    g2.drawImage(tileImage, 0, 0, width, height,
                        imageX1, imageY1, imageX2, imageY2, null);
                    if (isHovered) {
                        g2.setColor(new Color(255, 255, 255, 45));
                        g2.fillRect(0, 0, width, height);
                    }
                    paintClue(g2, width);
                    paintCellBorder(g2, width, height);
                    g2.dispose();
                    return;
                }

                // Choose tile color
                Color bg;
                if (isCorrect) {
                    bg = isHovered ? theme.correctHover : theme.correct;
                } else {
                    bg = isHovered ? theme.tileHover : theme.tile;
                }

                // Fill rounded tile background with subtle vertical gradient
                GradientPaint gp = new GradientPaint(
                    0, 0, bg.brighter(),
                    0, height, bg
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, width, height);

                // Draw tile number
                String text = getText();
                if (text != null && !text.isEmpty()) {
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getAscent();

                    int x = (width - textWidth) / 2;
                    int y = (height + textHeight) / 2 - 4;

                    // Subtle text shadow
                    g2.setColor(new Color(0, 0, 0, 70));
                    g2.drawString(text, x + 1, y + 2);

                    // Text fill
                    g2.setColor(TILE_TEXT_COLOR);
                    g2.drawString(text, x, y);
                }
            }

            paintClue(g2, width);
            paintCellBorder(g2, width, height);
            g2.dispose();
        }

        private void paintCellBorder(Graphics2D g2, int width, int height) {
            boolean highlightedImageTile = tileImage != null && isCorrect;
            g2.setColor(highlightedImageTile ? CORRECT_IMAGE_BORDER : theme.emptyBorder);
            g2.setStroke(new BasicStroke(highlightedImageTile ? 2f : 1f));
            g2.drawRect(0, 0, width - 1, height - 1);
        }
    }
}
