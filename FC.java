import java.util.ArrayList;
import java.util.HashMap;

public class FC {
	private BinaryCSPReader reader = new BinaryCSPReader(); //object for reading the csp prnewovided.
	private BinaryCSP binaryCSP;
	private ArrayList<BinaryConstraint> constraints;
	
	private ArrayList<Integer> unallocatedVars = new ArrayList<Integer>(); 	//List of unallocated variables (varList).
	private ArrayList<Integer> allocatedVars = new ArrayList<Integer>(); 	//List of allocated variables.
	//Hashmap of every variable and its domain(as an arraylist).
	private HashMap<Integer, ArrayList<Integer>> varDomains = new HashMap<Integer, ArrayList<Integer>>();

	private int numberOfNodes = 0; 	//Count the number of Fc traversal.
	protected long arcRevisions = 0;     //Number of arcs traversed.
	private boolean solutionFound = false; 	//Checks if a solution was found (Used to stop the algorithm).
	
	private long start; //to measure the time taken to find a solution.
	public String varOrder; //Stores the string provided by the user stating the order of variable selection.

	/**
	 * Constructor for the forward checking class.
	 * @param args The whole arary of arguments provided by the user.
	 */
	public FC(String[] args) {

		String instance = args[0];
        varOrder = args[2];
		binaryCSP = reader.readBinaryCSP(instance);
		constraints = binaryCSP.getConstraints(); //getting the constraints
		setVars(); //sets the variables in with its domain.
		//the varOrder restriction is not applied in the setVars method as it is only for setting the variables
		
		start = System.currentTimeMillis();
		forwardChecking(unallocatedVars);
	}
	
	
	/**
	 * Method to populate the unallocated list with all the variables and varDomains.
	 * Hash Map with the corresponding domains of each variable.
	 */	
	private void setVars() {
		for(int i = 0; i < binaryCSP.getNoVariables(); i++) {
			int variable = i;
			ArrayList<Integer> domain = new ArrayList<>();
			for(int j = binaryCSP.getLB(i); j < binaryCSP.getUB(i)+1; j++) { //creating the domain list.
				domain.add(j);
			}
			//after the domain is filled, variable is added to the unallocated var list and is placed in the hashmap of variable and domain.
			unallocatedVars.add(i);
			varDomains.put(variable, domain);
		}
	}
	
