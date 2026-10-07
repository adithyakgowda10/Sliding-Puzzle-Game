import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages the state, rules, and solvability mathematics for sliding puzzles.
 */
public class PuzzleLogic {

    // Grid dimension (3x3 for the standard 8-puzzle)
    public static final int SIZE = 3;
    public static final int EMPTY_TILE = 0;

    /**
     * Difficulty levels determine the number of random moves
     * applied from the solved state during shuffling.
     */
    public enum Difficulty {
        EASY("Easy (15 moves)", 15),
        MEDIUM("Medium (45 moves)", 45),
        HARD("Hard (120 moves)", 120);

        private final String displayName;
        private final int shuffleMoves;

        Difficulty(String displayName, int shuffleMoves) {
            this.displayName = displayName;
            this.shuffleMoves = shuffleMoves;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getShuffleMoves() {
            return shuffleMoves;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** Finds bounded IDA* solution paths for sliding puzzles from 3x3 through 6x6. */
    static final class PuzzleSearch {

            private static final long MAX_NODES = 8_000_000L;
            private static final long MAX_SEARCH_MILLIS = 20_000L;
            private static final int FOUND = -1;

            private final int size;
            private final int[] board;
            private final int[] path;
            private long nodes;
            private long deadline;
            private int solutionLength;

            private PuzzleSearch(int[] initialBoard, int size) {
                this.size = size;
                board = Arrays.copyOf(initialBoard, initialBoard.length);
                path = new int[initialBoard.length * initialBoard.length];
            }

            static List<Integer> solve(int[] initialBoard, int size) {
                if (initialBoard == null || size < 3 || size > 6 || initialBoard.length != size * size
                        || !hasValidTiles(initialBoard) || !PuzzleLogic.isSolvable(initialBoard, size)) {
                    return null;
                }
                return new PuzzleSearch(initialBoard, size).search();
            }

            private static boolean hasValidTiles(int[] initialBoard) {
                boolean[] seen = new boolean[initialBoard.length];
                for (int value : initialBoard) {
                    if (value < 0 || value >= initialBoard.length || seen[value]) {
                        return false;
                    }
                    seen[value] = true;
                }
                return true;
            }

            private List<Integer> search() {
                int emptyIndex = 0;
                while (board[emptyIndex] != PuzzleLogic.EMPTY_TILE) {
                    emptyIndex++;
                }

                deadline = System.currentTimeMillis() + MAX_SEARCH_MILLIS;
                int bound = heuristic();
                while (true) {
                    int result = search(emptyIndex, -1, 0, bound);
                    if (result == FOUND) {
                        List<Integer> moves = new ArrayList<>(solutionLength);
                        for (int i = 0; i < solutionLength; i++) {
                            moves.add(path[i]);
                        }
                        return moves;
                    }
                    if (result == Integer.MAX_VALUE) {
                        return null;
                    }
                    bound = result;
                }
            }

            private int search(int emptyIndex, int previousEmptyIndex, int depth, int bound) {
                if ((++nodes & 4095) == 0
                        && (Thread.currentThread().isInterrupted() || nodes >= MAX_NODES
                            || System.currentTimeMillis() >= deadline)) {
                    return Integer.MAX_VALUE;
                }

                int estimate = depth + heuristic();
                if (estimate > bound) {
                    return estimate;
                }
                if (estimate == depth) {
                    solutionLength = depth;
                    return FOUND;
                }

                int row = emptyIndex / size;
                int col = emptyIndex % size;
                int minimumBound = Integer.MAX_VALUE;
                int[] neighbors = {
                    row > 0 ? emptyIndex - size : -1,
                    row + 1 < size ? emptyIndex + size : -1,
                    col > 0 ? emptyIndex - 1 : -1,
                    col + 1 < size ? emptyIndex + 1 : -1
                };

                for (int neighbor : neighbors) {
                    if (neighbor < 0 || neighbor == previousEmptyIndex) {
                        continue;
                    }
                    int movedTile = board[neighbor];
                    board[emptyIndex] = movedTile;
                    board[neighbor] = PuzzleLogic.EMPTY_TILE;
                    path[depth] = movedTile;

                    int result = search(neighbor, emptyIndex, depth + 1, bound);

                    board[neighbor] = movedTile;
                    board[emptyIndex] = PuzzleLogic.EMPTY_TILE;
                    if (result == FOUND || result == Integer.MAX_VALUE) {
                        return result;
                    }
                    minimumBound = Math.min(minimumBound, result);
                }
                return minimumBound;
            }

            private int heuristic() {
                int distance = 0;
                for (int index = 0; index < board.length; index++) {
                    int value = board[index];
                    if (value == PuzzleLogic.EMPTY_TILE) {
                        continue;
                    }
                    int goalIndex = value - 1;
                    distance += Math.abs(index / size - goalIndex / size)
                        + Math.abs(index % size - goalIndex % size);
                }
                return distance + linearConflict();
            }

            private int linearConflict() {
                int conflicts = 0;
                for (int row = 0; row < size; row++) {
                    boolean[] used = new boolean[size];
                    for (int firstCol = 0; firstCol < size; firstCol++) {
                        int first = board[row * size + firstCol];
                        if (first == 0 || (first - 1) / size != row || used[firstCol]) {
                            continue;
                        }
                        for (int secondCol = firstCol + 1; secondCol < size; secondCol++) {
                            int second = board[row * size + secondCol];
                            if (!used[secondCol] && second != 0 && (second - 1) / size == row
                                    && (first - 1) % size > (second - 1) % size) {
                                conflicts += 2;
                                used[firstCol] = true;
                                used[secondCol] = true;
                                break;
                            }
                        }
                    }
                }
                for (int col = 0; col < size; col++) {
                    boolean[] used = new boolean[size];
                    for (int firstRow = 0; firstRow < size; firstRow++) {
                        int first = board[firstRow * size + col];
                        if (first == 0 || (first - 1) % size != col || used[firstRow]) {
                            continue;
                        }
                        for (int secondRow = firstRow + 1; secondRow < size; secondRow++) {
                            int second = board[secondRow * size + col];
                            if (!used[secondRow] && second != 0 && (second - 1) % size == col
                                    && (first - 1) / size > (second - 1) / size) {
                                conflicts += 2;
                                used[firstRow] = true;
                                used[secondRow] = true;
                                break;
                            }
                        }
                    }
                }
                return conflicts;
            }
    }

    /**
     * Direction enum for keyboard arrow-key navigation.
     */
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private int size;
    private int[][] board;
    private int[][] initialBoard;

    // Current coordinates of the empty space (0)
    private int emptyRow;
    private int emptyCol;

    // Snapshot of initial empty space coordinates
    private int initialEmptyRow;
    private int initialEmptyCol;

    // Number of valid moves made by the player in current round
    private int moveCount;

    // Current difficulty setting
    private Difficulty currentDifficulty = Difficulty.MEDIUM;
    private final List<Integer> shuffleMovesFromSolved = new ArrayList<>();
    private final List<Integer> movesSinceShuffle = new ArrayList<>();

    /**
     * Constructor initializes a solved board.
     */
    public PuzzleLogic() {
        this(SIZE);
    }

    public PuzzleLogic(int size) {
        validateBoardSize(size);
        this.size = size;
        this.board = new int[size][size];
        this.initialBoard = new int[size][size];
        setSolvedState();
        saveInitialState();
        this.moveCount = 0;
    }

    public void setBoardSize(int size) {
        validateBoardSize(size);
        if (this.size == size) {
            return;
        }
        this.size = size;
        this.board = new int[size][size];
        this.initialBoard = new int[size][size];
        setSolvedState();
        saveInitialState();
        moveCount = 0;
    }

    private static void validateBoardSize(int size) {
        if (size < 3 || size > 6) {
            throw new IllegalArgumentException("Board size must be between 3 and 6.");
        }
    }

    public int getSize() {
        return size;
    }

    /**
     * Sets the board to the standard target solved state:
     * 1  2  3
     * 4  5  6
     * 7  8  [0]
     */
    public void setSolvedState() {
        shuffleMovesFromSolved.clear();
        movesSinceShuffle.clear();
        moveCount = 0;
        int value = 1;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (r == size - 1 && c == size - 1) {
                    board[r][c] = EMPTY_TILE;
                    emptyRow = r;
                    emptyCol = c;
                } else {
                    board[r][c] = value++;
                }
            }
        }
    }

