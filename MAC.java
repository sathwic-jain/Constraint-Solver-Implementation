/*
 * 180008512
 */

import java.util.ArrayList;
import java.util.HashMap;

public class MAC {
	
	private BinaryCSPReader reader = new BinaryCSPReader();
	private BinaryCSP binaryCSP;
	private ArrayList<BinaryConstraint> constraints;
	
	private ArrayList<Integer> unallocatedVars = new ArrayList(); //List of unallocatedVars variables (varList).
	private ArrayList<Integer> allocated = new ArrayList<>(); //List of allocated variables.
	private HashMap<Integer, ArrayList<Integer>> varDomains = new HashMap<>(); //Variables domain.
	
	private int numberOfNodes = 0; //Count the number of MAC procedures.
	protected long arcRevisions = 0;     //Number of arcs traversed.
	private boolean solutionFound = false; //Checks if a solution was found (Used to stop the algorithm).
	private String varOrder;
	
	private long start; //Used to measure the time taken to find a solution.
	
	public MAC(String[] args) {

		String instance = args[0];
		varOrder = args[2];
		binaryCSP = reader.readBinaryCSP(instance);
		constraints = binaryCSP.getConstraints(); //getting the constraints
		setVars(); //sets the variables in with its domain.
		//the varOrder restriction is not applied in the setVars method as it is only for setting the variables

		start = System.currentTimeMillis();
		MAC3(unallocatedVars);
		varOrder = args[2];
	}
	
	/**
	 * Method to populate the unallocatedVars list with all the variables and varDomains Hash Map
	 * with the corresponding varDomains of each variable.
	 */
	private void setVars() {
		for(int i = 0; i < binaryCSP.getNoVariables(); i++) {
			int variable = i;
			ArrayList<Integer> domain = new ArrayList<>();
			for(int j = binaryCSP.getLB(i); j < binaryCSP.getUB(i)+1; j++) {
				domain.add(j);
			}
			unallocatedVars.add(i);
			varDomains.put(variable, domain);
		}
	}
	
			
	private void MAC3(ArrayList<Integer> varList) {
		
		numberOfNodes++;
		
		int var = selectVar(varList, varOrder);	//Select a variable.
		int val = selectVal(var);	//Select a value from the variable's domain.		
		
		
		HashMap<Integer, ArrayList<Integer>> prevvarDomains = (HashMap<Integer, ArrayList<Integer>>) varDomains.clone(); //Saving varDomains to undo the pruning.

		assign(var, val);	
		
		if(allocated.size() == binaryCSP.getNoVariables() && !solutionFound) { //Print the first solution found.

			// System.out.println("\n Solution found after " + numberOfNodes + " nodes");

			long finish = System.currentTimeMillis();
			long timeElapsed = finish - start;
			System.out.println(numberOfNodes);
			System.out.println(arcRevisions);
			System.out.println(timeElapsed);
			System.out.println();
			printSolution();
			solutionFound = true;
		}
		else if(AC3(var) && !solutionFound) { //If a solution was not found, proceed.
			varList = getVarList();
			MAC3(varList);
		}
		
		varDomains = prevvarDomains; //Undoing pruning.
		unassign(var, val); 
		
		deleteValue(var, val);
		prevvarDomains = (HashMap<Integer, ArrayList<Integer>>) varDomains.clone(); 	//Saving varDomains to undo the pruning.

		if(!varDomains.get(var).isEmpty()) {
			if(AC3(var))
				MAC3(varList);
			else
				varDomains = prevvarDomains; //Undoing pruning.
		}

		restoreValue(var, val);		
	}
	
	
	/**
	 * AC3 MAIN PROCEDURE
	 * @param var
	 * @return true if consistent, otherwise return false.
	 */
	private boolean AC3(int var) {
		
		ArrayList<int[]> queue = new ArrayList<>(); //Initialises Queue.
		
		for(BinaryConstraint c : constraints) { //Inputs arcs into the queue.
			if(c.getFirstVar() == var && !allocated.contains(c.getSecondVar())){
				int[] arc = new int[2];
				arc[0] = var;
				arc[1] = c.getSecondVar();
				queue.add(arc);
			}
		}
		
		while(!queue.isEmpty()) { //While queue is not empty.
			int[] arc = queue.get(0); //Select first arc of the queue.
			queue.remove(0); //Removes arc from the queue.
			try {
				if(revise(arc[0], arc[1])) { 
						for(BinaryConstraint c : constraints) {  //Inputs new arcs into the queue.
							if(c.getFirstVar() == arc[1]) {
								int[] newArc = {arc[1], c.getSecondVar()};
								queue.add(newArc);
							}
						}
				}
			}
			catch(Exception e) { //If pruned domain is empty.
				return false;
			}
		}
		
		return true;
	}
	
	
	/**
	 * Prunes domain of arc(var, futureVar)
	 * @param var Current variable
	 * @param futureVar The variable connected to the current variable that is to bre pruned
	 * @return false if there where no changes, otherwise return true.
	 * @throws Exception when the pruned domain is empty.
	 */
	private boolean revise(int var, int futureVar) throws Exception{
		arcRevisions++;
        boolean changed = false;
        ArrayList<Integer> acceptableValues = getUnConstraintValues(var, futureVar);    //Values from supported tuples for arc(var, futureVar).
        if (acceptableValues.size() >= 1) { // enter the loop only if there is a connection between assigned var and futureVar in constraints
            //ArrayList<Integer> newDomain = (ArrayList<Integer>) varDomains.get(futureVar).clone();    //Get domain.
            ArrayList<Integer> newDomain = new ArrayList<Integer>(varDomains.get(futureVar));
			for (int i = 0; i < varDomains.get(futureVar).size(); i++) {
                if (!acceptableValues.contains(varDomains.get(futureVar).get(i))) {    //If domain contains an unsupported value.
                    newDomain.remove(newDomain.indexOf(varDomains.get(futureVar).get(i)));    //Prune unsupported value.
                    changed = true;
                }
            }
            if (newDomain.isEmpty()) {
                //If the new pruned domain is empty.
                throw new Exception();
            } else {
                varDomains.put(futureVar, newDomain);    //Replace the domain with the new pruned domain.
            }
        }
        return changed;
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
	 * Method to get a varList with the variables that have not been allocated yet.
	 * @return populated ArrayList varList
	 */
	private ArrayList<Integer> getVarList() {
		ArrayList<Integer> varList = new ArrayList<>();
		for(int i = 0; i < binaryCSP.getNoVariables(); i++) {
			if(!allocated.contains(i)) { //If variable i has not been allocated yet.
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
		allocated.remove(allocated.indexOf(var));
	}
	
	
	/**
	 * Assigns a variable by adding it to the assigned list, and prunes all 
	 * of the values from the domain apart from the assigned value (Domain will only contain val).
	 * @param var variable to which value to assigned.
	 * @param val  value that has to be assigned.
	 */
	private void assign(int var, int val) {
		allocated.add(var);
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
	 * @param var Variable whose value is to be selected
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
		if (this.varOrder.equals("asc")) { // checks for ascending order
            return varList.get(0); // sends first variable as the varList is already in ascending order
        } else if (this.varOrder.equals("sdf")) {
		int variable = varList.get(0);
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
	}
		return -1; //dummy return to satisfy the return statement
	
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
	










































