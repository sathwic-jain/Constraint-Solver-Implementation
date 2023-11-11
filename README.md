# Constraint-Solver-Implementation

## Overview

This Java program implements two constraint satisfaction problem-solving algorithms: Forward Checking (FC) and Maintaining Arc Consistency (MAC). The provided functionalities are encapsulated in separate Java files: `FC.java` for Forward Checking and `MAC.java` for MAC. The main program, `Main.java`, facilitates user input and execution.

## Getting Started

### Prerequisites

- Java Runtime Environment (JRE) installed

### Usage

Execute the JAR file with the following command:

```bash
java -jar P2.jar <filename.csp> <fc|mac> <asc|sdf>
```

- `<filename.csp>`: Path to the CSP file
- `<fc|mac>`: Choose between Forward Checking (fc) or Maintaining Arc Consistency (mac)
- `<asc|sdf>`: (Optional) Choose variable selection order: ascending (asc) or smallest-domain-first (sdf)

### Example

```bash
java -jar P2.jar example.csp fc asc
```

## Model

### Forward Checking

#### Data Structures

- `unallocatedVars`: ArrayList of unallocated variables
- `allocatedVars`: ArrayList of allocated variables
- `varDomains`: HashMap to store variable domains

#### Methods

- `setVars()`: Populates unallocatedVars and varDomains
- `forwardChecking(ArrayList<Integer> varList)`: Implements the Forward Checking algorithm
- `branchFCLeft(ArrayList<Integer> varList, int var, int val)`: Branches left in the search space
- `branchFCRight(ArrayList<Integer> varList, int var, int val)`: Branches right in the search space
- `reviseFutureArcs(ArrayList<Integer> varList, int var)`: Checks consistency of future arcs
- `revise(int var, int futureVar)`: Prunes domain of arc(var, futureVar)
- ... (Other utility methods)

### Maintaining Arc Consistency (MAC)

#### Additional Data Structure

- `queue`: ArrayList of Integer arrays used as a queue for arcs

#### Methods

- `MAC3(ArrayList<Integer> varList)`: Implements the MAC algorithm
- `AC3(int var)`: Main AC3 procedure
- `revise(int var, int futureVar)`: Prunes domain of arc(var, futureVar)

## Examples

Example usage and output for both FC and MAC.

## Acknowledgments

- This project was developed for the CS4402 course at University of St. Andrews.
- Thanks to the course instructors and contributors.
