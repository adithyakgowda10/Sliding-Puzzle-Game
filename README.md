# Sliding Puzzle Game (8-Puzzle to 35-Puzzle)

A polished, modern desktop application of the classic **8-Puzzle** sliding tile game built entirely with standard **Java Swing**.

Designed with beginner-friendly, clean, modular code, this project combines a polished Swing interface with image puzzles, daily challenges, and guaranteed mathematical solvability.

---

## 🌟 Features

- **Selectable 3×3 to 6×6 Grid:** Play boards from the 8-puzzle through the 35-puzzle with size-aware solvability rules.
- **Closed Square Layout:** Tiles align edge-to-edge with shared borders, like the classic boxed puzzle.
- **Modern UI Design:**
  - Soft blue-gray canvas, crisp white stat panels, and high-contrast navy typography.
  - Edge-to-edge square tiles with clear borders, smooth hover states, and a fixed Ocean palette.
  - Subtle indicator for tiles already placed in their correct solved positions.
  - Visually distinct, recessed empty slot.
- **Controls & Input:**
  - **Mouse Clicks:** Click any tile orthogonally adjacent to the empty slot to slide it.
  - **Keyboard Navigation:** Full support for **Arrow Keys** (`↑`, `↓`, `←`, `→`) and **W / A / S / D**.
- **Real-Time Gameplay Tracking:**
  - **Move Counter:** Updates immediately after each valid move.
  - **Timer:** Formats elapsed time as `MM:SS`, starts upon your first move, stops upon victory, and resets on new games.
  - **Session Best Score:** Tracks the fewest moves and fastest time achieved in the current play session.
- **Pause / Resume:** Freeze the board and stop the clock, then continue from the same position.
- **AI Solver:** Animates a guaranteed legal solution for 3×3 through 6×6 number or image puzzles using the recorded shuffle and move history.
- **Consistent Puzzle Difficulty:** Number and image puzzles use a fixed medium shuffle depth and move goal.
- **Move Challenges:** Earn gold at the move goal, silver within ten extra moves, or bronze for any clear.
- **Clue Tokens:** Spend one of three per-round tokens to briefly mark a tile's correct destination.
- **Image Gallery:** Load and save up to twelve of your own PNG, JPG, GIF, or BMP pictures, then replay them as image puzzles.
- **Daily Puzzle:** A UTC-date-seeded board gives everyone the same solvable layout. Daily completion and personal best moves/time are saved.
- **Board Palette:** Uses the Ocean palette throughout gameplay.
- **Grid-Aware Images:** Split an image into nine, sixteen, twenty-five, or thirty-six matching movable tiles.
- **Guaranteed Solvability:**
  - Employs a random walk from the solved state combined with mathematical **Inversion Counting** parity checks. Never generates an impossible board!
- **Game Controls:**
  - **NEW GAME:** Generates a fresh solvable puzzle.
  - **SHUFFLE:** Re-scrambles current tiles.
  - **RESET:** Restores the board back to the exact initial scrambled state so you can retry the layout.
  - **Image...:** Loads a picture and starts an image-based sliding puzzle. Use **Preview** to view the complete picture.
  - **Gallery:** Reopen a previously selected picture.
  - **Menu:** Access sound, image selection, gallery, preview, daily challenge, and rules.
  - **Board Size:** Choose 3 x 3 through 6 x 6 on the setup screen; image slicing, shuffle depth, move goal, and daily record follow the selected size.
  - **Clue:** Reveal a tile's correct destination briefly, up to three times per round.
  - **Pause / Resume:** Freeze or continue the board and timer.
  - **AI Solve:** Solve the current number or image puzzle by reversing its recorded legal moves, then animate the solution.
- **Victory Celebration:**
  - Animated image reveal, medal award, total moves, completion time, and session record badges.
  - Offers a quick "Play Again" button.
- **Procedural Sound Effects:**
  - Zero external `.wav` files required! Synthesizes soft mechanical slide clicks, invalid move blips, and an ascending victory fanfare using the standard `javax.sound.sampled` API.
  - Includes a mute/unmute toggle (`🔊 Sound` / `🔇 Muted`).

---

## 🛠 Technologies Used

- **Language:** Java (JDK 8 or higher; tested on JDK 11, 17, 21, and 25)
- **GUI Framework:** Java Swing (`javax.swing.*`, `java.awt.*`)
- **Sound API:** Standard Java Sound (`javax.sound.sampled.*`)
- **External Dependencies:** **None** (Zero third-party libraries; runs out of the box with standard Java runtime).

---

## 📁 Project Structure

```
SlidingPuzzleGame/
│
├── .vscode/
│   └── launch.json            # VS Code Run & Debug configurations (F5)
│
├── src/
│   ├── SlidingPuzzle.java      # Main application window, UI layout, event routing
│   ├── GameBoard.java          # Resizable 3x3-6x6 grid, tile rendering, key bindings
│   ├── PuzzleLogic.java        # Game rules, solvability math, and recorded solution paths
│   ├── GameServices.java       # Timer and procedural sound services
│   └── PuzzleLogicTest.java    # Standalone headless test suite
│
├── screenshots/
│   └── README.md               # Visual assets and preview documentation
│
└── README.md                   # Full documentation and guide
```

---

## 📐 Mathematical Solvability & Shuffling

A common pitfall in naive 8-puzzle implementations is randomly placing tiles 1–8 into the grid. In mathematics, **half of all random permutations of the 8-puzzle are physically impossible to solve**.

