import java.util.*;

// Placement: a placement is a all possible available spaces 
// to fill the character representing by its starting coordinates of the board and ending coordinates of 
// the board. It also connects with the Combination in such a way that placement size is mapped with the 
// combination size and all the words of that particular size are connected there. New arc and arcCons 
// connection are initialized which helps in forward checking and arc consistency. Also the most Constraining 
// and constrained placement heuristic is stored for each placement. Every Horizontal and Vertical Cross 
// Connection is stored. 
public class Placement implements Cloneable{
    Combination combination;
    List<Connection> cons;
    List<Connection> arcCons;
    int startXCoord;
    int startYCoord;
    int endXCoord;
    int endYCoord;
    int id;
    static int pIDCounter = 0;
    List<String> words;
    List<Intersection> crossConnection;
    int[][] mostConstrainingCombinationHeuristic;
	int size;
	String currentPlacement = "";
	int mostConstrainingPlacementHeuristic;
	int mostConstrainedPlacementHeuristic;
	int usedWordIndex;

    Placement(int startXCoord, int startYCoord, int endXCoord, int endYCoord, Combination combination) {
		this.startXCoord = startXCoord;
		this.startYCoord = startYCoord;
		this.endXCoord = endXCoord;
		this.endYCoord = endYCoord;
		this.combination = combination;
		cons = new ArrayList<Connection>();
		arcCons = new ArrayList<Connection>();
		crossConnection = new ArrayList<Intersection>();
		words = new ArrayList<String>();
		id = pIDCounter++;
		if (startXCoord == endXCoord) {
			for (int i = 0; i < (endYCoord - startYCoord + 1); i++) {
				currentPlacement += " ";
			}
		} else {
			for (int i = 0; i < (endXCoord - startXCoord + 1); i++) {
				currentPlacement += " ";
			}
		}
		if (combination != null) {
			mostConstrainingCombinationHeuristic = new int[26][combination.size];
			setMostConstrainingValueHeuristic(new ArrayList<String>());
		}
		usedWordIndex = -1;
	}

	// Checking for arc Consistency
	public boolean arcCons(ArrayList<String> wordsUsed, ArrayList<Placement> sortedByID) {

		ArrayList<String> availableValues = allAvailableWords();
		for (int i = 0; i < wordsUsed.size(); i++) {
			availableValues.remove(wordsUsed.get(i));
		}
		int[] usedIndex = new int[combination.size];
		for (int i = 0; i < usedIndex.length; i++) {
			usedIndex[i] = -1;
		}
		for (int i = 0; i < crossConnection.size(); i++) {
			if (sortedByID.get(crossConnection.get(i).id).mostConstrainingPlacementHeuristic != -1) {
				usedIndex[crossConnection.get(i).sIndex] = i;

				Intersection intersection = crossConnection.get(i);
				Placement adjNeighbors = sortedByID.get(intersection.id);
				for (int j = 0; j < 26; j++) {
					// System.out.println(adjNeighbors.mostConstrainingCombinationHeuristic);
					if (adjNeighbors.mostConstrainingCombinationHeuristic[j][intersection.dPosition] != 0) {
						char c = (char) (j + 'a');
						arcCons.add(new Connection(c, intersection.sIndex));
					}
				}
				boolean isUpdated = updatePlacement(wordsUsed);
				if (!isUpdated) {
					return false;
				}
				for (int j = 0; j < 26; j++) {
					if (mostConstrainingCombinationHeuristic[j][intersection.sIndex] != 0) {
						if (adjNeighbors.startXCoord == 5 && adjNeighbors.startYCoord == 6 && adjNeighbors.endXCoord == 8 && adjNeighbors.endYCoord == 6) {
						}
						char c = (char) (j + 'a');
						adjNeighbors.arcCons.add(new Connection(c, intersection.dPosition));
					}
				}
				isUpdated = adjNeighbors.updatePlacement(wordsUsed);
				if (!isUpdated) {
					return false;
				}
			}
		}
		return true;
	}

