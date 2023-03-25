/*
 *
 * EduDB is made available under the OSI-approved MIT license.
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * /
 */

import net.edudb.Request;
import net.edudb.Response;
import net.edudb.Server;
import net.edudb.ServerHandler;
import net.edudb.engine.Config;
import net.edudb.engine.DatabaseEngine;
import net.edudb.engine.FileManager;
import net.edudb.engine.authentication.Authentication;
import net.edudb.engine.authentication.UserRole;
import net.edudb.exception.AuthenticationFailedException;
import net.edudb.exception.UserAlreadyExistException;
import net.edudb.statistics.Schema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainCommandsTest {
    Server server = new Server();
    ServerHandler serverHandler = server.getServerHandler();

    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";
    private static String token;
    private static final String DATABASE_NAME = "test_db";
    private static final String TABLE_NAME = "test_table";
    private static final String[] COLUMN_NAMES = {"name"};
    private static final String[] COLUMN_TYPES = {"varchar"};
    private static final String[] COLUMN_VALUES = {"old"};
    private static final String[] COLUMN_VALUES_2 = {"old2"};
    private static final String[] COLUMN_NEW_VALUES = {"new"};

    @BeforeEach
    public void setup() throws UserAlreadyExistException, AuthenticationFailedException, IOException {
        TestUtils.deleteDirectory(new File(Config.absolutePath()));
        new File(Config.workspacesPath()).mkdirs();
        new File(Config.usersPath()).createNewFile();
        DatabaseEngine.getInstance().createUser(USERNAME, PASSWORD, UserRole.DEFAULT_ROLE);
        token = Authentication.login(USERNAME, PASSWORD);

    }

    @AfterEach
    public void tearDown() {
        TestUtils.deleteDirectory(new File(Config.absolutePath()));
        Schema.getInstance().reset();
    }


    public Response sendCommand(String command, String database) {
        Request request = new Request(command, database);
        request.setAuthToken(token);
        return serverHandler.handle(request);
    }

    public Response sendCommand(String command) {
        return sendCommand(command, DATABASE_NAME);
    }

    @Test
    public void testCreateDatabase() {
        // Create database
        sendCommand(TestUtils.createDatabase(DATABASE_NAME), null);

        Assertions.assertTrue(new File(Config.databasePath(USERNAME, DATABASE_NAME)).exists());
        Assertions.assertTrue(new File(Config.tablesPath(USERNAME, DATABASE_NAME)).exists());
        Assertions.assertTrue(new File(Config.pagesPath(USERNAME, DATABASE_NAME)).exists());
        Assertions.assertTrue(new File(Config.schemaPath(USERNAME, DATABASE_NAME)).exists());
    }

    @Test
    public void testDropDatabase() {
        // Create database
        sendCommand(TestUtils.createDatabase(DATABASE_NAME), null);
        // Drop database
        sendCommand(TestUtils.dropDatabase(DATABASE_NAME));

        Assertions.assertFalse(new File(Config.databasePath(USERNAME, DATABASE_NAME)).exists());
        Assertions.assertFalse(new File(Config.tablesPath(USERNAME, DATABASE_NAME)).exists());
        Assertions.assertFalse(new File(Config.pagesPath(USERNAME, DATABASE_NAME)).exists());
        Assertions.assertFalse(new File(Config.schemaPath(USERNAME, DATABASE_NAME)).exists());
    }

    @Test
    public void testCreateTable() throws FileNotFoundException {
        // Create database
        sendCommand(TestUtils.createDatabase(DATABASE_NAME), null);
        // Create table
        sendCommand(TestUtils.createTable(TABLE_NAME, COLUMN_NAMES, COLUMN_TYPES));

        List<String> lines = FileManager.readFile(Config.schemaPath(USERNAME, DATABASE_NAME));

        Assertions.assertTrue(new File(Config.tablePath(USERNAME, DATABASE_NAME, TABLE_NAME)).exists());
        Assertions.assertEquals(1, lines.size());
        Assertions.assertEquals(TABLE_NAME, lines.get(0).split(" ")[0]);

    }

    @Test
    public void testDropTable() throws FileNotFoundException {
        // Create database
        sendCommand(TestUtils.createDatabase(DATABASE_NAME), null);
        // Create table
        sendCommand(TestUtils.createTable(TABLE_NAME, COLUMN_NAMES, COLUMN_TYPES));
        // Drop table
        sendCommand(TestUtils.dropTable(TABLE_NAME));

        ArrayList<String> lines = FileManager.readFile(Config.schemaPath(USERNAME, DATABASE_NAME));

        Assertions.assertFalse(new File(Config.tablePath(USERNAME, DATABASE_NAME, TABLE_NAME)).exists());
        Assertions.assertEquals(0, lines.size());
    }

    @Test
    public void testInsert() {
        // Create database
        sendCommand(TestUtils.createDatabase(DATABASE_NAME), null);
        // Create table
        sendCommand(TestUtils.createTable(TABLE_NAME, COLUMN_NAMES, COLUMN_TYPES));
        // Insert
        sendCommand(TestUtils.insert(TABLE_NAME, COLUMN_VALUES));
        // Select
        Response selectResponse = sendCommand(TestUtils.selectAll(TABLE_NAME));

        Assertions.assertEquals(1, selectResponse.getRecords().size());
        Assertions.assertEquals(COLUMN_VALUES[0], selectResponse.getRecords().get(0).getData().values().toArray()[0].toString());
    }

    @Test
    public void testUpdate() {
        // Create database
        sendCommand(TestUtils.createDatabase(DATABASE_NAME), null);
        // Create table
        sendCommand(TestUtils.createTable(TABLE_NAME, COLUMN_NAMES, COLUMN_TYPES));
        // Insert
        sendCommand(TestUtils.insert(TABLE_NAME, COLUMN_VALUES));
        // Update
        sendCommand(TestUtils.update(TABLE_NAME, COLUMN_NAMES[0], COLUMN_VALUES[0], COLUMN_NEW_VALUES[0]));
        // Select
        Response selectResponse = sendCommand(TestUtils.selectAll(TABLE_NAME));

        Assertions.assertEquals(1, selectResponse.getRecords().size());
        Assertions.assertEquals(COLUMN_NEW_VALUES[0], selectResponse.getRecords().get(0).getData().values().toArray()[0].toString());
    }

    @Test
    public void testDelete() {
        // Create database
        sendCommand(TestUtils.createDatabase(DATABASE_NAME), null);
        // Create table
        sendCommand(TestUtils.createTable(TABLE_NAME, COLUMN_NAMES, COLUMN_TYPES));
        // Insert
        sendCommand(TestUtils.insert(TABLE_NAME, COLUMN_VALUES));
        // Delete
        sendCommand(TestUtils.delete(TABLE_NAME, COLUMN_NAMES[0], COLUMN_VALUES[0]));
        // Select
        Response selectResponse = sendCommand(TestUtils.selectAll(TABLE_NAME));

        Assertions.assertEquals(0, selectResponse.getRecords().size());
    }

    @Test
    public void testSelect() {
        // Create database
        sendCommand(TestUtils.createDatabase(DATABASE_NAME), null);
        // Create table
        sendCommand(TestUtils.createTable(TABLE_NAME, COLUMN_NAMES, COLUMN_TYPES));
        // Insert
        sendCommand(TestUtils.insert(TABLE_NAME, COLUMN_VALUES));
        sendCommand(TestUtils.insert(TABLE_NAME, COLUMN_VALUES_2));
        // Select
        Response selectResponse = sendCommand(TestUtils.select(TABLE_NAME, COLUMN_NAMES[0], COLUMN_VALUES[0]));

        Assertions.assertEquals(1, selectResponse.getRecords().size());
        Assertions.assertEquals(COLUMN_VALUES[0], selectResponse.getRecords().get(0).getData().values().toArray()[0].toString());
    }

}
