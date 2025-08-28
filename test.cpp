#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <stdexcept>
#include <algorithm>
#include <cmath>
#include <iomanip>
#include <sstream>
#include <map>
#include <regex>

// Using standard types - no external dependencies required
using BigInt = long long;
using BigFloat = long double;

/**
 * Simple JSON Parser for our specific use case
 * Parses the JSON structure used in test cases without external dependencies
 */
class SimpleJsonParser {
public:
    /**
     * Parses a JSON file and extracts the required data
     * Returns a map with keys like "n", "k", "base_1", "value_1", etc.
     */
    static std::map<std::string, std::string> parseTestCase(const std::string& filename) {
        std::ifstream file(filename);
        if (!file.is_open()) {
            throw std::runtime_error("Cannot open file: " + filename);
        }
        
        // Read entire file content
        std::string content((std::istreambuf_iterator<char>(file)),
                           std::istreambuf_iterator<char>());
        file.close();
        
        std::map<std::string, std::string> result;
        
        // Remove all whitespace and newlines for easier parsing
        content.erase(std::remove_if(content.begin(), content.end(), ::isspace), content.end());
        
        try {
            // Parse keys section: "keys":{"n":4,"k":3}
            std::regex keysRegex("\"keys\":\\{\"n\":(\\d+),\"k\":(\\d+)\\}");
            std::smatch keysMatch;
            if (std::regex_search(content, keysMatch, keysRegex)) {
                result["n"] = keysMatch[1].str();
                result["k"] = keysMatch[2].str();
            }
            
            // Parse data entries: "1":{"base":"10","value":"4"}
            std::regex entryRegex("\"(\\d+)\":\\{\"base\":\"(\\d+)\",\"value\":\"([^\"]+)\"\\}");
            std::sregex_iterator iter(content.begin(), content.end(), entryRegex);
            std::sregex_iterator end;
            
            for (; iter != end; ++iter) {
                std::string index = (*iter)[1].str();
                std::string base = (*iter)[2].str();
                std::string value = (*iter)[3].str();
                result["base_" + index] = base;
                result["value_" + index] = value;
            }
            
        } catch (const std::exception& e) {
            throw std::runtime_error("JSON parsing failed: " + std::string(e.what()));
        }
        
        return result;
    }
};

/**
 * Polynomial Solver - Finds constant c using Lagrange interpolation
 * 
 * This program:
 * 1. Reads JSON files containing encoded values in different bases
 * 2. Decodes the y-values from their respective bases to decimal
 * 3. Uses Lagrange interpolation to find the constant term at x=0
 * 4. Uses standard C++ types (supports numbers up to ~9 × 10^18)
 */
class PolynomialSolver {
private:
    /**
     * Represents a single root point (x, y) where:
     * x = the x-coordinate (input value)
     * y = the y-coordinate (decoded from base-encoded string)
     */
    struct Root {
        BigInt x; // x-coordinate (usually the index from JSON)
        BigInt y; // y-coordinate (decoded from base-encoded value)
        
        Root(BigInt x_val, BigInt y_val) : x(x_val), y(y_val) {}
        
        std::string toString() const {
            return "(" + std::to_string(x) + ", " + std::to_string(y) + ")";
        }
    };
    
    /**
     * Container for a complete test case
     * Holds the metadata (n, k) and all the roots
     */
    struct TestCase {
        int n;                    // Number of roots
        int k;                    // Parameter k
        std::vector<Root> roots;  // All decoded roots
        
        TestCase(int n_val, int k_val, const std::vector<Root>& roots_val) 
            : n(n_val), k(k_val), roots(roots_val) {}
    };

public:
    /**
     * Result class to hold the processed test case data
     * Contains n, k, decoded roots, and calculated constant c
     */
    struct ProcessResult {
        int n;                    // Number of roots
        int k;                    // Parameter k from JSON
        std::vector<Root> roots;  // List of decoded (x, y) coordinates
        BigInt constantC;         // Calculated constant c
        