    /**
     * Saves a snapshot of the current board layout to initialBoard.
     * This enables the player to restart the exact same puzzle using the "Reset" button.
     */
    private void saveInitialState() {
        for (int r = 0; r < size; r++) {
            System.arraycopy(board[r], 0, initialBoard[r], 0, size);
        }
        initialEmptyRow = emptyRow;
        initialEmptyCol = emptyCol;
    }

    /**
     * Starts a new game with the specified difficulty.
     * Generates a guaranteed-solvable shuffled state.
     *
     * @param difficulty the selected difficulty level
     */
    public void newGame(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
        shuffle(difficulty.getShuffleMoves());
        this.moveCount = 0;
    }

    /**
     * Resets the board back to the initial state of the current game.
     * Move counter is reset to zero.
     */
    public void resetGame() {
        for (int r = 0; r < size; r++) {
            System.arraycopy(initialBoard[r], 0, board[r], 0, size);
        }
        emptyRow = initialEmptyRow;
        emptyCol = initialEmptyCol;
        moveCount = 0;
        movesSinceShuffle.clear();
    }

    /**
     * Shuffles the puzzle by performing a random walk of valid moves
     * starting from the solved state.
     * 
     * WHY THIS GUARANTEES SOLVABILITY:
     * Any board configuration reached by a sequence of valid sliding moves
     * from the solved state is mathematically guaranteed to be solvable, because
     * sliding moves preserve the parity of tile permutations.
     * 
     * @param moveCountTarget the number of random moves to execute
     */
    public void shuffle(int moveCountTarget) {
        shuffle(moveCountTarget, new java.util.Random());
    }

