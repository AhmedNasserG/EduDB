/*
 *
 * EduDB is made available under the OSI-approved MIT license.
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * /
 */

package net.edudb.engine;

import net.edudb.buffer.BufferManager;
import net.edudb.server.ServerWriter;
import net.edudb.statistics.Schema;
import net.edudb.structure.table.TableManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Manages the system's databases and their required files and directories.
 *
 * @author Ahmed Abdul Badie
 */
public class DatabaseSystem {
    private static final DatabaseSystem instance = new DatabaseSystem();
    private final String DATABASES_DIR_NAME = "databases";
    private boolean databaseIsOpen;
    private ThreadLocal<String> databaseName = new ThreadLocal<>();

    private DatabaseSystem() {
    }

    public static DatabaseSystem getInstance() {
        return instance;
    }

    /**
     * Creates the required directories for the system to work properly.
     */
    public void initializeDirectories() {
        createDatabasesDirectory();
    }

    /**
     * Initialized the required directories for the database to be able to
     * function properly. These are the directories where files are saved to
     * disk.
     *
     * @param databaseName The name of the database to initialize its directories.
     */
    private void initializeDatabaseDirectories(String databaseName) {
        createTablesDirectory(databaseName);
        createBlocksDirectory(databaseName);
        // createIndexesDirectory(databaseName);
        createSchemaFile(databaseName);
    }


    /**
     * Opens a given database if it is available.
     *
     * @param databaseName The name of the database to open.
     */
//    public String open(String databaseName) {
//        if (databaseExists(databaseName)) {
//            setDatabaseName(databaseName);
//            setDatabaseIsOpen(true);
//            initializeDatabaseDirectories(getDatabaseName());
//            Schema.getInstance().setSchema();
//            return "Opened database '" + databaseName + "'";
//        } else {
//            return "Database '" + databaseName + "' does not exist";
//        }
//    }
    public boolean open(String databaseName) {
        if (!databaseExists(databaseName)) {
            return false;
        }
        setDatabaseName(databaseName);
        setDatabaseIsOpen(true);
        initializeDatabaseDirectories(getDatabaseName());
        Schema.getInstance().setSchema();
        return true;
    }

    /**
     * Closes the current open database, if any.
     */
    public boolean close() {

        BufferManager.getInstance().writeAll();
        TableManager.getInstance().writeAll();


        setDatabaseName(null);
        setDatabaseIsOpen(false);

        Schema.getInstance().resetSchema();

        return true;
    }

    /**
     * Creates a new database in the system iff it does not exist.
     *
     * @param databaseName The name of the database to create.
     */
    public boolean createDatabase(String databaseName) {
        if (databaseExists(databaseName)) {
            return false;
        }
        new File(Config.absolutePath() + DATABASES_DIR_NAME + "/" + databaseName).mkdir();

        return true;
    }

    /**
     * Drops a database from the system iff it does exist.
     *
     * @param databaseName The name of the database to drop.
     * @throws IOException
     */
    public String dropDatabase(String databaseName) throws IOException {
        if (!databaseExists(databaseName)) {
            return "Database '" + databaseName + "' does not exist";
        }
//        if (isDatabaseIsOpen()) {
//            close();
//        }

        Path directory = Paths.get(Config.absolutePath() + DATABASES_DIR_NAME + "/" + databaseName);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        return "Dropped database '" + databaseName + "'";
    }

    /**
     * Lists all the databases in the system.
     *
     * @return A string containing the names of the databases.
     * @author Ahmed Nasser Gaafar
     */
    public String listDatabases() {
        File databases = new File(Config.absolutePath() + DATABASES_DIR_NAME);
        String[] databaseNames = databases.list();
        String result = "";
        for (String databaseName : databaseNames) {
            result += "* " + databaseName + "\r\n";
        }
        return result;
    }


    /**
     * Checks whether the given database exists in the system.
     *
     * @param databaseName The name of the database to check.
     * @return The availability of the database.
     */
    public boolean databaseExists(String databaseName) {
        return new File(Config.absolutePath() + DATABASES_DIR_NAME + "/" + databaseName).exists();
    }

    /**
     * Creates the database's root directory.
     */
    private void createDatabasesDirectory() {
        File databases = new File(Config.databasesPath());
        if (!databases.exists()) {
            databases.mkdirs();
        }
    }

    /**
     * Creates the database's tables directory.
     *
     * @param databaseName The name of the database.
     */
    private void createTablesDirectory(String databaseName) {
        File tables = new File(Config.absolutePath() + DATABASES_DIR_NAME + "/" + databaseName + "/tables");
        if (!tables.exists()) {
            tables.mkdir();
        }
    }

    /**
     * Creates the database's pages directory.
     *
     * @param databaseName The name of the database.
     */
    private void createBlocksDirectory(String databaseName) {
        File blocks = new File(Config.absolutePath() + DATABASES_DIR_NAME + "/" + databaseName + "/blocks");
        if (!blocks.exists()) {
            blocks.mkdir();
        }
    }

    /**
     * This method is documented since the indx is not yet supported
     */
    // private void createIndexesDirectory(String databaseName) {
    // File indexes = new File(Config.absolutePath() + databasesString + "/" +
    // databaseName + "/indexes");
    // if (!indexes.exists()) {
    // indexes.mkdir();
    // }
    // }

    /**
     * Creates the database's schema file.
     *
     * @param databaseName The name of the database.
     */
    private void createSchemaFile(String databaseName) {
        File schema = new File(Config.absolutePath() + DATABASES_DIR_NAME + "/" + databaseName + "/schema.txt");
        if (!schema.exists()) {
            try {
                schema.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Exits the system.
     *
     * @param status The status of the exit.
     */
    public void exit(int status) {
        if (isDatabaseIsOpen()) {
            close();
        }

        /**
         * Writes exit to the client to close the connection.
         */
        if (ServerWriter.getInstance().getContext() != null) {
            ServerWriter.getInstance().writeln("[edudb::exit]");
        }
        System.exit(status);
    }

    public void setDatabaseIsOpen(boolean databaseIsOpen) {
        this.databaseIsOpen = databaseIsOpen;
    }

    public boolean isDatabaseIsOpen() {
//        return this.databaseIsOpen;
        return this.databaseName.get() != null;
    }

    /**
     * @return The name of the current open database.
     */
    public String getDatabaseName() {
        return this.databaseName.get();
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName.set(databaseName);
    }
}
