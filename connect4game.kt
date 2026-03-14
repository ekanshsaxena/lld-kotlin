enum class PlaceDiscResult {
    SUCCESS,
    INVALID_COLUMN,
    COLUMN_FULL
}

enum class DiscColor(val symbol: Char) {
    RED('R'),
    YELLOW('Y')
}

enum class GameStatus {
    NOT_STARTED,
    IN_PROGRESS,
    WON,
    DRAWN
}

class Player(val discColor: DiscColor)

class Board {
    private val MAX_ROW = 6
    private val MAX_COL = 7
    private val matrix: MutableList<MutableList<DiscColor?>> =
            MutableList(MAX_ROW) { MutableList(MAX_COL) { null } }
    private val lastAvailableRows: MutableList<Int> = MutableList(MAX_COL) { MAX_ROW - 1 }
    private var filledCells: Int = 0
    private var lastUpdatedCell: MutableList<Int> = MutableList(2) { -1 }

    fun placeDisc(col: Int, player: Player): PlaceDiscResult {
        if (col < 0 || col >= MAX_COL) return PlaceDiscResult.INVALID_COLUMN
        val row = lastAvailableRows[col]
        if (row < 0) return PlaceDiscResult.COLUMN_FULL
        matrix[row][col] = player.discColor
        filledCells++
        lastUpdatedCell = mutableListOf(row, col)
        lastAvailableRows[col]--
        return PlaceDiscResult.SUCCESS
    }

    fun checkWinner(): Boolean {
        if (filledCells == 0) return false
        val row = lastUpdatedCell[0]
        val col = lastUpdatedCell[1]
        val lastUpdatedColor = matrix[row][col]

        var leftFilledCells = 0
        var newCol = col - 1
        while (newCol >= 0 && matrix[row][newCol] == lastUpdatedColor) {
            leftFilledCells++
            newCol--
        }
        var rightFilledCells = 0
        newCol = col + 1
        while (newCol < MAX_COL && matrix[row][newCol] == lastUpdatedColor) {
            rightFilledCells++
            newCol++
        }
        if (leftFilledCells + rightFilledCells + 1 >= 4) return true

        var topFilledCells = 0
        var newRow = row - 1
        while (newRow >= 0 && matrix[newRow][col] == lastUpdatedColor) {
            topFilledCells++
            newRow--
        }
        var bottomFilledCells = 0
        newRow = row + 1
        while (newRow < MAX_ROW && matrix[newRow][col] == lastUpdatedColor) {
            bottomFilledCells++
            newRow++
        }
        if (topFilledCells + bottomFilledCells + 1 >= 4) return true

        var topLeftFilledCells = 0
        newRow = row - 1
        newCol = col - 1
        while (newRow >= 0 && newCol >= 0 && matrix[newRow][newCol] == lastUpdatedColor) {
            topLeftFilledCells++
            newRow--
            newCol--
        }
        var bottomRightFilledCells = 0
        newRow = row + 1
        newCol = col + 1
        while (newRow < MAX_ROW && newCol < MAX_COL && matrix[newRow][newCol] == lastUpdatedColor) {
            bottomRightFilledCells++
            newRow++
            newCol++
        }
        if (topLeftFilledCells + bottomRightFilledCells + 1 >= 4) return true

        var topRightFilledCells = 0
        newRow = row - 1
        newCol = col + 1
        while (newRow >= 0 && newCol < MAX_COL && matrix[newRow][newCol] == lastUpdatedColor) {
            topRightFilledCells++
            newRow--
            newCol++
        }
        var bottomLeftFilledCells = 0
        newRow = row + 1
        newCol = col - 1
        while (newRow < MAX_ROW && newCol >= 0 && matrix[newRow][newCol] == lastUpdatedColor) {
            bottomLeftFilledCells++
            newRow++
            newCol--
        }
        if (topRightFilledCells + bottomLeftFilledCells + 1 >= 4) return true

        return false
    }

    fun checkDraw(): Boolean {
        return filledCells == (MAX_ROW * MAX_COL)
    }
}

class Game {
    private lateinit var board: Board
    private lateinit var players: List<Player>
    private var currentTurn: Int = 0
    var gameStatus: GameStatus = GameStatus.NOT_STARTED
        private set
    var winner: Player? = null
        private set

    fun isGameOver(): Boolean = gameStatus == GameStatus.WON || gameStatus == GameStatus.DRAWN

    fun startGame() {
        players = listOf(Player(DiscColor.RED), Player(DiscColor.YELLOW))
        board = Board()
        currentTurn = 0
        gameStatus = GameStatus.IN_PROGRESS
        winner = null

        while (true) {
            val currentPlayer = players[currentTurn]
            print("Player ${currentPlayer.discColor}: choose the col: ")
            val chosenCol = readLine()?.toIntOrNull()
            if (chosenCol == null) {
                print("Invalid input, please enter a number")
                continue
            }
            when (board.placeDisc(chosenCol, currentPlayer)) {
                PlaceDiscResult.INVALID_COLUMN -> {
                    println("Column $chosenCol is out of bounds, please choose between 0 and 6")
                    continue
                }
                PlaceDiscResult.COLUMN_FULL -> {
                    println("Column $chosenCol is already full, please choose another")
                    continue
                }
                PlaceDiscResult.SUCCESS -> {
                    if (board.checkWinner()) {
                        winner = currentPlayer
                        gameStatus = GameStatus.WON
                        println("Player ${currentPlayer.discColor} won the Game!")
                        break
                    }
                    if (board.checkDraw()) {
                        gameStatus = GameStatus.DRAWN
                        println("Game is a Draw!")
                        break
                    }
                    currentTurn = changeTurn(currentTurn)
                }
            }
        }
    }

    private fun changeTurn(currentTurn: Int): Int {
        return (currentTurn + 1) % players.size
    }
}

fun main() {
    val game = Game()
    game.startGame()
    println("Game Status: ${game.gameStatus}")
    game.winner?.let { println("Winner disc color: ${it.discColor}") }
}
