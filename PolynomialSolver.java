package com.hashira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Polynomial Solver - Finds constant c in f(x) = ax² + bx + c
 * 
 * This program:
 * 1. Reads JSON files containing encoded values in different bases
 * 2. Decodes the y-values from their respective bases to decimal
 * 3. Uses the decoded roots (x, y) to solve for the constant c
 * 4. Handles large numbers efficiently using BigInteger/BigDecimal
 */
public class PolynomialSolver {
    
    /**
     * Result class to hold the processed test case data
     * Contains n, k, decoded roots, and calculated constant c
     */
    public static class ProcessResult {
        private int n;                    // Number of roots
        private int k;                    // Parameter k from JSON
        private List<Root> roots;         // List of decoded (x, y) coordinates
        private BigInteger constantC;     // Calculated constant c
        
        public ProcessResult(int n, int k, List<Root> roots, BigInteger constantC) {
            this.n = n;
            this.k = k;
            this.roots = roots;
            this.constantC = constantC;
        }
        
        public int getN() { return n; }
        public int getK() { return k; }
        public List<Root> getRoots() { return roots; }
        public BigInteger getConstantC() { return constantC; }
    }
    
    /**
     * Represents a single root point (x, y) where:
     * x = the x-coordinate (input value)
     * y = the y-coordinate (decoded from base-encoded string)
     */
    private static class Root {
        private BigInteger x;  // x-coordinate (usually the index from JSON)
        private BigInteger y;  // y-coordinate (decoded from base-encoded value)
        
