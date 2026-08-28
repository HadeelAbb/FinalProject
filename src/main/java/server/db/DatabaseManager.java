package server.db;

import java.sql.Connection; //built-in Java class that represents the physical network pipeline to your MySQL database disk
import java.sql.DriverManager;  //tool that takes your password and database location and handles the authentication handshake with MySQL
import java.sql.SQLException;   //error handling during a database operation
import java.io.InputStream;
import java.util.Properties;

//We want to make sure your server only opens one single connection pipe to MySQL
// so it doesn't overload the laptop's memory.
// We do this by hiding the constructor.


public class DatabaseManager {

    // 1. A static variable that will hold our one and only instance in RAM
    private static DatabaseManager instance = null;

    // 2. The variable holding our active connection pipe
    private Connection connection = null;

    // The network address pointing to port 3306 on your own laptop's hard drive disk (localhost)
    private String dbUrl = "jdbc:mysql://localhost:3306/hsts_db?serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
    private  String dbUser;
    private  String dbPassword;

    // 3. Private constructor: This stops other classes from typing 'new DatabaseManager()'
    private DatabaseManager() { //singleton
        loadCredentials();
    }

    private void loadCredentials() {
        Properties prop = server.ConfigLoader.load();
        this.dbUser = prop.getProperty("db.user");
        this.dbPassword = prop.getProperty("db.password");
        if (this.dbUser == null || this.dbPassword == null) {
            System.err.println("[SERVER-DB] WARNING: db.user/db.password not found in config.properties! Falling back to defaults.");
            if (this.dbUser == null) this.dbUser = "root";
            if (this.dbPassword == null) this.dbPassword = "";
        }
    }

    // 4. The global access point. Anyone who needs the DB must call this method.
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager(); // Create it once if it doesn't exist
        }
        return instance;
    }
    /**
      Connects to the physical MySQL server running on your laptop.
     **/
    public boolean connect() {
        try {
            // 1. Load the MySQL driver class into your computer's RAM
            Class.forName("com.mysql.cj.jdbc.Driver");

            System.out.println("[SERVER-DB] Attempting to connect to the MySQL Hard Drive Storage...");

            // 2. Dial the database using our URL, username, and password
            this.connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            System.out.println("[SERVER-DB] Connection successful! The pipeline is open.");
            return true;

        } catch (ClassNotFoundException e) {
            System.err.println("[SERVER-DB ERROR] MySQL Driver not found. Check your pom.xml!");
            return false;
        } catch (SQLException e) {
            System.err.println("[SERVER-DB ERROR] Connection failed! Is your MySQL Workbench service running?");
            e.printStackTrace();
            return false;
        }
    }

    /**
      Gets the active connection pipe so controllers can run statements.
     **/
    public Connection getConnection() {
        return this.connection;
    }

    /**
      Closes the connection when the server application shuts down.
     **/
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close(); // Cut the network connection line cleanly
                System.out.println("[SERVER-DB] Connection safely closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Temporary test method for Issue #3 to verify Java can read our seeded questions.
     */
    public void testFetchQuestions() {
        String sql = "SELECT question_id, text, difficulty FROM questions";

        // Create a database statement statement execution channel
        try (java.sql.Statement stmt = this.connection.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n>>> FETCHING SEEDED QUESTIONS FROM HARD DRIVE:");

            // Loop through every row returned by the database engine
            while (rs.next()) {
                String id = rs.getString("question_id");
                String text = rs.getString("text");
                String difficulty = rs.getString("difficulty");

                System.out.println("[" + id + "] (" + difficulty + ") " + text);
            }
            System.out.println(">>> FETCH COMPLETE.\n");

        } catch (java.sql.SQLException e) {
            System.err.println("[SERVER-DB ERROR] Failed to fetch test questions.");
            e.printStackTrace();
        }
    }
}
