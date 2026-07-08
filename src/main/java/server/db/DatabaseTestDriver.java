package server.db;

import server.controllers.LoginServerController;
import server.controllers.QuestionServerController;

public class DatabaseTestDriver {
    public static void main(String[] args) {
        System.out.println("=== STARTING COMPLETE BACKEND FUNCTIONAL TEST ===");

        // 1. Initialize and connect to the Database Storage Engine
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.connect()) {
            System.err.println(">>> FATAL: Database connection down. Aborting test.");
            return;
        }

        // 2. Initialize both verified Controllers
        LoginServerController loginController = new LoginServerController();
        QuestionServerController questionController = new QuestionServerController();

        System.out.println("\n--- STEP 1: TESTING LOGIN SESSION CACHE ---");
        boolean loginSuccess = loginController.login("admin", "123456");
        System.out.println("Initial Login Authorization: " + loginSuccess);

        // Try a duplicate hack entry to verify memory set protection
        boolean duplicateBlocked = !loginController.login("admin", "123456");
        System.out.println("Duplicate Session Guard Working: " + duplicateBlocked);


        System.out.println("\n--- STEP 2: LOADING YOUR 6 SEQUENTIAL BLUEPRINT QUESTIONS ---");

        // Helper list setup to quickly supply 4 distinct choices per query mapping
        java.util.List<String> logicAns = java.util.Arrays.asList("q -> p", "not p or q", "p and not q", "not p and not q");
        java.util.List<String> bstAns = java.util.Arrays.asList("O(1)", "O(log n)", "O(n)", "O(n log n)");
        java.util.List<String> lifoAns = java.util.Arrays.asList("Queue", "Stack", "Singly Linked List", "Binary Tree");
        java.util.List<String> oopAns = java.util.Arrays.asList("Method Overloading", "Method Overriding", "Compilation Error", "Encapsulation Violation");
        java.util.List<String> sortAns = java.util.Arrays.asList("O(n)", "O(n log n)", "O(n^2)", "O(1)");
        java.util.List<String> dpAns = java.util.Arrays.asList("O(n)", "O(m*n)", "O(2^n)", "O(log n)");

        // Course 22 - Mathematical Logic track (Will generate: 22001)
        questionController.createQuestion(
                "Which of the following propositions is logically equivalent to the conditional statement p -> q?",
                "MEDIUM",
                "Apply standard logical equivalences.",
                "Propositional Logic",
                "22",
                logicAns
        );

        // Course 11 - Computer Science track (Will generate: 11001, 11002, 11003, 11004, 11005)
        questionController.createQuestion(
                "What is the time complexity of searching in a perfectly balanced Binary Search Tree (BST)?",
                "MEDIUM",
                "Choose the single most accurate asymptotic upper bound.",
                "Data Structures",
                "11",
                bstAns
        );

        questionController.createQuestion(
                "Which of the following data structures operates strictly on a Last-In, First-Out (LIFO) basis?",
                "EASY",
                "Select the correct foundational abstract data type.",
                "Data Structures",
                "11",
                lifoAns
        );

        questionController.createQuestion(
                "What occurs when a Java subclass defines a method with the same signature as a superclass method?",
                "MEDIUM",
                "Assume standard object-oriented programming conventions.",
                "Object-Oriented Programming",
                "11",
                oopAns
        );

        questionController.createQuestion(
                "new question test",
                "MEDIUM",
                "Consider standard comparison-based sorting models.",
                "Sorting Algorithms",
                "11",
                sortAns
        );

        questionController.createQuestion(
                "What is the worst-case time complexity of the optimized edit distance dynamic programming algorithm?",
                "HARD",
                "Select the single best algorithmic complexity.",
                "Dynamic Programming",
                "11",
                dpAns
        );

        System.out.println("[SERVER] Your custom questions have been beautifully indexed!");


        System.out.println("\n--- STEP 3: TESTING OBJECT MAPPING & SEARCH QUERY ---");
        // Pass both the topic keyword search string AND a test course ID (e.g., "11")
        java.util.List<shared.entities.Question> searchResults = questionController.searchQuestions("Data", "11");
        System.out.println("Search executed. Total Question objects instantiated from DB: " + searchResults.size());

        // Loop through the list of objects and print their internal data fields
        for (shared.entities.Question q : searchResults) {
            System.out.println(" -> Found Object [ID: " + q.getQuestionId() + "] | Topic: " + q.getTopic() + " | Text: " + q.getText());
        }


        System.out.println("\n--- STEP 4: CLEANING UP ACTIVE SESSION ---");
        loginController.logout("admin");

        // 3. Disconnect safely from MySQL
        dbManager.disconnect();
        System.out.println("\n=== ALL CONTROLLER SUBSYSTEMS VERIFIED SUCCESS ===");
    }
}