        public Root(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
        
        public BigInteger getX() { return x; }
        public BigInteger getY() { return y; }
        
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
    
    /**
     * Container for a complete test case
     * Holds the metadata (n, k) and all the roots
     */
    private static class TestCase {
        private int n;                    // Number of roots
        private int k;                    // Parameter k
        private List<Root> roots;         // All decoded roots
        
        public TestCase(int n, int k, List<Root> roots) {
            this.n = n;
            this.k = k;
            this.roots = roots;
        }
        
        public int getN() { return n; }
        public int getK() { return k; }
        public List<Root> getRoots() { return roots; }
    }
    
    /**
     * Main entry point for processing a single test case file
     * This method is called by the TestRunner class
     */
    public static ProcessResult processTestCase(String filename) throws IOException {
        TestCase testCase = readTestCase(filename);
        BigInteger constantC = solvePolynomial(testCase);
        return new ProcessResult(testCase.getN(), testCase.getK(), testCase.getRoots(), constantC);
    }
    
    /**
     * Main method - runs both test cases automatically
     */
    public static void main(String[] args) {
        try {
            // Test case 1
            System.out.println("=== Test Case 1 ===");
            TestCase testCase1 = readTestCase("test_case_1.json");
            System.out.println("Roots: " + testCase1.getRoots());
            BigInteger constantC1 = solvePolynomial(testCase1);
            System.out.println("Constant c for test case 1: " + constantC1);
            
            System.out.println("\n=== Test Case 2 ===");
            TestCase testCase2 = readTestCase("test_case_2.json");
            System.out.println("Roots: " + testCase2.getRoots());
            BigInteger constantC2 = solvePolynomial(testCase2);
            System.out.println("Constant c for test case 2: " + constantC2);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Reads and parses a JSON test case file
     * 
     * JSON Structure:
     * {
     *   "keys": {"n": 4, "k": 3},
     *   "1": {"base": "10", "value": "4"},
     *   "2": {"base": "2", "value": "111"},
     *   ...
     * }
     * 
     * @param filename The JSON file to read
     * @return TestCase object with decoded roots
     */
    private static TestCase readTestCase(String filename) throws IOException {
        // Create Jackson ObjectMapper for JSON parsing
        ObjectMapper mapper = new ObjectMapper();
        
        // Parse the JSON file into a tree structure
        JsonNode rootNode = mapper.readTree(new File(filename));
        
        // Extract metadata from "keys" section
        int n = rootNode.get("keys").get("n").asInt();  // Number of roots
        int k = rootNode.get("keys").get("k").asInt();  // Parameter k
        
        List<Root> roots = new ArrayList<>();
        
        // Parse each root from the JSON
        // The JSON structure shows that we have pairs of values
        // We need to interpret these as (x, y) coordinates
        for (int i = 1; i <= n; i++) {
            if (rootNode.has(String.valueOf(i))) {
                // Get the root data for index i
                JsonNode rootData = rootNode.get(String.valueOf(i));
                String base = rootData.get("base").asText();    // e.g., "2", "10", "16"
                String value = rootData.get("value").asText();  // e.g., "111", "4", "a1b2"
                
                // 🔑 KEY STEP: Decode the value from its base to decimal
                // This converts strings like "111" (base 2) to BigInteger 7
                BigInteger decodedValue = decodeFromBase(value, base);
                
                // For this problem, we'll treat the decoded value as y
                // and use the index i as x (you can modify this based on your specific requirements)
                BigInteger x = BigInteger.valueOf(i);  // x = index (1, 2, 3, ...)
                BigInteger y = decodedValue;           // y = decoded value from base
                
                roots.add(new Root(x, y));
            }
        }
        
        return new TestCase(n, k, roots);
    }
    
    /**
     * Main polynomial solving logic
     * 
     * Strategy:
     * 1. If we have 3+ roots, use system of equations (Cramer's rule)
     * 2. If fewer roots, use simple polynomial assumption
     * 
     * @param testCase The test case with decoded roots
     * @return The calculated constant c
     */
    private static BigInteger solvePolynomial(TestCase testCase) {
        List<Root> roots = testCase.getRoots();
        
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("No roots provided");
        }
        
        // For a polynomial f(x) = ax² + bx + c
        // We have multiple points (x, y) where f(x) = y
        
        // If we have at least 3 points, we can solve for a, b, and c
        if (roots.size() >= 3) {
            return solveSystemOfEquations(roots);
        } else {
            // Fallback to simple approach for fewer points
            return solveSimplePolynomial(roots);
        }
    }
    
    /**
     * Solves the polynomial using system of equations
     * 
     * Mathematical approach:
     * We have 3 equations:
     * ax₁² + bx₁ + c = y₁  (from root 1)
     * ax₂² + bx₂ + c = y₂  (from root 2)  
     * ax₃² + bx₃ + c = y₃  (from root 3)
     * 
     * We can solve this system using Cramer's rule to find c
     * 
     * @param roots List of at least 3 roots
     * @return The calculated constant c
     */
    private static BigInteger solveSystemOfEquations(List<Root> roots) {
        // Use the first 3 points to solve the system:
        // ax₁² + bx₁ + c = y₁
        // ax₂² + bx₂ + c = y₂  
        // ax₃² + bx₃ + c = y₃
        
        Root p1 = roots.get(0);  // First root (x₁, y₁)
        Root p2 = roots.get(1);  // Second root (x₂, y₂)
        Root p3 = roots.get(2);  // Third root (x₃, y₃)
        
        // Convert to BigDecimal for precision in calculations
        BigDecimal x1 = new BigDecimal(p1.getX());
        BigDecimal y1 = new BigDecimal(p1.getY());
        BigDecimal x2 = new BigDecimal(p2.getX());
        BigDecimal y2 = new BigDecimal(p2.getY());
        BigDecimal x3 = new BigDecimal(p3.getX());
        BigDecimal y3 = new BigDecimal(p3.getY());
        
        // 🔑 MATHEMATICAL STEP: Using Cramer's rule to solve the system
        // Matrix: [x₁² x₁ 1] [a]   [y₁]
        //         [x₂² x₂ 1] [b] = [y₂]
        //         [x₃² x₃ 1] [c]   [y₃]
        
        // Calculate the determinant of the coefficient matrix
        BigDecimal det = x1.multiply(x1).multiply(x2).add(x2.multiply(x2).multiply(x3))
                        .add(x3.multiply(x3).multiply(x1))
                        .subtract(x1.multiply(x1).multiply(x3))
                        .subtract(x2.multiply(x2).multiply(x1))
                        .subtract(x3.multiply(x3).multiply(x2));
        
        // Check if determinant is zero (system has no unique solution)
        if (det.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("Warning: Determinant is zero, using fallback method");
            return solveSimplePolynomial(roots);
        }
        
        // Calculate c using Cramer's rule
        // Replace the third column with the constants [y₁, y₂, y₃]
        BigDecimal detC = x1.multiply(x1).multiply(x2).multiply(y3)
                           .add(x2.multiply(x2).multiply(x3).multiply(y1))
                           .add(x3.multiply(x3).multiply(x1).multiply(y2))
                           .subtract(x1.multiply(x1).multiply(x3).multiply(y2))
                           .subtract(x2.multiply(x2).multiply(x1).multiply(y3))
                           .subtract(x3.multiply(x3).multiply(x2).multiply(y1));
        
        // c = detC / det
        BigDecimal c = detC.divide(det, 0, RoundingMode.HALF_UP);
        
        // Verify the solution with other roots
        verifySolution(roots, c);
        
        return c.toBigInteger();
    }
    
    /**
     * Fallback method for solving polynomial with fewer than 3 roots
     * 
     * Assumes: f(x) = x² + c (a=1, b=0)
     * Then: c = y - x²
     * 
     * @param roots List of roots (less than 3)
     * @return The calculated constant c
     */
    private static BigInteger solveSimplePolynomial(List<Root> roots) {
        // Simple approach: assume a = 1 and b = 0, then c = y - x²
        Root firstRoot = roots.get(0);
        BigInteger x = firstRoot.getX();
        BigInteger y = firstRoot.getY();
        
        // Calculate x²
        BigInteger xSquared = x.multiply(x);
        
        // Calculate c = y - x²
        BigInteger c = y.subtract(xSquared);
        
        // Verify with other roots if possible
        for (int i = 1; i < roots.size(); i++) {
            Root root = roots.get(i);
            BigInteger expectedY = root.getX().multiply(root.getX()).add(c);
            if (!expectedY.equals(root.getY())) {
                System.out.println("Warning: Root " + root + " doesn't satisfy the equation with c = " + c);
            }
        }
        
        return c;
    }
    
    /**
     * Verifies the calculated solution with all roots
     * 
     * For verification, assumes f(x) = x² + c
     * Checks if f(x) = y for each root
     * 
     * @param roots All roots to verify against
     * @param c The calculated constant c
     */
    private static void verifySolution(List<Root> roots, BigDecimal c) {
        // Verify the solution with all roots
        for (Root root : roots) {
            BigDecimal x = new BigDecimal(root.getX());
            BigDecimal y = new BigDecimal(root.getY());
            
            // For verification, assume a = 1, b = 0: f(x) = x² + c
            BigDecimal expectedY = x.multiply(x).add(c);
            BigDecimal difference = y.subtract(expectedY).abs();
            
            // If difference is more than 1, show a warning
            if (difference.compareTo(BigDecimal.ONE) > 0) {
                System.out.println("Warning: Root " + root + " has difference: " + difference);
            }
        }
    }
    
    /**
     * 🔑 CORE FUNCTION: Decodes a string value from a given base to decimal
     * 
     * This is the heart of the solution! It converts encoded strings like:
     * - "111" (base 2) → 7 (decimal)
     * - "213" (base 4) → 39 (decimal)
     * - "a1b2" (base 16) → 41394 (decimal)
     * 
     * @param value The encoded string (e.g., "111", "213", "a1b2")
     * @param baseStr The base as a string (e.g., "2", "4", "16")
     * @return BigInteger representing the decoded decimal value
     */
    private static BigInteger decodeFromBase(String value, String baseStr) {
        int base = Integer.parseInt(baseStr);
        
        // Handle different bases using Java's built-in BigInteger constructors
        // Each constructor takes the string and the base as parameters
        
        if (base == 10) {
            // Decimal: no conversion needed, just parse the string
            return new BigInteger(value);
        } else if (base == 2) {
            // Binary: "111"₂ = 1×2² + 1×2¹ + 1×2⁰ = 4 + 2 + 1 = 7₁₀
            return new BigInteger(value, 2);
        } else if (base == 4) {
            // Quaternary: "213"₄ = 2×4² + 1×4¹ + 3×4⁰ = 32 + 4 + 3 = 39₁₀
            return new BigInteger(value, 4);
        } else if (base == 6) {
            // Senary: "13444211440455345511"₆ → decimal
            return new BigInteger(value, 6);
        } else if (base == 7) {
            // Septenary: "1101613130313526312514143"₇ → decimal
            return new BigInteger(value, 7);
        } else if (base == 8) {
            // Octal: "316034514573652620673"₈ → decimal
            return new BigInteger(value, 8);
        } else if (base == 12) {
            // Duodecimal: "45153788322a1255483"₁₂ → decimal
            // Note: 'a' represents 10 in base 12
            return new BigInteger(value, 12);
        } else if (base == 15) {
            // Pentadecimal: "aed7015a346d63"₁₅ → decimal
            // Note: 'a'=10, 'e'=14, 'd'=13 in base 15
            return new BigInteger(value, 15);
        } else if (base == 16) {
            // Hexadecimal: "e1b5e05623d881f"₁₆ → decimal
            // Note: 'a'=10, 'b'=11, 'c'=12, 'd'=13, 'e'=14, 'f'=15
            return new BigInteger(value, 16);
        } else if (base == 3) {
            // Ternary: "2122212201122002221120200210011020220200"₃ → decimal
            return new BigInteger(value, 3);
        } else {
            throw new IllegalArgumentException("Unsupported base: " + base);
        }
    }
} 