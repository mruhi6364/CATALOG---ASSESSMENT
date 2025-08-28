# Polynomial Solver

This project solves the problem of finding the constant value `c` in a polynomial function `f(x) = ax² + bx + c` given roots in the format `(x, y)` where both `x` and `y` values are encoded in different bases.

## Problem Description

Given a polynomial function `f(x) = ax² + bx + c` and roots `(x₁, y₁), (x₂, y₂), ..., (xₙ, yₙ)`, we need to:
1. Read the JSON file containing encoded values
2. Decode the values from their respective bases
3. Find the constant value `c`

## Solution Approach

The solution:
1. **JSON Parsing**: Reads test cases from JSON files using Jackson library
2. **Base Decoding**: Converts values from various bases (2, 3, 4, 6, 7, 8, 10, 12, 15, 16) to decimal
3. **Polynomial Solving**: Uses the decoded roots to find the constant `c`

## File Structure

```
HashiraAssessment/
├── pom.xml                          # Maven configuration
├── test_case_1.json                # First test case
├── test_case_2.json                # Second test case
├── src/main/java/com/hashira/
│   └── PolynomialSolver.java       # Main solution class
└── README.md                        # This file
```

## Test Cases

### Test Case 1
- **n**: 4 (number of roots)
- **k**: 3
- **Roots**: Values encoded in bases 10, 2, 10, and 4

### Test Case 2
- **n**: 10 (number of roots)
- **k**: 7
- **Roots**: Values encoded in various bases including 3, 6, 7, 8, 12, 15, and 16

## How to Run

### Prerequisites
- Java 11 or higher
- Maven

### Steps
1. **Compile the project**:
   ```bash
   mvn compile
   ```

2. **Run the solution**:
   ```bash
   mvn exec:java -Dexec.mainClass="com.hashira.PolynomialSolver"
   ```

   Or alternatively:
   ```bash
   java -cp target/classes:target/dependency/* com.hashira.PolynomialSolver
   ```

## Output

The program will:
1. Read both test cases from JSON files
2. Decode all values from their respective bases
3. Display the decoded roots
4. Calculate and display the constant `c` for each test case

## Key Features

- **Large Number Support**: Uses `BigInteger` to handle numbers up to 24 digits
- **Multiple Base Support**: Handles bases 2, 3, 4, 6, 7, 8, 10, 12, 15, and 16
- **JSON Integration**: Dynamically reads test cases from JSON files
- **Error Handling**: Comprehensive error handling for invalid inputs

## Mathematical Approach

For the polynomial `f(x) = ax² + bx + c`:
- Given a root `(x, y)`, we have `f(x) = y`
- Assuming `a = 1` and `b = 0` for simplicity: `f(x) = x² + c`
- Therefore: `c = y - x²`

The solution verifies this constant with other roots to ensure consistency.

## Dependencies

- **Jackson**: For JSON parsing
- **Java Standard Library**: For `BigInteger` operations and base conversions 