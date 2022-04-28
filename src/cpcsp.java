import java.io.*;
import java.util.*;

public class cpcsp {
    public static void main(String args[]) throws FileNotFoundException, CloneNotSupportedException{
        cpSolver(args[0], args[1]);
    }

    public static void cpSolver(String crosswordBoardName, String wordListName) throws FileNotFoundException, CloneNotSupportedException{

        File readFile = new File(wordListName);
		Scanner inputReader = new Scanner(readFile);
        ArrayList<String> listOfWords = new ArrayList<String>();
        while(inputReader.hasNext()) {
			String currentLine = inputReader.nextLine();
			if (currentLine.chars().allMatch(Character::isLetter)) {
				listOfWords.add(currentLine);
			}
		}
		
		List<List<Character>> tempCrossWordBoard = new ArrayList<>();

		File readBoard = new File(crosswordBoardName);
		Scanner inputBoard = new Scanner(readBoard);
		while(inputBoard.hasNext()) {
			String currentLine = inputBoard.nextLine();
			String currentBoardRow[] = currentLine.split(",");
			List<Character> tempBoardRow = new ArrayList<>();
			for(int i=0;i<currentBoardRow.length;i++){
				tempBoardRow.add(currentBoardRow[i].charAt(0));
			}
			tempCrossWordBoard.add(tempBoardRow);
		}

		char crosswordBoard[][] = new char[tempCrossWordBoard.size()][tempCrossWordBoard.get(0).size()];
		for(int i=0;i<crosswordBoard.length;i++){
			for(int j=0;j<crosswordBoard.length;j++){
				crosswordBoard[i][j] = tempCrossWordBoard.get(i).get(j);
			}
		}

        HashMap<Integer, Combination> combinationBySize = new HashMap<Integer, Combination>();
		for (int i = 0; i < listOfWords.size(); i++) {

			String word = listOfWords.get(i);
			int size = word.length();

			if (combinationBySize.containsKey(size)) {
				Combination combination = combinationBySize.get(size);
				combination.wordList.add(word.toLowerCase());
			} else {
				Combination combination = new Combination(size);
				combination.wordList.add(word.toLowerCase());
				combinationBySize.put(size, combination);
			}

		}
		for (Integer size : combinationBySize.keySet()) {
			combinationBySize.get(size).findALLCombinations();
		}

		List<Placement> horizontalPlacements = new ArrayList<Placement>();
		List<Placement> verticalPlacements = new ArrayList<Placement>();

		getHorizontalPlacements(horizontalPlacements, crosswordBoard, combinationBySize);
		getVerticalPlacements(verticalPlacements, crosswordBoard, combinationBySize);
		
		displayBoard(crosswordBoard);

		// connecting the neighbors (Vertical into Horizontal)
		for (int i = 0; i < horizontalPlacements.size(); i++) {
			for (int j = 0; j < verticalPlacements.size(); j++) {
				int[] charPosition = horizontalPlacements.get(i).intersects(verticalPlacements.get(j));
				if (charPosition != null) {
					horizontalPlacements.get(i).crossConnection.add(new Intersection(verticalPlacements.get(j).id, charPosition[0], charPosition[1]));
				}
			}
			horizontalPlacements.get(i).mostConstrainingPlacementHeuristic = horizontalPlacements.get(i).crossConnection.size();
		}

		// connecting the neighbors (Horizontal into Vertical)
		for (int i = 0; i < verticalPlacements.size(); i++) {
			for (int j = 0; j < horizontalPlacements.size(); j++) {
				int[] charPosition = verticalPlacements.get(i).intersects(horizontalPlacements.get(j));
				if (charPosition != null) {
					// System.out.println("Info0: "+info[0]+"Info1: "+info[1]);
					verticalPlacements.get(i).crossConnection.add(new Intersection(horizontalPlacements.get(j).id, charPosition[0], charPosition[1]));
				}
			}
			verticalPlacements.get(i).mostConstrainingPlacementHeuristic = verticalPlacements.get(i).crossConnection.size();
		}

		// defining the begin BoardState
		ArrayList<Placement> beginState = new ArrayList<Placement>();
		BoardState initialS = new BoardState(beginState);

		// adding all the horizontal and vertical placements
		beginState.addAll(horizontalPlacements);
		beginState.addAll(verticalPlacements);

		// sorting the begin BoardState by mostConstrainingVariableHeuristic  (heuristic - 1)
		Collections.sort(beginState, (a,b) -> b.mostConstrainingPlacementHeuristic - a.mostConstrainingPlacementHeuristic);

		ArrayList<Placement> beginStateCopyTemp = new ArrayList<Placement>();
		for (Placement placement : beginState) {
			beginStateCopyTemp.add((Placement) placement.clone());
		}
		ArrayList<Placement> beginStateCopy = new ArrayList<Placement>(beginStateCopyTemp);

		// sorting the begin BoardState by ID (heuristic - 2)
		Collections.sort(beginStateCopy, (a,b) -> a.id - b.id);

		// defining the stack and pushing begin BoardState
		Stack<BoardState> currentboardState = new Stack<BoardState>();
		currentboardState.push(initialS);

		int iterationNumber = 0;
		int backTrack = 0;
		char[][] currentCrossWordBoard;

		while (!currentboardState.isEmpty()) {
			iterationNumber++;
			// creating an array list of placements
			ArrayList<Placement> tempPlacements = new ArrayList<Placement>();
			// printing the current crosswordBoard
			currentCrossWordBoard = displayPresentState(currentboardState.peek(), crosswordBoard);

			// checking if the current crosswordBoard is solved
			if (isBoardSolved(currentCrossWordBoard)) {
				ArrayList<String> hors = new ArrayList<String>();
				ArrayList<String> vers = new ArrayList<String>();
				for (int i = 0; i < currentboardState.peek().placements.size(); i++) {
					if (currentboardState.peek().placements.get(i).startXCoord == currentboardState.peek().placements.get(i).endXCoord) {
						hors.add(currentboardState.peek().placements.get(i).currentPlacement);
					} else
						vers.add(currentboardState.peek().placements.get(i).currentPlacement);
				}
				break;
			}

			// cloning the peek to the tempPlacements
			for (Placement placement : currentboardState.peek().placements) {
				tempPlacements.add((Placement) placement.clone());
			}

			// creating a new tempPlacements that holds the original tempPlacements values sorted by ID
			ArrayList<Placement> tempPlacementsCopy = new ArrayList<Placement>(tempPlacements);

			// sorting the new tempPlacements
			Collections.sort(tempPlacementsCopy, (a,b) -> a.id - b.id);

			// getting the Placement with the highest heuristic
			Placement selectedPlacement = tempPlacements.get(0);
			int cc = selectedPlacement.mostConstrainingPlacementHeuristic;
			int mrvIndex = 0;
			int mrvPlacement = selectedPlacement.mostConstrainedPlacementHeuristic;

			for (int k = 0; k < tempPlacements.size(); k++) {
				if (tempPlacements.get(k).mostConstrainingPlacementHeuristic == cc && tempPlacements.get(k).combination != null) {
					if (tempPlacements.get(k).mostConstrainedPlacementHeuristic <= mrvPlacement) {
						mrvIndex = k;
						mrvPlacement = tempPlacements.get(k).mostConstrainedPlacementHeuristic;
					}
				} else
					break;
			}

			// now min index represent the Placement with both most constraining Placement and minimum remaining value
			selectedPlacement = tempPlacements.get(mrvIndex);

			// if the Placement is already filled
			if (selectedPlacement.mostConstrainedPlacementHeuristic <= 0) {
				currentboardState.clear();
				break;
			}

			// this list will have all the used words
			ArrayList<String> newList = new ArrayList<String>();

			// adding the used words in the crosswordBoard
			newList.addAll(currentboardState.peek().usedWords);
			// System.out.println("used words from crosswordBoard added: "+newList); adding the used words in that Placement ( useful in backtracking )
			newList.addAll(beginStateCopy.get(selectedPlacement.id).words);

			// getting a list of all the values that can fit initially (with the current Connection) and also sorted by their min conflict
			ArrayList<String> values = selectedPlacement.assignAValue(newList, tempPlacementsCopy);

			// if there's no values, then backtrack
			if (values == null) {
				beginStateCopy.get(selectedPlacement.id).words.clear();
				currentboardState.pop();
				backTrack++;
				continue;
			}

			boolean isStuck = false;

			// otherwise we will traverse the values to see who satisfy both the forward
			// checking and the arc consistency
			for (int i = 0; i < values.size(); i++) {

				ArrayList<Placement> t1 = new ArrayList<Placement>();

				for (Placement placement : currentboardState.peek().placements) {
					t1.add((Placement) placement.clone());
				}
				Placement selected = t1.get(mrvIndex);
				ArrayList<Placement> t2 = new ArrayList<Placement>(t1);
				Collections.sort(t2, (a,b) -> a.id - b.id);
				boolean wordAlignmentPossible = selected.addNeighborConnection(values.get(i), t2, newList);

				// if the ith value satisfies both the arc consistency and the forward checking then we will assign it safely
				if (wordAlignmentPossible) {
					selected.currentPlacement = values.get(i);
					beginStateCopy.get(selected.id).words.add(values.get(i));
					Collections.sort(t1, (a,b) -> b.mostConstrainingPlacementHeuristic - a.mostConstrainingPlacementHeuristic);
					BoardState nextState = new BoardState(t1);
					nextState.selectedWord = selected.currentPlacement;
					currentboardState.push(nextState);
					isStuck = true;
					break;
				}

			}

			// if the loop is done and there's no value that satisfy the forward checking
			// and the arc consistency, then we will backtrack
			if (!isStuck) {
				beginStateCopy.get(selectedPlacement.id).words.clear();
				currentboardState.pop();
				backTrack++;
			}
		}

		if (currentboardState.isEmpty()) {
			System.out.println("Solution doesn't exists");
		} 
		else {
			System.out.println("Solution Found");
			System.out.println("Total no. of backTracks: "+backTrack);
		}
    }