        ProcessResult(int n_val, int k_val, const std::vector<Root>& roots_val, BigInt constantC_val)
            : n(n_val), k(k_val), roots(roots_val), constantC(constantC_val) {}
    };

    /**
     * Main entry point for processing a single test case file
     */
    static ProcessResult processTestCase(const std::string& filename) {
        TestCase testCase = readTestCase(filename);
        BigInt constantC = solvePolynomial(testCase);
        return ProcessResult(testCase.n, testCase.k, testCase.roots, constantC);
    }

    /**
     * Main method - runs both test cases automatically
     */
    static void runTests() {
        try {
            // Test case 1
            std::cout << "=== Test Case 1 ===" << std::endl;
            TestCase testCase1 = readTestCase("test_case_1.json");
            std::cout << "Found " << testCase1.roots.size() << " roots:" << std::endl;
            for (const auto& root : testCase1.roots) {
                std::cout << "  " << root.toString() << std::endl;
            }
            
            BigInt constantC1 = solvePolynomial(testCase1);
            std::cout << "Constant c for test case 1: " << constantC1 << std::endl;
            
            std::cout << "\n=== Test Case 2 ===" << std::endl;
            TestCase testCase2 = readTestCase("test_case_2.json");
            std::cout << "Found " << testCase2.roots.size() << " roots:" << std::endl;
            for (size_t i = 0; i < std::min(testCase2.roots.size(), size_t(5)); ++i) {
                std::cout << "  " << testCase2.roots[i].toString() << std::endl;
            }
            if (testCase2.roots.size() > 5) {
                std::cout << "  ... and " << (testCase2.roots.size() - 5) << " more roots" << std::endl;
            }
            
            BigInt constantC2 = solvePolynomial(testCase2);
            std::cout << "Constant c for test case 2: " << constantC2 << std::endl;
            
        } catch (const std::exception& e) {
            std::cerr << "Error: " << e.what() << std::endl;
        }
    }

private:
    /**
     * Reads and parses a JSON test case file using simple regex parsing
     * 
     * JSON Structure:
     * {
     *   "keys": {"n": 4, "k": 3},
     *   "1": {"base": "10", "value": "4"},
     *   "2": {"base": "2", "value": "111"},
     *   ...
     * }
     */
    static TestCase readTestCase(const std::string& filename) {
        // Parse JSON using simple parser
        auto jsonData = SimpleJsonParser::parseTestCase(filename);
        
        // Extract metadata from parsed data
        int n = std::stoi(jsonData.at("n"));  // Number of roots
        int k = std::stoi(jsonData.at("k"));  // Parameter k
        
        std::cout << "Parsing test case: n=" << n << ", k=" << k << std::endl;
        
        std::vector<Root> roots;
        
        // Parse each root from the parsed data
        // Note: We need to check all possible indices, not just 1 to n
        // because some test cases might have gaps (like test_case_1.json has index 6)
        for (int i = 1; i <= 20; i++) { // Check up to 20 to catch any gaps
            std::string baseKey = "base_" + std::to_string(i);
            std::string valueKey = "value_" + std::to_string(i);
            
            if (jsonData.find(baseKey) != jsonData.end() && 
                jsonData.find(valueKey) != jsonData.end()) {
                
                std::string base = jsonData.at(baseKey);    // e.g., "2", "10", "16"
                std::string value = jsonData.at(valueKey);  // e.g., "111", "4", "a1b2"
                
                std::cout << "Processing index " << i << ": base=" << base 
                         << ", value=" << value << std::endl;
                
                // 🔑 KEY STEP: Decode the value from its base to decimal
                BigInt decodedValue = decodeFromBase(value, base);
                
                // For this problem, we'll treat the decoded value as y
                // and use the index i as x
                BigInt x = static_cast<BigInt>(i);  // x = index (1, 2, 3, ...)
                BigInt y = decodedValue; // y = decoded value from base
                
                std::cout << "  Decoded: " << value << " (base " << base 
                         << ") = " << y << " (decimal)" << std::endl;
                
                roots.emplace_back(x, y);
            }
        }
        
        std::cout << "Successfully parsed " << roots.size() << " roots" << std::endl;
        return TestCase(n, k, roots);
    }
    
