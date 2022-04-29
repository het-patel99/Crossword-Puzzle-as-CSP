import java.io.*;
import java.util.*;

public class cpcsp {
    public static void main(String args[]) throws FileNotFoundException, CloneNotSupportedException{

		// Crossword Solver function. args[0] is grid file and args[1] is wordlist file.
        cpSolver(args[0], args[1]);
    }

    public static void cpSolver(String crosswordBoardName, String wordListName) throws FileNotFoundException, CloneNotSupportedException{

        // Fetching all the words from the wordList and stored in listOfWords ArrayList.
		File readFile = new File(wordListName);
		Scanner inputReader = new Scanner(readFile);
        ArrayList<String> listOfWords = new ArrayList<String>();
        while(inputReader.hasNext()) {
			String currentLine = inputReader.nextLine();
			if (currentLine.chars().allMatch(Character::isLetter)) {
				listOfWords.add(currentLine);
			}
		}
		inputReader.close();
		
		// Fetching the grid and storing in the 2d char array. Here '*' represents blocked cells and 
		// ' ' represents unblocked cells where characters can be placed. 
		File readBoard = new File(crosswordBoardName);
		Scanner inputBoard = new Scanner(readBoard);
		int noOfLines = 0;
		int noOfCommas = 0;
		while(inputBoard.hasNext()) {
			String currentLine = inputBoard.nextLine();
			noOfLines++;
			noOfCommas = 0;
			for(int i=0;i<currentLine.length();i++){
				if(currentLine.charAt(i)==','){
					noOfCommas++;
				}
			}
			
		}
		inputBoard.close();
		Scanner inputBoardNew = new Scanner(readBoard);
		int index = 0;
		char crosswordBoard[][] = new char[noOfLines][noOfCommas+1];
		while(inputBoardNew.hasNext()) {
			String currentLine = inputBoardNew.nextLine();
			String currLineArr[] = currentLine.split(",");

			for(int i=0;i<currLineArr.length;i++){
				crosswordBoard[index][i] = currLineArr[i].charAt(0);
			}
			index++;
		}
		inputBoardNew.close();

		// A hashMap is created such that its key is word length and its value is Combination class 
		// where every word in the word dictionary are mapped to its length.
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

		// Use of findAllCombinations: From word dictionary, for every possible word length, 
		// a 26 x (possible word length) combinations are created and stored in such a way that 
		// every alphabets at every possible index of the word length.
		for (Integer size : combinationBySize.keySet()) {
			combinationBySize.get(size).findALLCombinations();
		}

		// creating Horizontal and Vertical Placement. Placement: a placement is a all possible available spaces 
		// to fill the character representing by its starting coordinates of the board and ending coordinates of 
		// the board. It also connects with the Combination in such a way that placement size is mapped with the 
		// combination size and all the words of that particular size are connected there. New arc and arcCons 
		// connection are initialized which helps in forward checking and arc consistency. Also the most Constraining 
		// and constrained placement heuristic is stored for each placement. Every Horizontal and Vertical Cross 
		// Connection is stored. 
		List<Placement> horizontalPlacements = new ArrayList<Placement>();
		List<Placement> verticalPlacements = new ArrayList<Placement>();
		getHorizontalPlacements(horizontalPlacements, crosswordBoard, combinationBySize);
		getVerticalPlacements(verticalPlacements, crosswordBoard, combinationBySize);

		// Connecting the Vertical Placement into Horizontal Placement.
		for (int i = 0; i < horizontalPlacements.size(); i++) {
			for (int j = 0; j < verticalPlacements.size(); j++) {
				int[] charPosition = horizontalPlacements.get(i).intersects(verticalPlacements.get(j));
				if (charPosition != null) {
					horizontalPlacements.get(i).crossConnection.add(new Intersection(verticalPlacements.get(j).id, charPosition[0], charPosition[1]));
				}
			}
			horizontalPlacements.get(i).mostConstrainingPlacementHeuristic = horizontalPlacements.get(i).crossConnection.size();
		}

		// Connecting the Horizontal Placement into Vertical Placement.
		for (int i = 0; i < verticalPlacements.size(); i++) {
			for (int j = 0; j < horizontalPlacements.size(); j++) {
				int[] charPosition = verticalPlacements.get(i).intersects(horizontalPlacements.get(j));
				if (charPosition != null) {
					verticalPlacements.get(i).crossConnection.add(new Intersection(horizontalPlacements.get(j).id, charPosition[0], charPosition[1]));
				}
			}
			verticalPlacements.get(i).mostConstrainingPlacementHeuristic = verticalPlacements.get(i).crossConnection.size();
		}

		// Defining the begin BoardState and adding all the horizontal and vertical placements
		ArrayList<Placement> beginState = new ArrayList<Placement>();
		BoardState initialS = new BoardState(beginState);
		beginState.addAll(horizontalPlacements);
		beginState.addAll(verticalPlacements);

		// sorting the begin BoardState by mostConstrainingPlacementHeuristic  (heuristic - 1)
		// MostConstrainingPlacementHeuristic: A placement with most contraining variable heuristic.
		Collections.sort(beginState, (a,b) -> b.mostConstrainingPlacementHeuristic - a.mostConstrainingPlacementHeuristic);

		ArrayList<Placement> beginStateCopyTemp = new ArrayList<Placement>();
		for (Placement placement : beginState) {
			beginStateCopyTemp.add((Placement) placement.clone());
		}
		ArrayList<Placement> beginStateCopy = new ArrayList<Placement>(beginStateCopyTemp);

		// sorting the begin BoardState by ID (heuristic - 2)
		Collections.sort(beginStateCopy, (a,b) -> a.id - b.id);

		// Creating a stack with Class BoardState and adding initial BoardState
		Stack<BoardState> currentboardState = new Stack<BoardState>();
		currentboardState.push(initialS);
		int backTrack = 0;
		char[][] currentCrossWordBoard;
		long startTime = System.currentTimeMillis();

		while (!currentboardState.isEmpty()) {
			
			// creating a Placement arraylist and fetching the present crosswordBoard
			ArrayList<Placement> tempPlacements = new ArrayList<Placement>();
			currentCrossWordBoard = displayPresentState(currentboardState.peek(), crosswordBoard);

			// Base Condition to check if the Board is solved or not.
			if (isBoardSolved(currentCrossWordBoard)) {
				System.out.println("Solution Found:\n");
				// Function to print the crossword board.
				displayBoard(currentCrossWordBoard);
				System.out.println();
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

			// Taking a clone of the peek element of the stack to the tempPlacements
			for (Placement placement : currentboardState.peek().placements) {
				tempPlacements.add((Placement) placement.clone());
			}

			// creating a new tempPlacements that holds the original tempPlacements values sorted by ID
			ArrayList<Placement> tempPlacementsCopy = new ArrayList<Placement>(tempPlacements);
			Collections.sort(tempPlacementsCopy, (a,b) -> a.id - b.id);

			// Fetching the placement based the mostConstrainingPlacementHeuristic. 
			Placement selectedPlacement = tempPlacements.get(0);
			int currentMCPS = selectedPlacement.mostConstrainingPlacementHeuristic;
			int mrvIndex = 0;
			int mrvPlacement = selectedPlacement.mostConstrainedPlacementHeuristic;

			for (int k = 0; k < tempPlacements.size(); k++) {
				if (tempPlacements.get(k).mostConstrainingPlacementHeuristic == currentMCPS && tempPlacements.get(k).combination != null) {
					if (tempPlacements.get(k).mostConstrainedPlacementHeuristic <= mrvPlacement) {
						mrvIndex = k;
						mrvPlacement = tempPlacements.get(k).mostConstrainedPlacementHeuristic;
					}
				} else
					break;
			}

			// mrvIndex represent the placement with most constraining Placement and minimum remaining value (Combination)
			selectedPlacement = tempPlacements.get(mrvIndex);

			// Checking if word is already assigned to the placement.
			if (selectedPlacement.mostConstrainedPlacementHeuristic <= 0) {
				currentboardState.clear();
				break;
			}

			// Here usedWordList is to keep track of all the used words. Adding all the used words here. 
			ArrayList<String> usedWordList = new ArrayList<String>();
			usedWordList.addAll(currentboardState.peek().usedWords);
			usedWordList.addAll(beginStateCopy.get(selectedPlacement.id).words);

			// getting a list of all the Combination that can fit initially (with the current Connection) sorted by their 
			// min conflict
			ArrayList<String> values = selectedPlacement.assignAValue(usedWordList, tempPlacementsCopy);

			// If no word can be assigned for the placement, then Backtrack.
			if (values == null) {
				beginStateCopy.get(selectedPlacement.id).words.clear();
				currentboardState.pop();
				backTrack++;
				continue;
			}

			boolean isStuck = false;

			// If any word assignment is possible for the placement then we will check for arc Consistency 
			// and checks if it pruns out the current placement. 
			for (int i = 0; i < values.size(); i++) {

				ArrayList<Placement> t1 = new ArrayList<Placement>();

				for (Placement placement : currentboardState.peek().placements) {
					t1.add((Placement) placement.clone());
				}
				Placement selected = t1.get(mrvIndex);
				ArrayList<Placement> t2 = new ArrayList<Placement>(t1);
				Collections.sort(t2, (a,b) -> a.id - b.id);
				boolean wordAlignmentPossible = selected.addNeighborConnection(values.get(i), t2, usedWordList);

				// Assigning the word to the placement if both forward checking and arc consistency are in favour of the placement. 
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

			// If from this placement if the forward checking
			// and the arc consistency are not feasible , then Backtrack.
			if (!isStuck) {
				beginStateCopy.get(selectedPlacement.id).words.clear();
				currentboardState.pop();
				backTrack++;
			}
		}

		long endTime = System.currentTimeMillis();

		// Display Outputs
		if (currentboardState.isEmpty()) {
			System.out.println("Solution doesn't exists");
			System.out.println("Total no. of backTracks: "+backTrack);
			System.out.println("Total Time Taken: " + (endTime - startTime) + "ms");
		} 
		else {
			System.out.println("Total no. of backTracks: "+backTrack);
			System.out.println("Total Time Taken: " + (endTime - startTime) + "ms");
		}
    }

	// Function to find continuous possible horizontal placements for complete crossword Board. 
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

	// Function to find continuous possible vertical placements for complete crossword Board.
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
	
	// Function to print the crossword board.
	public static void displayBoard(char[][] crosswordBoard) {
		for (int i = 0; i < crosswordBoard.length; i++) {
			for (int j = 0; j < crosswordBoard[0].length; j++) {
				System.out.print(crosswordBoard[i][j] + ",");
			}
			System.out.println();
		}
	}

	// Function to check if the Board is solved or not.
	public static boolean isBoardSolved(char[][] crosswordBoard) {
		for (int i = 0; i < crosswordBoard.length; i++) {
			for (int j = 0; j < crosswordBoard[0].length; j++) {
				if (crosswordBoard[i][j] == ' ')
					return false;
			}
		}
		return true;
	}

	// Function to assign the placement to the Board
	public static void assign(char[][] crosswordBoard, Placement placement) {
		if (placement.startXCoord == placement.endXCoord) {
			for (int i = 0; i < placement.currentPlacement.length(); i++) {
				crosswordBoard[placement.startXCoord][placement.startYCoord + i] = placement.currentPlacement.charAt(i);
			}
		} else {
			for (int i = 0; i < placement.currentPlacement.length(); i++) {
				crosswordBoard[placement.startXCoord + i][placement.startYCoord] = placement.currentPlacement.charAt(i);
			}
		}
	}

	// Function to display any given state of the board.  
	public static char[][] displayPresentState(BoardState state, char[][] tempArr) {

		char[][] newcrosswordBoard = new char[tempArr.length][tempArr[0].length];
		for (int i = 0; i < tempArr.length; i++) {
			for (int j = 0; j < tempArr[0].length; j++) {
				newcrosswordBoard[i][j] = tempArr[i][j];
			}
		}

		ArrayList<Placement> currentPlacement = state.placements;

		for (int i = 0; i < currentPlacement.size(); i++) {
			Placement placement = currentPlacement.get(i);
			assign(newcrosswordBoard, placement);
		}
		displayBoard(newcrosswordBoard);
		System.out.println();
		System.out.println("******************************\n");
		return newcrosswordBoard;
	}
}