    public void shuffle(int moveCountTarget, long seed) {
        shuffle(moveCountTarget, new java.util.Random(seed));
    }

    private void shuffle(int moveCountTarget, java.util.Random random) {
        // Start from the clean solved state
        setSolvedState();

        int lastMoveRow = -1;
        int lastMoveCol = -1;
        int movesMade = 0;

        // Direction vectors: UP, DOWN, LEFT, RIGHT
        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = {0, 0, -1, 1};

        while (movesMade < moveCountTarget) {
            // Find all valid adjacent neighbors of the empty space
            java.util.List<int[]> validNeighbors = new java.util.ArrayList<>();

            for (int i = 0; i < 4; i++) {
                int nr = emptyRow + dRow[i];
                int nc = emptyCol + dCol[i];

                if (isValidBoardCoordinate(nr, nc)) {
                    // Avoid immediately undoing the move we just made to prevent back-and-forth oscillation
                    if (nr != lastMoveRow || nc != lastMoveCol) {
                        validNeighbors.add(new int[]{nr, nc});
                    }
                }
            }

            if (validNeighbors.isEmpty()) {
                // Fallback: accept any valid neighbor if oscillation filter eliminated all
                for (int i = 0; i < 4; i++) {
                    int nr = emptyRow + dRow[i];
                    int nc = emptyCol + dCol[i];
                    if (isValidBoardCoordinate(nr, nc)) {
                        validNeighbors.add(new int[]{nr, nc});
                    }
                }
            }

            // Pick a random neighbor and swap with empty tile
            int[] chosen = validNeighbors.get(random.nextInt(validNeighbors.size()));
            lastMoveRow = emptyRow;
            lastMoveCol = emptyCol;

            // Perform swap without incrementing player move count
            appendReducedMove(shuffleMovesFromSolved, board[chosen[0]][chosen[1]]);
            board[emptyRow][emptyCol] = board[chosen[0]][chosen[1]];
            board[chosen[0]][chosen[1]] = EMPTY_TILE;
            emptyRow = chosen[0];
            emptyCol = chosen[1];

            movesMade++;
        }

        // Ensure the shuffled puzzle is not accidentally already solved
        if (isSolved()) {
            // If somehow solved, perform two more moves
            for (int i = 0; i < 4; i++) {
                int nr = emptyRow + dRow[i];
                int nc = emptyCol + dCol[i];
                if (isValidBoardCoordinate(nr, nc)) {
                    appendReducedMove(shuffleMovesFromSolved, board[nr][nc]);
                    board[emptyRow][emptyCol] = board[nr][nc];
                    board[nr][nc] = EMPTY_TILE;
                    emptyRow = nr;
                    emptyCol = nc;
                    break;
                }
            }
        }

        // Store this shuffled board as the starting point for "Reset"
        saveInitialState();
        this.moveCount = 0;
    }