	public ArrayList<String> assignAValue(ArrayList<String> wordsUsed, ArrayList<Placement> sortedByID) {

		ArrayList<String> availableValues = allAvailableWords();
		for (int i = 0; i < wordsUsed.size(); i++) {
			availableValues.remove(wordsUsed.get(i));
		}
		int[] usedIndex = new int[combination.size];
		for (int i = 0; i < usedIndex.length; i++) {
			usedIndex[i] = -1;
		}
		int hasChange = 0;
		for (int i = 0; i < crossConnection.size(); i++) {
			if (sortedByID.get(crossConnection.get(i).id).mostConstrainingPlacementHeuristic != -1) {
				usedIndex[crossConnection.get(i).sIndex] = i;
				hasChange++;
			}
		}
		ArrayList<BestCombination> getBestCombination = new ArrayList<BestCombination>();
		ArrayList<String> possibleValues = new ArrayList<String>();
		if (hasChange == 0) {
			if (availableValues.size() == 0)
				return null;
			return availableValues;
		}

		for (int i = 0; i < availableValues.size(); i++) {

			String currentWord = availableValues.get(i);
			int numberOfAvailableNeighborsOptions = 0;
			boolean isWordPossible = true;
			for (int j = 0; j < currentWord.length(); j++) {
				char ch = currentWord.charAt(j);
				if (usedIndex[j] != -1) {
					Intersection intersection = crossConnection.get(usedIndex[j]);
					Placement adjNeighbors = sortedByID.get(intersection.id);
					int noOfTimesWords = adjNeighbors.mostConstrainingCombinationHeuristic[ch - 'a'][intersection.dPosition];
					if (noOfTimesWords != 0) {
						numberOfAvailableNeighborsOptions += noOfTimesWords;
					} else {
						isWordPossible = false;
						break;
					}
				}
			}
			if (isWordPossible) {
				getBestCombination.add(new BestCombination(numberOfAvailableNeighborsOptions, i));
			}
		}
		Collections.sort(getBestCombination, (fv1,fv2)-> fv2.possibleWordsCounter - fv1.possibleWordsCounter);
		if (getBestCombination.size() == 0) {
			return null;
		} else {
			for (int i = 0; i < getBestCombination.size(); i++) {
				possibleValues.add(availableValues.get(getBestCombination.get(i).index));
			}
			return possibleValues;
		}
	}

	public boolean setMostConstrainingValueHeuristic(ArrayList<String> wordsUsed) {

		ArrayList<String> availableValues = allAvailableWords();
		for (int i = 0; i < wordsUsed.size(); i++) {
			availableValues.remove(wordsUsed.get(i));
		}

		this.mostConstrainedPlacementHeuristic= availableValues.size();

		for (int i = 0; i < availableValues.size(); i++) {
			for (int j = 0; j < availableValues.get(i).length(); j++) {
				boolean match = false;

				boolean ischarAtIndexThere = false;
				for (int w = 0; w < arcCons.size(); w++) {
					if (arcCons.get(w).position == j) {
						ischarAtIndexThere = true;
						if (arcCons.get(w).character == availableValues.get(i).charAt(j)) {
							match = true;
						}
					}
				}

				if (ischarAtIndexThere) {
					if (!match) {
						availableValues.remove(i);
						i--;
						break;
					}

				}
			}
		}

		for (int i = 0; i < availableValues.size(); i++) {
			for (int j = 0; j < availableValues.get(i).length(); j++) {
				mostConstrainingCombinationHeuristic[availableValues.get(i).charAt(j) - 'a'][j]++;
			}
		}

		if (availableValues.size() == 0){
			return false;
		}
		return true;
	}

	public ArrayList<String> allAvailableWords() {

		ArrayList<String> availableValues;
		if (cons.size() != 0) {
			availableValues = new ArrayList<String>(combination.get(cons.get(0).character, cons.get(0).position));
			for (int i = 1; i < cons.size(); i++) {
				availableValues.retainAll(combination.get(cons.get(i).character, cons.get(i).position));
			}
		} else {
			availableValues = new ArrayList<String>(combination.wordList);
		}

		return availableValues;
	}

	Placement(int startXCoord, int startYCoord, int endXCoord, int endYCoord) {
		this.startXCoord = startXCoord;
		this.startYCoord = startYCoord;
		this.endXCoord = endXCoord;
		this.endYCoord = endYCoord;
		if (startXCoord == endXCoord) {
			for (int i = 0; i < (endYCoord - startYCoord + 1); i++) {
				currentPlacement += " ";
			}
		} else {
			for (int i = 0; i < (endXCoord - startXCoord + 1); i++) {
				currentPlacement += " ";
			}
		}
		crossConnection = new ArrayList<Intersection>();

		id = pIDCounter++;

	}

	@Override
	protected Object clone() throws CloneNotSupportedException {

		Placement placement = (Placement) super.clone();

		placement.arcCons = new ArrayList<Connection>();
		for (Connection c : arcCons) {
			placement.arcCons.add((Connection) c.clone());
		}
		placement.cons = new ArrayList<Connection>();
		for (Connection c : cons) {
			placement.cons.add((Connection) c.clone());
		}
		placement.currentPlacement = new String(currentPlacement);
		return placement;
	}

	public boolean addNeighborConnection(String assigned, ArrayList<Placement> sortedByID,
			ArrayList<String> wordsUsed) {

		int[] usedIndex = new int[combination.size];

		for (int i = 0; i < usedIndex.length; i++) {
			usedIndex[i] = -1;
		}

		for (int i = 0; i < crossConnection.size(); i++) {
			if (sortedByID.get(crossConnection.get(i).id).mostConstrainingPlacementHeuristic != -1) {
				usedIndex[crossConnection.get(i).sIndex] = i;
			}
		}

		for (int i = 0; i < assigned.length(); i++) {
			char currentChar = assigned.charAt(i);

			if (usedIndex[i] != -1) {
				Intersection intersection = crossConnection.get(usedIndex[i]);
				Placement adjNeighbors = sortedByID.get(intersection.id);
				adjNeighbors.cons.add(new Connection(currentChar, intersection.dPosition));
				adjNeighbors.currentPlacement = adjNeighbors.currentPlacement.substring(0, intersection.dPosition) + currentChar
						+ adjNeighbors.currentPlacement.substring(intersection.dPosition + 1);
				adjNeighbors.mostConstrainingPlacementHeuristic--;
				boolean isUpdated = adjNeighbors.updatePlacement(wordsUsed);
				if (!isUpdated) {
					return false;
				}
				isUpdated = adjNeighbors.arcCons(wordsUsed, sortedByID);
				if (!isUpdated) {
					return false;
				}
			}
		}
		mostConstrainingPlacementHeuristic = -1;
		return true;
	}