	/**
	 * Method that implements forward checking.
	 * @param varList The list of variables required for FC.
	 */	
	private void forwardChecking(ArrayList<Integer> varList) {
		
		numberOfNodes++;
		
		if(allocatedVars.size() == binaryCSP.getNoVariables() && !solutionFound) {	//Print the first solution found.

			System.out.println(numberOfNodes); //printing the number of nodes is printed first.
			System.out.println(arcRevisions); //the number of arcs revised is printed second.
			long finish = System.currentTimeMillis();
			long timeElapsed = finish - start;

			//System.out.println(timeElapsed); //time taken to solve third
			//System.out.println();
			printSolution(); //finally printing the values of each variable serparately.
			solutionFound = true;
		}
		else if(!solutionFound) {	//If a solution was not found, we proceed.
			int var = selectVar(varList, varOrder);	//Selecting a variable according to the varOrder requested.
			int val = selectVal(var);	//Select a value from the variable's domain(always in ascending order).	
			
			branchFCLeft(varList, var, val);	//Branching left.
			branchFCRight(varList, var, val);	//After branching left returns, branch right.
		}
	}
	
	
	/**
	 * Branching left.
	 * @param varList variable list
	 * @param var current var
	 * @param val current val assigned to the variable
	 */
	private void branchFCLeft(ArrayList<Integer> varList, int var, int val) {
		
		HashMap<Integer, ArrayList<Integer>> prevDomains = new HashMap<Integer, ArrayList<Integer>>(varDomains); //clone of varDomains
		assign(var, val); 	//Assign the variable.
		
		
		if(reviseFutureArcs(varList, var)) {	//If consistent.
			varList = getVarList(); 	//Re-create varList.
			forwardChecking(varList);
		}
		
		varDomains = prevDomains; 	//Undoing pruning.
		unassign(var, val);		//Unassigning variable.
	}

	
	/**
	 * Branching right.
	 * @param varList variable list in use
	 * @param var the current variable
	 * @param val the current value of the current variable
	 */
	private void branchFCRight(ArrayList<Integer> varList, int var, int val) {
		
		deleteValue(var, val);
		
		if(!varDomains.get(var).isEmpty()) {
			HashMap<Integer, ArrayList<Integer>> prevDomains = new HashMap<Integer, ArrayList<Integer>>(varDomains);	//Saving domains to undo the pruning.
			if(reviseFutureArcs(varList, var)) {
				forwardChecking(varList);
			}
			varDomains = prevDomains;	//Undoing pruning.
		}
		
		restoreValue(var, val);
	}
	
	
	/**
	 * Revises if future arcs are consistent.
	 * @param varList the set of variables
	 * @param var the current variable
	 * @return true if consistent, otherwise return false.
	 */
	private boolean reviseFutureArcs(ArrayList<Integer> varList, int var) {
	
		boolean consistent = true;
		
		for(int futureVar : varList) {
			if(futureVar != var) {
				try {
					consistent = revise(var, futureVar);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(!consistent)
					return false;
			}
		}

		return true;
	}
	
	
	/**
	 * Prunes domain of arc(var, futureVar).
	 * @param var Current variable
	 * @param futureVar The variable connected to the current variable that is to be pruned.
	 * @return false if there where no changes, otherwise return true.
	 * @throws Exception when the pruned domain is empty.
	 */
	private boolean revise(int var, int futureVar) throws Exception {
		arcRevisions++;

		ArrayList<Integer> acceptableValues = getUnConstraintValues(var, futureVar); 	//Values from supported tuples for arc(var, futureVar).
		if(!acceptableValues.isEmpty()) {
		ArrayList<Integer> newDomain = new ArrayList<Integer>(varDomains.get(futureVar));	//Get domain.
		for(int i = 0; i < varDomains.get(futureVar).size(); i++) {
			if(!acceptableValues.contains(varDomains.get(futureVar).get(i))) {	//If domain contains an unsupported value.
				newDomain.remove(newDomain.indexOf(varDomains.get(futureVar).get(i)));	//Prune unsupported value.
			}
		}
		
		if(newDomain.isEmpty())	{  //If the new pruned domain is empty.
			return false;
		}
		else {	
		varDomains.put(futureVar, newDomain);	//Replace the domain with the new pruned domain.
        return true;
		}
	} else {
		return true;
	}
}
	
	
	/**
	 * Method to get the values that where supported by the tuples of c(var, futureVar).
	 * @param var current variable of the node
	 * @param futureVar The future variables linked.
	 * @return an ArrayList with the supported values.
	 */
	private ArrayList<Integer> getUnConstraintValues(int var, int futureVar) {
		
		ArrayList<Integer> acceptableValues = new ArrayList<>();
		
		for(BinaryConstraint c : constraints) {
			if(c.getFirstVar() == var && c.getSecondVar() == futureVar) {
				for(BinaryTuple bt : c.getTuples()) {
					for(int val : varDomains.get(var)) {
						if(bt.getVal1() == val) {
							acceptableValues.add(bt.getVal2());
						}
					}
				}
			}
		}
		return acceptableValues;
	}
	
	
	/**
	 * Method to get a varList with the variables that have not been assigned yet.
	 * @return populated ArrayList varList
	 */
	private ArrayList<Integer> getVarList() {
		ArrayList<Integer> varList = new ArrayList<>();
		for(int i = 0; i < binaryCSP.getNoVariables(); i++) {
			if(!allocatedVars.contains(i)) { //If variable i has not been assigned yet.
				varList.add(i); //Add the variable to the list.
			}
		}
		return varList;
	}
	
	
	/**
	 * Unassigns a variable by removing the variable of the assigned list.
	 * @param var variable whose value is to be unassigned.
	 * @param val Value to be unassigned
	 */
	private void unassign(int var, int val) {
		allocatedVars.remove(allocatedVars.indexOf(var));
	}
	
	
	/**
	 * Assigns a variable by adding it to the assigned list, and prunes all 
	 * of the values from the domain apart from the assigned value (Domain will only contain val).
	 * @param var Variable to which value has to be assigned.
	 * @param val value that has to be assigned.
	 */
	private void assign(int var, int val) {
		allocatedVars.add(var);
		ArrayList<Integer> domain = new ArrayList<>();
		domain.add(val);		
		varDomains.put(var, domain);
	}
	
	
	/**
	 * Re-introduces the value in the domain of the specified variable. 
	 * @param var Variable whose values has to be restored.
	 * @param val Value to be re-assigned.
	 */
	private void restoreValue(int var, int val) {
		ArrayList<Integer> domain = varDomains.get(var);
		domain.add(val);
		varDomains.put(var, domain);
	}
	
	
	/**
	 * Deletes a value from the domain of the specified variable.
	 * @param var Variable whose domain value has to e removed.
	 * @param val Value that has to be removed.
	 */
	private void deleteValue(int var, int val) {
		ArrayList<Integer> domain = varDomains.get(var);
		domain.remove(domain.indexOf(val));
		varDomains.put(var, domain);
	}
	
	
	/**
	 * Selects the first value from the domain of the specified variable, after the domain
	 * is ordered in ascending order.
	 * @param var variable whose value is to be 
	 * @return val selected.
	 */
	private int selectVal(int var) {
		ArrayList<Integer> domain = varDomains.get(var);
		domain.sort(null); //Sorts domain in ascending order.
		return domain.get(0);
	}
	
	
	/**
	 * Selects a variable from the varList. At the beginning, return the first variable. 
	 * Else, return the variable with the smallest domain.
	 * @param varList The current list of variables
	 * @return variable selected variable according to the variable order required.
	 */
	private int selectVar(ArrayList<Integer> varList, String varOrder) {
		int variable = varList.get(0);
		if(varOrder.equals("sdf")) {
		int smallestDomain = varDomains.get(variable).size();
		if(varList.size() == binaryCSP.getNoVariables()) //If at the beginning
			return variable;
		else {
			for(int i = 0; i < varList.size(); i++) {
				if(varDomains.get(varList.get(i)).size() < smallestDomain) { 
					variable = varList.get(i); //Keeps track of the variable with the smallest domain.
					smallestDomain = varDomains.get(varList.get(i)).size(); //Keeps track of the size of the smallest domain encountered.
				}
			}
		}
		return variable;
	} else {
		return variable;
	}
	}
	
	/**
	 * Prints the solution.
	 */
	private void printSolution() {
		for(int i = 0; i < varDomains.size(); i++) {
		    System.out.println(varDomains.get(i).get(0));
		}		
	}
}
	









