    /**
     * Checks if a tile at (row, col) can be moved into the empty space.
     * A tile can move if and only if it is orthogonally adjacent to the empty slot.
     *
     * @param row target tile row (0-2)
     * @param col target tile col (0-2)
     * @return true if the tile is adjacent to empty space
     */
    public boolean canMove(int row, int col) {
        if (!isValidBoardCoordinate(row, col)) {
            return false;
        }
        if (row == emptyRow && col == emptyCol) {
            return false;
        }
        // Orthogonally adjacent means Manhattan distance equals 1
        int rowDiff = Math.abs(row - emptyRow);
        int colDiff = Math.abs(col - emptyCol);
        return (rowDiff + colDiff == 1);
    }

    /**
     * Attempts to move the tile at (row, col).
     * If valid, swaps the tile with the empty space and increments move count.
     *
     * @param row tile row
     * @param col tile column
     * @return true if move was valid and executed, false otherwise
     */
    public boolean moveTile(int row, int col) {
        if (canMove(row, col)) {
            // Swap tile with empty space
            appendReducedMove(movesSinceShuffle, board[row][col]);
            board[emptyRow][emptyCol] = board[row][col];
            board[row][col] = EMPTY_TILE;
            emptyRow = row;
            emptyCol = col;
            moveCount++;
            return true;
        }
        return false;
    }

    private static void appendReducedMove(List<Integer> moves, int tile) {
        int lastIndex = moves.size() - 1;
        if (lastIndex >= 0 && moves.get(lastIndex) == tile) {
            moves.remove(lastIndex);
        } else {
            moves.add(tile);
        }
    }

    /**
     * Returns legal tile moves that solve the current board, based on the
     * recorded shuffle and subsequent player moves.
     */
    public List<Integer> getKnownSolutionPath() {
        List<Integer> solution = new ArrayList<>(movesSinceShuffle.size() + shuffleMovesFromSolved.size());
        for (int i = movesSinceShuffle.size() - 1; i >= 0; i--) {
            solution.add(movesSinceShuffle.get(i));
        }
        for (int i = shuffleMovesFromSolved.size() - 1; i >= 0; i--) {
            solution.add(shuffleMovesFromSolved.get(i));
        }
        return solution;
    }

    /**
     * Moves a tile relative to the empty space using directional keys.
     * E.g., pressing UP moves the tile below the empty space UP into the empty space.
     *
     * @param direction the direction of the movement
     * @return true if a tile moved, false if blocked by wall
     */
    public boolean moveByDirection(Direction direction) {
        int targetRow = emptyRow;
        int targetCol = emptyCol;

        switch (direction) {
            case UP:
                // Move the tile situated BELOW the empty slot UP into it
                targetRow = emptyRow + 1;
                break;
            case DOWN:
                // Move the tile situated ABOVE the empty slot DOWN into it
                targetRow = emptyRow - 1;
                break;
            case LEFT:
                // Move the tile situated to the RIGHT of the empty slot LEFT into it
                targetCol = emptyCol + 1;
                break;
            case RIGHT:
                // Move the tile situated to the LEFT of the empty slot RIGHT into it
                targetCol = emptyCol - 1;
                break;
        }

        if (isValidBoardCoordinate(targetRow, targetCol)) {
            return moveTile(targetRow, targetCol);
        }
        return false;
    }

