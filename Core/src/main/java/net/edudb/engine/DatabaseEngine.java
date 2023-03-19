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
import net.edudb.engine.authentication.Authentication;
import net.edudb.engine.authentication.UserRole;
import net.edudb.exception.*;
import net.edudb.statistics.DatabaseSchema;
import net.edudb.statistics.Schema;
import net.edudb.statistics.WorkspaceSchema;
import net.edudb.structure.table.Table;
import net.edudb.structure.table.TableManager;

import java.util.LinkedHashMap;

public class DatabaseEngine {
    private static DatabaseEngine instance = new DatabaseEngine();
    private FileManager fileManager;
    private Schema schema;

    private DatabaseEngine() {
    }


    public static DatabaseEngine getInstance() {
        return instance;
    }

    public void start() {
        fileManager = FileManager.getInstance();
        fileManager.createDirectoryIfNotExists(Config.databasesPath("admin")); //FIXME: refactor this
        schema = Schema.getInstance();
    }

    public void createUser(String username, String password, UserRole role) throws UserAlreadyExistException {

        Authentication.createUser(username, password, role);
        try {
            fileManager.createWorkspace(username);
            schema.addWorkspace(username);
        } catch (WorkspaceAlreadyExistException e) {
            throw new RuntimeException(e);
        }
    }

    public void dropUser(String username) throws UserNotFoundException {
        Authentication.removeUser(username);
        try {
            fileManager.deleteWorkspace(username);
            schema.removeWorkspace(username);
        } catch (WorkspaceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public void openDatabase(String workspaceName, String databaseName) throws DatabaseNotFoundException, WorkspaceNotFoundException {
        WorkspaceSchema workspaceSchema = schema.getWorkspace(workspaceName);
        workspaceSchema.loadDatabase(databaseName);
    }

    public void closeDatabase(String workspaceName, String databaseName) throws DatabaseNotFoundException, WorkspaceNotFoundException {

        BufferManager.getInstance().writeAll(); //FIXME: it will write the whole buffer, not just the database's buffer
        TableManager.getInstance().writeAllTables(workspaceName, databaseName);

        WorkspaceSchema workspaceSchema = schema.getWorkspace(workspaceName);
        workspaceSchema.offloadDatabase(databaseName);
    }

    public void createDatabase(String workspaceName, String databaseName) throws DatabaseAlreadyExistException, WorkspaceNotFoundException {
        FileManager.getInstance().createDatabase(workspaceName, databaseName);

        WorkspaceSchema workspaceSchema = schema.getWorkspace(workspaceName);
        workspaceSchema.addDatabase(databaseName);
    }

    public void dropDatabase(String workspaceName, String databaseName) throws DatabaseNotFoundException, WorkspaceNotFoundException {
        FileManager.getInstance().deleteDatabase(workspaceName, databaseName);

        WorkspaceSchema workspaceSchema = schema.getWorkspace(workspaceName);
        workspaceSchema.removeDatabase(databaseName);
    }

    public String[] listDatabases(String workspaceName) throws WorkspaceNotFoundException {
        WorkspaceSchema workspaceSchema = schema.getWorkspace(workspaceName);
        return workspaceSchema.listDatabases();
    }

    public Table createTable(String workspaceName, String databaseName, String tableSchemaLine, LinkedHashMap<String, String> columnTypes) throws TableAlreadyExistException, DatabaseNotFoundException, WorkspaceNotFoundException {
        TableManager tableManager = TableManager.getInstance();
        Table table = tableManager.createTable(workspaceName, databaseName, tableSchemaLine, columnTypes);


        WorkspaceSchema workspaceSchema = schema.getWorkspace(workspaceName);
        DatabaseSchema databaseSchema = workspaceSchema.getDatabase(databaseName);
        databaseSchema.addTable(tableSchemaLine);

        return table;
    }

    public void dropTable(String workspaceName, String databaseName, String tableName) throws TableNotFoundException, DatabaseNotFoundException, WorkspaceNotFoundException {
        TableManager tableManager = TableManager.getInstance();
        tableManager.deleteTable(workspaceName, databaseName, tableName);

        WorkspaceSchema workspaceSchema = schema.getWorkspace(workspaceName);
        DatabaseSchema databaseSchema = workspaceSchema.getDatabase(databaseName);
        databaseSchema.removeTable(tableName);
    }


}