	public static void getHorizontalPlacements(List<Placement> horizontalPlacements, char[][] crosswordBoard, HashMap<Integer, Combination> combinationBySize) {

		for (int i = 0; i < crosswordBoard.length; i++) {
			int position = 0;
			for (int j = 0; j < crosswordBoard[i].length; j++) {
				if (crosswordBoard[i][j] == '*') {
					if (position < j) {
						int size = j - position;
						if (size != 1) {
							Combination combination = combinationBySize.get(size);
							if (combination != null) {
								Placement placement = new Placement(i, position, i, j - 1, combination);
								horizontalPlacements.add(placement);
							}
						}
					}
					position = j + 1;
				}
			}
			if (position != crosswordBoard[i].length) {
				int size = crosswordBoard[i].length - position;
				if (size != 1) {
					Combination combination = combinationBySize.get(size);
					if (combination != null) {
						Placement placement = new Placement(i, position, i, crosswordBoard[i].length - 1, combination);
						horizontalPlacements.add(placement);
					}
				}

			}
		}

	}

	public static void getVerticalPlacements(List<Placement> verticalPlacements, char[][] crosswordBoard, HashMap<Integer, Combination> combinationBySize) {

		for (int i = 0; i < crosswordBoard[0].length; i++) {
			int position = 0;
			for (int j = 0; j < crosswordBoard.length; j++) {
				if (crosswordBoard[j][i] == '*') {
					if (position < j) {
						int size = j - position;
						if (size != 1) {
							Combination combination = combinationBySize.get(size);
							if (combination != null) {
								Placement placement = new Placement(position, i, j - 1, i, combination);
								verticalPlacements.add(placement);
							}
						}
					}
					position = j + 1;
				}
			}
			if (position != crosswordBoard.length) {
				int size = crosswordBoard.length - position;
				if (size != 1) {
					Combination combination = combinationBySize.get(size);
					if (combination != null) {
						Placement placement = new Placement(position, i, crosswordBoard.length - 1, i, combination);
						verticalPlacements.add(placement);
					}
				}
			}
		}
	}
	
