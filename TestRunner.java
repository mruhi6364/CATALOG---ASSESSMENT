package com.hashira;

import java.io.File;
import java.util.Scanner;

public class TestRunner {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Polynomial Solver Test Runner ===");
        System.out.println("Available test cases:");
        System.out.println("1. test_case_1.json");
        System.out.println("2. test_case_2.json");
        System.out.println("3. Custom JSON file");
        
        System.out.print("\nEnter your choice (1-3): ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline
        
        try {
            String filename;
            switch (choice) {
                case 1:
                    filename = "test_case_1.json";
                    break;
                case 2:
                    filename = "test_case_2.json";
                    break;
                case 3:
                    System.out.print("Enter custom JSON filename: ");
                    filename = scanner.nextLine();
                    break;
                default:
                    System.out.println("Invalid choice. Using test_case_1.json");
                    filename = "test_case_1.json";
            }
            
            // Check if file exists
            File file = new File(filename);
            if (!file.exists()) {
                System.out.println("Error: File " + filename + " not found!");
                return;
            }
            
            System.out.println("\n=== Processing " + filename + " ===");
            PolynomialSolver.ProcessResult result = PolynomialSolver.processTestCase(filename);
            
            System.out.println("\n=== Results ===");
            System.out.println("Number of roots (n): " + result.getN());
            System.out.println("Parameter k: " + result.getK());
            System.out.println("Decoded roots: " + result.getRoots());
            System.out.println("Constant c: " + result.getConstantC());
            
        } catch (Exception e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
} 