    /**
     * Main polynomial solving logic using Lagrange interpolation
     * 
     * Strategy:
     * Use Lagrange interpolation to find the constant term at x=0
     */
    static BigInt solvePolynomial(const TestCase& testCase) {
        const std::vector<Root>& roots = testCase.roots;
        
        if (roots.empty()) {
            throw std::invalid_argument("No roots provided");
        }
        
        std::cout << "Solving polynomial with " << roots.size() << " roots" << std::endl;
        std::cout << "Using k=" << testCase.k << " points for interpolation" << std::endl;
        
        // Use exactly k points for Lagrange interpolation
        int numPoints = std::min(testCase.k, static_cast<int>(roots.size()));
        
        return lagrangeInterpolationAtZero(roots, numPoints);
    }
    
    /**
     * Uses Lagrange interpolation to find the polynomial value at x=0
     * This gives us the constant term of the polynomial
     */
    static BigInt lagrangeInterpolationAtZero(const std::vector<Root>& roots, int numPoints) {
        BigFloat result = 0.0;
        
        std::cout << "Calculating constant term using " << numPoints << " points:" << std::endl;
        
        for (int i = 0; i < numPoints; i++) {
            BigFloat yi = static_cast<BigFloat>(roots[i].y);
            BigFloat xi = static_cast<BigFloat>(roots[i].x);
            
            // Calculate Li(0) = Π(j≠i) (-xj) / (xi - xj)
            BigFloat lagrangeBasis = 1.0;
            
            for (int j = 0; j < numPoints; j++) {
                if (i != j) {
                    BigFloat xj = static_cast<BigFloat>(roots[j].x);
                    lagrangeBasis *= (-xj) / (xi - xj);
                }
            }
            
            std::cout << "  Point " << roots[i].toString() << " -> basis = " << lagrangeBasis << std::endl;
            
            result += yi * lagrangeBasis;
        }
        
        std::cout << "Final result at x=0: " << result << std::endl;
        
        // Round to nearest integer
        return static_cast<BigInt>(std::round(result));
    }
    
    /**
     * 🔑 CORE FUNCTION: Decodes a string value from a given base to decimal
     * 
     * This is the heart of the solution! It converts encoded strings like:
     * - "111" (base 2) → 7 (decimal)
     * - "213" (base 4) → 39 (decimal)
     * - "a1b2" (base 16) → 41394 (decimal)
     */
    static BigInt decodeFromBase(const std::string& value, const std::string& baseStr) {
        int base = std::stoi(baseStr);
        
        // Convert character to digit value
        auto charToDigit = [](char c) -> int {
            if (c >= '0' && c <= '9') {
                return c - '0';
            } else if (c >= 'a' && c <= 'z') {
                return c - 'a' + 10;
            } else if (c >= 'A' && c <= 'Z') {
                return c - 'A' + 10;
            }
            throw std::invalid_argument("Invalid character in base conversion: " + std::string(1, c));
        };
        
        BigInt result = 0;
        BigInt baseMultiplier = 1;
        
        // Process digits from right to left
        for (int i = static_cast<int>(value.length()) - 1; i >= 0; i--) {
            int digitValue = charToDigit(value[i]);
            
            if (digitValue >= base) {
                throw std::invalid_argument("Digit value " + std::to_string(digitValue) + 
                                          " is invalid for base " + std::to_string(base));
            }
            
            result += static_cast<BigInt>(digitValue) * baseMultiplier;
            baseMultiplier *= base;
        }
        
        return result;
    }
};

// Main function
int main() {
    std::cout << "Polynomial Solver C++ Version (Lagrange Interpolation)" << std::endl;
    std::cout << "=======================================================" << std::endl;
    
    PolynomialSolver::runTests();
    
    return 0;
}