	public static void displayBoard(char[][] crosswordBoard) {
		for (int i = 0; i < crosswordBoard.length; i++) {
			for (int j = 0; j < crosswordBoard[0].length; j++) {
				System.out.print(crosswordBoard[i][j] + ",");
			}
			System.out.println();
		}
	}

	public static boolean isBoardSolved(char[][] crosswordBoard) {
		for (int i = 0; i < crosswordBoard.length; i++) {
			for (int j = 0; j < crosswordBoard[0].length; j++) {
				if (crosswordBoard[i][j] == ' ')
					return false;
			}
		}
		return true;
	}

	public static void assign(char[][] arr, Placement placement) {
		if (placement.startXCoord == placement.endXCoord) {
			for (int i = 0; i < placement.currentPlacement.length(); i++) {
				arr[placement.startXCoord][placement.startYCoord + i] = placement.currentPlacement.charAt(i);
			}
		} else {
			for (int i = 0; i < placement.currentPlacement.length(); i++) {
				arr[placement.startXCoord + i][placement.startYCoord] = placement.currentPlacement.charAt(i);
			}
		}
	}

	public static char[][] displayPresentState(BoardState s, char[][] grid) {

		char[][] newcrosswordBoard = new char[grid.length][grid[0].length];

		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[0].length; j++) {
				newcrosswordBoard[i][j] = grid[i][j];
			}
		}

		ArrayList<Placement> currentPlacement = s.placements;

		for (int i = 0; i < currentPlacement.size(); i++) {
			Placement placement = currentPlacement.get(i);
			assign(newcrosswordBoard, placement);
		}
		displayBoard(newcrosswordBoard);
		System.out.println();
		System.out.println("******************************");
		System.out.println();
		return newcrosswordBoard;
	}
}