### The Inversion Theorem
An **inversion** occurs when a larger-numbered tile appears before a smaller-numbered tile in row-major order (excluding the blank space). For any odd-width grid, a board is solvable when the inversion count is even. For an even-width grid, inversions plus the blank row counted from the bottom must be odd.

- **Horizontal slides (Left / Right):** Do not alter the relative order of tiles in 1D row-major array $\implies \Delta \text{inversions} = 0$.
- **Vertical slides (Up / Down):** A tile swaps positions across 2 other tiles in the 1D array $\implies \Delta \text{inversions} \in \{-2, 0, +2\}$.

In all valid moves, the parity ($\text{inversions} \pmod 2$) **never changes**. Since the target solved state has $0$ inversions ($0 \pmod 2 = 0$), **a puzzle state is solvable if and only if its number of inversions is EVEN**.

Boards are shuffled using legal moves, preserving solvability at every supported size.

### How We Guarantee Solvability:
1. **Random Walk Shuffling:** The puzzle begins in the solved state and executes $N$ valid sliding moves (avoiding immediate reversals). Because each transition is a legitimate physical move, the board is mathematically guaranteed to remain solvable.
2. **Inversion Parity Checker:** `PuzzleLogic.isSolvable(flatArray)` verifies that $\text{countInversions} \pmod 2 == 0$ as an explicit invariant check.

---

## 🚀 How to Compile & Run

### Option 1: In VS Code (Recommended)
1. Open the `SlidingPuzzleGame` folder in **Visual Studio Code** (`File > Open Folder...`).
2. If prompted, install the official **Extension Pack for Java** by Microsoft.
3. Open `src/SlidingPuzzle.java`.
4. Click the **Run** button (play icon at the top right) or press **F5**.

---

### Option 2: Using Terminal / Command Prompt

#### Step 1: Open Terminal in the `SlidingPuzzleGame` folder
```bash
cd SlidingPuzzleGame
```

#### Step 2: Compile the Java Source Files
Compile all Java sources from the project root:

```bash
javac src/*.java
```

*(Or, from inside the `src` directory:)*
```bash
javac *.java
```

#### Step 3: Run the Application
From the project root:
```bash
java -cp src SlidingPuzzle
```

---

## 🧪 Running the Headless Logic Test Suite

To verify game mechanics, tile movement rules, state reset, solvability invariance, and solver paths across all supported board sizes:

```bash
cd src
javac *.java
java PuzzleLogicTest
```

Expected Output:
```
==================================================
  SLIDING PUZZLE LOGIC & SOLVABILITY TEST SUITE   
==================================================
✔ Test 1 Passed: Solved state correctly identified.
✔ Test 2 Passed: Inversion parity and solvability theorem verified.
✔ Test 3 Passed: Tile adjacency and move permissions validated.
✔ Test 4 Passed: Tile movement and empty slot coordinate updates verified.
✔ Test 5 Passed: Reset restores exact starting board layout.
✔ Test 6 Passed: 100% of 100 random shuffles guaranteed solvable.
✔ Test 7 Passed: Identical seeds generate identical daily boards.
✔ Test 8 Passed: 4x4 parity, shuffles, and tile movement verified.
✔ Test 9 Passed: 4x4 cells are square, closed, and image painting works.
✔ Test 10 Passed: AI solver finds valid paths for 3x3 through 6x6 boards.
✔ Test 11 Passed: AI solves shuffled 3x3 through 6x6 boards.
✔ Test 12 Passed: 5x5 and 6x6 solvability, movement, and rendering verified.
==================================================
Test Summary: 12 / 12 Tests Passed.
==================================================
SUCCESS: All core game logic verified without errors.
```

---

## 🎮 How to Play

1. **Objective:**
  Arrange numbered tiles in order from top-left to bottom-right, leaving the empty space at the bottom-right corner. Select either the 3×3 or 4×4 grid:
   ```
   1  2  3
   4  5  6
   7  8  [ ]
   ```
2. **Move Tiles:**
   - Click any tile adjacent to the empty slot to slide it.
   - Or press an **Arrow Key** / **WASD** to push tiles towards the empty slot.
3. **Invalid Moves:**
   - Clicking tiles that are not adjacent to the empty space does nothing (or plays a subtle blip if audio is on).
4. **Action Buttons:**
  - **New Game:** Starts a fresh solvable board.
   - **Shuffle:** Re-scrambles the existing layout.
   - **Reset:** Restores the exact scrambled arrangement from the start of the match, allowing you to try an alternative solution path.
    - **Move Goal:** Meet the move goal to earn gold; up to ten extra moves earns silver, and every clear earns bronze.
    - **Clue:** Spend one of three tokens to mark a tile's correct destination briefly.
    - **Daily:** Play the same UTC-date board as everyone else and improve your saved daily best.
    - **Image Gallery:** Select **Image...** to add a picture or **Gallery** to replay a saved one.
    - **Theme:** Choose Ocean, Garden, or Sunset.

---

## 🔮 Future Enhancements

- **Advanced 15-Puzzle Search:** Add pattern databases for faster solutions to deeply shuffled 4×4 boards.
- **Larger Board Sizes:** Expand to 5×5 (24-puzzle).
- **Expanded Records:** Add persistent best time and move records for each board size.
# Sliding-Puzzle-Game
