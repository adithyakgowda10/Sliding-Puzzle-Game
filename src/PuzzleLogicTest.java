/**
 * PuzzleLogicTest.java
 * 
 * Standalone verification runner to test puzzle logic,
 * tile movement, reset functionality, and mathematical solvability.
 * 
 * Can be compiled and executed directly from the terminal:
 * javac PuzzleLogicTest.java PuzzleLogic.java
 * java PuzzleLogicTest
 */
public class PuzzleLogicTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  SLIDING PUZZLE LOGIC & SOLVABILITY TEST SUITE   ");
        System.out.println("==================================================");

        int passed = 0;
        int total = 0;

        // Test 1: Initial Solved State
        total++;
        PuzzleLogic logic = new PuzzleLogic();
        if (logic.isSolved() && logic.getMoveCount() == 0) {
            System.out.println("✔ Test 1 Passed: Solved state correctly identified.");
            passed++;
        } else {
            System.err.println("✘ Test 1 Failed: Solved state mismatch.");
        }

        // Test 2: Inversion Counter and Solvability Math
        total++;
        int[] solvedFlat = {1, 2, 3, 4, 5, 6, 7, 8, 0};
        int[] unsolvableFlat = {1, 2, 3, 4, 5, 6, 8, 7, 0}; // Swap 7 and 8 creates 1 inversion (odd)

        int solvedInversions = PuzzleLogic.countInversions(solvedFlat);
        boolean solvedSolvable = PuzzleLogic.isSolvable(solvedFlat);

        int unsolvableInversions = PuzzleLogic.countInversions(unsolvableFlat);
        boolean unsolvableSolvable = PuzzleLogic.isSolvable(unsolvableFlat);

        if (solvedInversions == 0 && solvedSolvable && unsolvableInversions == 1 && !unsolvableSolvable) {
            System.out.println("✔ Test 2 Passed: Inversion parity and solvability theorem verified.");
            passed++;
        } else {
            System.err.println("✘ Test 2 Failed: Solvability calculation error.");
        }

        // Test 3: Adjacency and Move Validation
        total++;
        logic.setSolvedState(); // Empty space at (2, 2)
        boolean canMoveValid1 = logic.canMove(2, 1); // Left of empty (Tile 8)
        boolean canMoveValid2 = logic.canMove(1, 2); // Above empty (Tile 6)
        boolean canMoveInvalid = logic.canMove(0, 0); // Corner diagonal (Tile 1)

        if (canMoveValid1 && canMoveValid2 && !canMoveInvalid) {
            System.out.println("✔ Test 3 Passed: Tile adjacency and move permissions validated.");
            passed++;
        } else {
            System.err.println("✘ Test 3 Failed: Invalid move validation logic.");
        }

        // Test 4: Move Execution and State Update
        total++;
        boolean moveSuccess = logic.moveTile(2, 1);
        if (moveSuccess && logic.getMoveCount() == 1 && !logic.isSolved() && logic.getEmptyRow() == 2 && logic.getEmptyCol() == 1) {
            System.out.println("✔ Test 4 Passed: Tile movement and empty slot coordinate updates verified.");
            passed++;
        } else {
            System.err.println("✘ Test 4 Failed: Move execution failed.");
        }

        // Test 5: Reset Functionality
        total++;
        logic.newGame(PuzzleLogic.Difficulty.EASY);
        int[][] initialSnapshot = logic.getBoardCopy();
        // Make arbitrary moves
        logic.moveByDirection(PuzzleLogic.Direction.UP);
        logic.moveByDirection(PuzzleLogic.Direction.LEFT);
        // Reset
        logic.resetGame();
        int[][] resetSnapshot = logic.getBoardCopy();

        boolean resetMatches = true;
        for (int r = 0; r < PuzzleLogic.SIZE; r++) {
            for (int c = 0; c < PuzzleLogic.SIZE; c++) {
                if (initialSnapshot[r][c] != resetSnapshot[r][c]) {
                    resetMatches = false;
                    break;
                }
            }
        }

        if (resetMatches && logic.getMoveCount() == 0) {
            System.out.println("✔ Test 5 Passed: Reset restores exact starting board layout.");
            passed++;
        } else {
            System.err.println("✘ Test 5 Failed: Reset did not restore initial layout.");
        }

        // Test 6: Solvability Invariance across 100 Random Shuffles
        total++;
        boolean allSolvable = true;
        for (int i = 0; i < 100; i++) {
            PuzzleLogic.Difficulty diff = (i % 3 == 0) ? PuzzleLogic.Difficulty.EASY :
                                          (i % 3 == 1) ? PuzzleLogic.Difficulty.MEDIUM :
                                                         PuzzleLogic.Difficulty.HARD;
            logic.newGame(diff);
            int[] flat = logic.getFlatBoard();
            if (!PuzzleLogic.isSolvable(flat)) {
                allSolvable = false;
                break;
            }
        }

        if (allSolvable) {
            System.out.println("✔ Test 6 Passed: 100% of 100 random shuffles guaranteed solvable.");
            passed++;
        } else {
            System.err.println("✘ Test 6 Failed: Unsolvable state generated!");
        }

        // Test 7: Seeded daily boards are repeatable
        total++;
        logic.shuffle(35, 20261005L);
        int[] firstDailyBoard = logic.getFlatBoard();
        logic.shuffle(35, 20261005L);
        int[] secondDailyBoard = logic.getFlatBoard();
        boolean sameDailyBoard = java.util.Arrays.equals(firstDailyBoard, secondDailyBoard);
        if (sameDailyBoard) {
            System.out.println("✔ Test 7 Passed: Identical seeds generate identical daily boards.");
            passed++;
        } else {
            System.err.println("✘ Test 7 Failed: Seeded shuffle was not repeatable.");
        }

        // Test 8: 4x4 movement and solvability
        total++;
        PuzzleLogic logic4 = new PuzzleLogic(4);
        int[] solved4 = logic4.getFlatBoard();
        int[] unsolvable4 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 14, 0};
        boolean allFourByFourShufflesSolvable = true;
        for (int i = 0; i < 100; i++) {
            logic4.shuffle(40, i);
            if (!PuzzleLogic.isSolvable(logic4.getFlatBoard(), 4)) {
                allFourByFourShufflesSolvable = false;
                break;
            }
        }
        logic4.setSolvedState();
        boolean fourByFourMoves = logic4.moveTile(3, 2)
            && logic4.getEmptyRow() == 3 && logic4.getEmptyCol() == 2;
        if (solved4.length == 16 && PuzzleLogic.isSolvable(solved4, 4)
                && !PuzzleLogic.isSolvable(unsolvable4, 4) && allFourByFourShufflesSolvable && fourByFourMoves) {
            System.out.println("✔ Test 8 Passed: 4x4 parity, shuffles, and tile movement verified.");
            passed++;
        } else {
            System.err.println("✘ Test 8 Failed: 4x4 puzzle behavior mismatch.");
        }

        // Test 9: 4x4 board view uses square, edge-aligned cells and paints image mode
        total++;
        GameBoard boardView = new GameBoard(logic4);
        boardView.setSize(380, 380);
        boardView.doLayout();
        java.awt.Rectangle firstCell = boardView.getComponent(0).getBounds();
        java.awt.Rectangle rightCell = boardView.getComponent(1).getBounds();
        java.awt.Rectangle lowerCell = boardView.getComponent(4).getBounds();
        boolean alignedCells = boardView.getComponentCount() == 16
            && firstCell.width == firstCell.height
            && firstCell.x + firstCell.width == rightCell.x
            && firstCell.y + firstCell.height == lowerCell.y;
        java.awt.image.BufferedImage sampleImage = new java.awt.image.BufferedImage(320, 320,
            java.awt.image.BufferedImage.TYPE_INT_RGB);
        boardView.setPuzzleImage(sampleImage);
        java.awt.image.BufferedImage renderedBoard = new java.awt.image.BufferedImage(380, 380,
            java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = renderedBoard.createGraphics();
        boardView.paint(graphics);
        graphics.dispose();
        int greenBorder = new java.awt.Color(144, 238, 144).getRGB();
        boolean correctImageTileHasGreenBorder = false;
        for (int y = firstCell.y; y < firstCell.y + firstCell.height; y++) {
            for (int x = firstCell.x; x < firstCell.x + firstCell.width; x++) {
                if (renderedBoard.getRGB(x, y) == greenBorder) {
                    correctImageTileHasGreenBorder = true;
                    break;
                }
            }
            if (correctImageTileHasGreenBorder) {
                break;
            }
        }
        if (alignedCells && correctImageTileHasGreenBorder) {
            System.out.println("✔ Test 9 Passed: 4x4 cells align and correctly placed image tiles have green borders.");
            passed++;
        } else {
            System.err.println("✘ Test 9 Failed: 4x4 board layout or correct-image border mismatch.");
        }

        // Test 10: AI solver returns valid paths for every supported board size
        total++;
        boolean allSolverSizesWork = true;
        for (int size = 3; size <= 6; size++) {
            int[] oneMoveBoard = new PuzzleLogic(size).getFlatBoard();
            int lastTileIndex = oneMoveBoard.length - 2;
            oneMoveBoard[lastTileIndex] = 0;
            oneMoveBoard[lastTileIndex + 1] = lastTileIndex + 1;
            java.util.List<Integer> solution = PuzzleLogic.PuzzleSearch.solve(oneMoveBoard, size);
            allSolverSizesWork &= isSolvedByPath(oneMoveBoard, solution, size);
        }
        if (allSolverSizesWork) {
            System.out.println("✔ Test 10 Passed: AI solver finds valid paths for 3x3 through 6x6 boards.");
            passed++;
        } else {
            System.err.println("✘ Test 10 Failed: AI solution path was invalid.");
        }

        // Test 11: AI can solve shuffled boards, not only one-move examples
        total++;
        boolean allShuffledBoardsWork = true;
        for (int size = 3; size <= 6; size++) {
            PuzzleLogic scrambled = new PuzzleLogic(size);
            scrambled.shuffle(size == 3 ? 18 : 8, size * 104729L);
            int[] scrambledBoard = scrambled.getFlatBoard();
            java.util.List<Integer> shuffledSolution = PuzzleLogic.PuzzleSearch.solve(scrambledBoard, size);
            allShuffledBoardsWork &= isSolvedByPath(scrambledBoard, shuffledSolution, size);
        }
        if (allShuffledBoardsWork) {
            System.out.println("✔ Test 11 Passed: AI solves shuffled 3x3 through 6x6 boards.");
            passed++;
        } else {
            System.err.println("✘ Test 11 Failed: AI could not solve a shuffled board.");
        }

        // Test 12: 5x5 and 6x6 boards support solvability, movement, and rendering
        total++;
        boolean largerBoardsWork = true;
        for (int size = 5; size <= 6; size++) {
            PuzzleLogic larger = new PuzzleLogic(size);
            int[] solved = larger.getFlatBoard();
            int[] impossible = java.util.Arrays.copyOf(solved, solved.length);
            int lastTileIndex = impossible.length - 2;
            int temporary = impossible[lastTileIndex];
            impossible[lastTileIndex] = impossible[lastTileIndex - 1];
            impossible[lastTileIndex - 1] = temporary;
            larger.shuffle(80, size);
            boolean solvability = PuzzleLogic.isSolvable(solved, size)
                && !PuzzleLogic.isSolvable(impossible, size)
                && PuzzleLogic.isSolvable(larger.getFlatBoard(), size);
            larger.setSolvedState();
            boolean movement = larger.moveTile(size - 1, size - 2)
                && larger.getEmptyRow() == size - 1 && larger.getEmptyCol() == size - 2;
            GameBoard largerView = new GameBoard(larger);
            largerView.setSize(360, 360);
            largerView.doLayout();
            java.awt.Rectangle first = largerView.getComponent(0).getBounds();
            java.awt.Rectangle right = largerView.getComponent(1).getBounds();
            boolean rendering = largerView.getComponentCount() == size * size
                && first.width == first.height && first.x + first.width == right.x;
            largerBoardsWork &= solvability && movement && rendering;
        }
        if (largerBoardsWork) {
            System.out.println("✔ Test 12 Passed: 5x5 and 6x6 solvability, movement, and rendering verified.");
            passed++;
        } else {
            System.err.println("✘ Test 12 Failed: Larger board behavior mismatch.");
        }

        // Test 13: Invalid board encodings are rejected instead of failing during parity checks
        total++;
        boolean invalidBoardsRejected = false;
        try {
            PuzzleLogic.isSolvable(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 8}, 3);
        } catch (IllegalArgumentException expected) {
            invalidBoardsRejected = true;
        }
        if (invalidBoardsRejected) {
            System.out.println("✔ Test 13 Passed: Invalid board tile values are rejected.");
            passed++;
        } else {
            System.err.println("✘ Test 13 Failed: Invalid board was accepted.");
        }

        // Test 14: Recorded solution paths solve generated boards after player moves and reset
        total++;
        boolean knownPathsWork = true;
        for (int size = 3; size <= 6; size++) {
            PuzzleLogic pathLogic = new PuzzleLogic(size);
            pathLogic.shuffle(size * 7, size * 8191L);
            PuzzleLogic.Direction[] directions = PuzzleLogic.Direction.values();
            for (int move = 0; move < 12; move++) {
                boolean moved = false;
                for (int offset = 0; offset < directions.length && !moved; offset++) {
                    moved = pathLogic.moveByDirection(directions[(move + offset) % directions.length]);
                }
                if (!moved) {
                    knownPathsWork = false;
                    break;
                }
            }
            int[] current = pathLogic.getFlatBoard();
            knownPathsWork &= isSolvedByPath(current, pathLogic.getKnownSolutionPath(), size);
            pathLogic.resetGame();
            knownPathsWork &= isSolvedByPath(
                pathLogic.getFlatBoard(), pathLogic.getKnownSolutionPath(), size);
        }
        if (knownPathsWork) {
            System.out.println("✔ Test 14 Passed: Recorded AI paths solve shuffled and player-moved boards at every size.");
            passed++;
        } else {
            System.err.println("✘ Test 14 Failed: Recorded AI solution path was invalid.");
        }

        System.out.println("==================================================");
        System.out.printf("Test Summary: %d / %d Tests Passed.\n", passed, total);
        System.out.println("==================================================");

        if (passed == total) {
            System.out.println("SUCCESS: All core game logic verified without errors.");
        } else {
            System.exit(1);
        }
    }

    private static boolean isSolvedByPath(int[] initial, java.util.List<Integer> path, int size) {
        if (path == null) {
            return false;
        }
        int[] board = java.util.Arrays.copyOf(initial, initial.length);
        for (int tile : path) {
            int empty = -1;
            int tileIndex = -1;
            for (int index = 0; index < board.length; index++) {
                if (board[index] == PuzzleLogic.EMPTY_TILE) empty = index;
                if (board[index] == tile) tileIndex = index;
            }
            if (empty < 0 || tileIndex < 0
                    || Math.abs(empty / size - tileIndex / size) + Math.abs(empty % size - tileIndex % size) != 1) {
                return false;
            }
            board[empty] = tile;
            board[tileIndex] = PuzzleLogic.EMPTY_TILE;
        }
        for (int index = 0; index < board.length - 1; index++) {
            if (board[index] != index + 1) return false;
        }
        return board[board.length - 1] == PuzzleLogic.EMPTY_TILE;
    }
}