	public boolean updatePlacement(ArrayList<String> wordsUsed) {
		if (combination != null) {
			mostConstrainingCombinationHeuristic = new int[26][combination.size];
			boolean flag = setMostConstrainingValueHeuristic(wordsUsed);
			return flag;
		}
		return false;
	}

	public int[] intersects(Placement placement) {

		int[] charPosition = new int[2];
		if (placement.startXCoord == placement.endXCoord && startXCoord == endXCoord) {
			return null;
		}
		if (placement.startYCoord == placement.endYCoord && startYCoord == endYCoord) {
			return null;
		}

		// Same Row
		if (startXCoord == endXCoord) {
			if (startYCoord <= placement.startYCoord && endYCoord >= placement.startYCoord) {
				if (placement.startXCoord <= startXCoord && placement.endXCoord >= startXCoord) {
					charPosition[0] = placement.startYCoord - startYCoord;
					charPosition[1] = startXCoord - placement.startXCoord;

					return charPosition;
				} else
					return null;
			} else
				return null;
		}
		
		// Same Col
		if (startYCoord == endYCoord) {
			if (startXCoord <= placement.startXCoord && endXCoord >= placement.startXCoord) {
				if (placement.startYCoord <= startYCoord && placement.endYCoord >= startYCoord) {
					charPosition[0] = placement.startXCoord - startXCoord;
					charPosition[1] = startYCoord - placement.startYCoord;

					return charPosition;
				} else
					return null;
			} else
				return null;
		}

		return null;
	}

	public String toString() {
		return "["+mostConstrainingPlacementHeuristic + "|" + mostConstrainedPlacementHeuristic + "|(" + startXCoord + "," + startYCoord
				+ ") to (" + endXCoord + " ," + endYCoord + ")  ->  {" + currentPlacement + "}" + "  " + id + "]";
	}
	
}

class Connection implements Cloneable {

    char character;
    int position;

    Connection(char character, int position){
        this.character = character;
        this.position = position;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException{
        return super.clone();
    }
}

class Intersection {

    int id;
    int sIndex;
    int dPosition;

    Intersection(int id, int sIndex, int dPosition){
        this.id = id;
        this.sIndex = sIndex;
        this.dPosition = dPosition;
    }

    public String toString() {
		return "[Intersection Id : " + id + " Source index: " + sIndex + " Destination position: " + dPosition+"]";
	}
}

class BestCombination {

    int possibleWordsCounter;
    int index;

    BestCombination(int possibleWordsCounter, int index){
        super();
        this.possibleWordsCounter = possibleWordsCounter;
        this.index = index;
    }

    public String toString() {
		return "[possible Words Counter: " + possibleWordsCounter + " Index: " + index + "]";
	}
}
class Combination {

	ArrayList<String> wordList;
	int size;
	ArrayList<String>[] allCombinations;

	Combination(int size) {
		this.size = size;
		wordList = new ArrayList<String>();
	}

	public ArrayList<String> get(char letter, int position) {
		if ((letter - 'a') * size + position < allCombinations.length && (letter - 'a') * size + position >= 0) {
			return allCombinations[(letter - 'a') * size + position];
		}
		return null;
	}

	// Use of findAllCombinations: From word dictionary, for every possible word length, 
	// a 26 x (possible word length) combinations are created and stored in such a way that 
	// every alphabets at every possible index of the word length.
	public void findALLCombinations() {

		allCombinations = new ArrayList[26 * size];
		for (int i = 0; i < 26 * size; i++) {
			allCombinations[i] = new ArrayList<String>();
		}

		for (int i = 0; i < wordList.size(); i++) {
			String str = wordList.get(i);
			for (int j = 0; j < str.length(); j++) {
				int characterIndex = str.charAt(j) - 'a';
				allCombinations[characterIndex * size + j].add(str);

			}
		}

	}

	public String toString() {
		return wordList.toString();
	}
}

class BoardState {

	ArrayList<String> usedWords;
	ArrayList<Placement> placements;
	String selectedWord;

	BoardState(ArrayList<Placement> placements) {
		this.placements = placements;
		usedWords = new ArrayList<String>();
		for (int i = 0; i < placements.size(); i++) {
			if (placements.get(i).mostConstrainingPlacementHeuristic == -1) {
				usedWords.add(placements.get(i).currentPlacement);
			}
		}

	}

	public String toString() {
		return placements.toString();
	}

}