    /**
     * Checks if the puzzle is currently in the winning/solved state.
     * Solved state:
     * 1 2 3
     * 4 5 6
     * 7 8 0
     *
     * @return true if solved, false otherwise
     */
    public boolean isSolved() {
        int expected = 1;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (r == size - 1 && c == size - 1) {
                    return board[r][c] == EMPTY_TILE;
                }
                if (board[r][c] != expected++) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if a given row and column index is within the 3x3 board boundary.
     */
    public static boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    private boolean isValidBoardCoordinate(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    /**
     * Flattens the 2D board into a 1D array of 9 elements.
     * Useful for solvability calculation and serialization.
     */
    public int[] getFlatBoard() {
        int[] flat = new int[size * size];
        int idx = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                flat[idx++] = board[r][c];
            }
        }
        return flat;
    }

    /**
     * MATHEMATICAL SOLVABILITY EXPLANATION:
     * 
     * In an 8-puzzle (3x3 grid, odd width N=3):
     * An "inversion" is a pair of tiles (A, B) such that A appears before B
     * in row-major order, but A > B (ignoring the empty blank tile 0).
     * 
     * Theorem:
     * A 3x3 sliding puzzle state is solvable IF AND ONLY IF the number of
     * inversions in its 1D representation is EVEN.
     * 
     * Reason:
     * - A horizontal slide (left/right) does not alter the relative order of tiles
     *   in 1D row-major array, so change in inversions = 0.
     * - A vertical slide (up/down) moves a tile ahead or behind exactly 2 other tiles.
     *   When swapping past 2 tiles, the inversion count changes by -2, 0, or +2.
     *   All of these changes maintain the parity (inversion count mod 2).
     * - The solved goal state has 0 inversions (0 is even).
     * - Therefore, every reachable state must also have an EVEN number of inversions!
     *
     * @param flatArray 1D array representation of 3x3 board
     * @return the number of inversions
     */
    public static int countInversions(int[] flatArray) {
        int inversions = 0;
        for (int i = 0; i < flatArray.length - 1; i++) {
            if (flatArray[i] == EMPTY_TILE) continue;
            for (int j = i + 1; j < flatArray.length; j++) {
                if (flatArray[j] == EMPTY_TILE) continue;
                if (flatArray[i] > flatArray[j]) {
                    inversions++;
                }
            }
        }
        return inversions;
    }

    /**
     * Determines whether the given 1D board state is mathematically solvable.
     *
     * @param flatArray 1D array representing 3x3 board
     * @return true if solvable (even inversions), false otherwise
     */
    public static boolean isSolvable(int[] flatArray) {
        return isSolvable(flatArray, SIZE);
    }

    public static boolean isSolvable(int[] flatArray, int boardSize) {
        if (flatArray == null || boardSize < 3 || boardSize > 6
                || flatArray.length != boardSize * boardSize) {
            throw new IllegalArgumentException("Board data must match a grid between 3x3 and 6x6.");
        }
        boolean[] seen = new boolean[flatArray.length];
        for (int tile : flatArray) {
            if (tile < 0 || tile >= flatArray.length || seen[tile]) {
                throw new IllegalArgumentException("Board data must contain each tile value exactly once.");
            }
            seen[tile] = true;
        }
        int inversions = countInversions(flatArray);
        if (boardSize % 2 == 1) {
            return inversions % 2 == 0;
        }
        int emptyIndex = 0;
        while (flatArray[emptyIndex] != EMPTY_TILE) {
            emptyIndex++;
        }
        int emptyRowFromBottom = boardSize - emptyIndex / boardSize;
        return (inversions + emptyRowFromBottom) % 2 == 1;
    }

    /**
     * Returns true if the tile at (row, col) is currently in its correct target position.
     * This is used by the GUI to render subtle visual satisfaction cues.
     */
    public boolean isTileInCorrectPosition(int row, int col) {
        if (row == size - 1 && col == size - 1) {
            return board[row][col] == EMPTY_TILE;
        }
        int targetVal = row * size + col + 1;
        return board[row][col] == targetVal;
    }

    // --- Getters and Setters ---

    public int getTile(int row, int col) {
        return board[row][col];
    }

    public int getEmptyRow() {
        return emptyRow;
    }

    public int getEmptyCol() {
        return emptyCol;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public Difficulty getCurrentDifficulty() {
        return currentDifficulty;
    }

    public void setCurrentDifficulty(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
    }

    public int[][] getBoardCopy() {
        int[][] copy = new int[size][size];
        for (int r = 0; r < size; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, size);
        }
        return copy;
    }
}
