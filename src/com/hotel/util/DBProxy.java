package com.hotel.util;

import com.hotel.constants.DBConstants;
import com.hotel.entity.Image;
import com.hotel.entity.Room;
import com.hotel.entity.User;
import com.hotel.enums.Capacity;
import com.hotel.enums.Category;
import com.hotel.enums.Role;
import com.hotel.enums.State;
import com.hotel.servlets.AddRoomServlet;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// put mysql-connector.jar in CATALINA_HOME\lib
public class DBProxy {
    /////////////////////////////////////////tables/////////////////////////////////////////
    /////////////////////////////////////////users////////////////////////////////////////
    private final String CREATE_TABLE_USERS_IF_NOT_EXIST = "CREATE TABLE IF NOT EXISTS Users\n" +
            "(Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "UId VARCHAR(100) NOT NULL,\n" +
            "Role ENUM ('Admin', 'Manager', 'Client') NOT NULL,\n" +
            "Login VARCHAR(100) NOT NULL UNIQUE,\n" +
            "FirstName VARCHAR(100) NOT NULL,\n" +
            "LastName VARCHAR(100) NOT NULL,\n" +
            "UserPassword VARCHAR(60) NOT NULL)\n" +
            " ENGINE = INNODB";
    /////////////////////////////////////////users////////////////////////////////////////
    ////////////////////////////////////////tokens/////////////////////////////////////////
    private final String CREATE_TABLE_TOKENS_IF_NOT_EXIST = "CREATE TABLE IF NOT EXISTS Tokens\n" +
            "(Id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
            "UId VARCHAR(100) NOT NULL,\n" +
            "Content VARCHAR(50) NOT NULL UNIQUE)\n" +
            "ENGINE = INNODB";
    ////////////////////////////////////////tokens/////////////////////////////////////////
    ////////////////////////////////////////user token/////////////////////////////////////
    private final String CREATE_TABLE_USER_TOKENS_IF_NOT_EXIST = "CREATE TABLE IF NOT EXISTS UserTokens\n" +
            "(UId VARCHAR(100) NOT NULL,\n" +
            "Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "UserId INT NOT NULL,\n" +
            "INDEX UsId (UserId), \n" +
            "FOREIGN KEY (UserId) REFERENCES Users(Id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
            "TokenId INT NOT NULL,\n" +
            "INDEX TokId (TokenId),\n" +
            "FOREIGN KEY (TokenId) REFERENCES Tokens(Id) ON DELETE CASCADE ON UPDATE CASCADE)\n" +
            "ENGINE = INNODB";
    ////////////////////////////////////////user token/////////////////////////////////////
    ///////////////////////////////////////rooms//////////////////////////////////////////
    private final String CREATE_TABLE_ROOMS_IF_NOT_EXIST =
            "CREATE TABLE IF NOT EXISTS Rooms\n" +
                    "(\n" +
                    "UId VARCHAR(100) NOT NULL,\n" +
                    "Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
                    "RoomNumber INT NOT NULL UNIQUE,\n" +
                    "Category ENUM ('Junior Suite', 'De Luxe', 'Suite', 'Business room', 'Family studio', 'King Suite', 'President Suite') NOT NULL,\n" +
                    "Price DECIMAL(19, 4) NOT NULL ,\n" +
                    "Capacity ENUM ('Single','Double', 'Twin', 'Triple', 'Extra bed', 'Quadriple', 'Child') NOT NULL,\n" +
                    "State ENUM ('Available', 'Reserved', 'Occupied', 'Unavailable')\n" +
                    ") ENGINE = INNODB";
    ///////////////////////////////////////rooms//////////////////////////////////////////
    ///////////////////////////////////////images////////////////////////////////////////
    private final String CREATE_TABLE_IMAGES_IF_NOT_EXIST = "CREATE TABLE IF NOT EXISTS Images\n" +
            "(\n" +
            "UId VARCHAR(100) NOT NULL,\n" +
            "Id SMALLINT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "Url VARCHAR(500) NOT NULL UNIQUE,\n" +
            "RoomId INT NOT NULL,\n" +
            "INDEX RId (RoomId),\n" +
            "FOREIGN KEY (RoomId) REFERENCES Rooms(Id) ON DELETE CASCADE ON UPDATE CASCADE\n" +
            ") ENGINE = INNODB";
    ///////////////////////////////////////images////////////////////////////////////////
    ///////////////////////////////////////emails ////////////////////////////////////////
    private final String CREATE_TABLE_EMAILS_IF_NOT_EXIST = "CREATE TABLE IF NOT EXISTS Emails (\n" +
            "Id SMALLINT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "Email VARCHAR(100) NOT NULL,\n" +
            "AdminPassword LONGBLOB NOT NULL\n" +
            ")";
    ///////////////////////////////////////emails ////////////////////////////////////////
    //////////////////////////////////////journal////////////////////////////////////////
    private final String CREATE_TABLE_JOURNAL_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS Journal\n" +
            "(\n" +
            "UId VARCHAR(100) NOT NULL,\n" +
            "Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
            "RoomId INT NOT NULL,\n" +
            "INDEX RId (RoomId),\n" +
            "FOREIGN KEY (RoomId) REFERENCES Rooms(Id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
            "UserId INT NOT NULL,\n" +
            "INDEX UsId (UserId), \n" +
            "FOREIGN KEY (UserId) REFERENCES Users(Id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
            "MoveInDate Date NOT NULL,\n" +
            "MoveOutDate Date NOT NULL\n" +
            ")";
    /////////////////////////////////////////tables/////////////////////////////////////////

    ////////////////////////////////////////triggers////////////////////////////////////
    /////////////////////////////////////insert  checker/////////////////////////////////
    private final String CREATE_TRIGGER_INSERT_CHECKER =
            "CREATE TRIGGER NumberInsertChecker BEFORE INSERT ON Rooms FOR EACH ROW\n" +
                    "BEGIN\n" +
                    "IF(NEW.RoomNumber < 0) THEN\n" +
                    "SET NEW.RoomNumber = ABS(NEW.RoomNumber) ; \n" +
                    "END IF ; \n" +
                    "IF(NEW.Price < 0) THEN\n" +
                    "SET NEW.Price = ABS(NEW.Price) ; \n" +
                    "END IF ; \n" +
                    "END \n";
    /////////////////////////////////////insert  checker/////////////////////////////////
    /////////////////////////////////////update  checker/////////////////////////////////
    private final String CREATE_TRIGGER_UPDATE_CHECKER =
            "CREATE TRIGGER NumberUpdateChecker BEFORE UPDATE ON Rooms FOR EACH ROW\n" +
                    "BEGIN\n" +
                    "IF(NEW.RoomNumber < 0) THEN\n" +
                    "SET NEW.RoomNumber = ABS(NEW.RoomNumber) ; \n" +
                    "END IF ; \n" +
                    "IF(NEW.Price < 0) THEN\n" +
                    "SET NEW.Price = ABS(NEW.Price) ; \n" +
                    "END IF ; \n" +
                    "END \n";
    /////////////////////////////////////update checker/////////////////////////////////
    ////////////////////////////////////////triggers////////////////////////////////////

    public final String getQueryDeleteUser() {
        return "DELETE FROM Users WHERE Login LIKE ?";
    }

    public final String getQueryDeleteToken() {
        return "DELETE FROM Tokens WHERE Content LIKE ? ORDER BY Id LIMIT 1";
    }

    public final String getQueryDeleteTokenByUserLogin() {
        return "DELETE ut, t FROM UserTokens as ut LEFT JOIN Users as u ON (ut.UserId = u.Id) LEFT JOIN Tokens as t ON (ut.TokenId = t.Id) WHERE u.Login LIKE ?";
    }

    public final String getQueryDeleteImage() {
        return "DELETE FROM Images WHERE UId LIKE ?";
    }

    public final String getQueryDeleteRoom() {
        return "DELETE FROM Rooms WHERE RoomNumber = ?";
    }

    public final String getQuerySelectIdFromRooms() {
        return "SELECT Id FROM Rooms WHERE RoomNumber = ?";
    }

    public final String getQuerySelectIdFromTokens() {
        return "SELECT Id FROM Tokens WHERE Content LIKE ?";
    }

    public final String getQuerySelectIdFromUsers() {
        return "SELECT Id FROM Users WHERE Login LIKE ?";
    }

    public DBProxy() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.fillInStackTrace();
        }
    }

    public void initialize() throws SQLException {
        createDBIfNotExist();

        List<String> queries = Arrays.asList(
                CREATE_TABLE_USERS_IF_NOT_EXIST,
                CREATE_TABLE_TOKENS_IF_NOT_EXIST,
                CREATE_TABLE_USER_TOKENS_IF_NOT_EXIST,
                CREATE_TABLE_ROOMS_IF_NOT_EXIST,
                CREATE_TABLE_IMAGES_IF_NOT_EXIST,
                CREATE_TABLE_EMAILS_IF_NOT_EXIST,
                CREATE_TABLE_JOURNAL_IF_NOT_EXISTS,

                "DROP TRIGGER IF EXISTS NumberInsertChecker",
                "DROP TRIGGER IF EXISTS NumberUpdateChecker",

                CREATE_TRIGGER_INSERT_CHECKER,
                CREATE_TRIGGER_UPDATE_CHECKER
        );

        for (String query : queries) {
            execute(query);
        }

        if (!containsLogin("Emails")) {
            execute("INSERT INTO Emails (Email, AdminPassword)\n" +
                    "VALUES\n" +
                    "('amely.honey@gmail.com', \"ZGN0dmJoeWZ6YmNuamhieg==\")");
        }
        if (!containsLogin("Users")) {
            execute("INSERT INTO Users (Uid, Role, Login, FirstName, LastName, UserPassword) VALUES \n" +
                    "(\"" + generateUId() + "\",\"Admin\", \"amely.honey@gmail.com\", \"Elena\", \"Maximenko\",  \"TDEyMzQ1Njc=\"),\n" +
                    "(\"" + generateUId() + "\",\"Client\", \"dimalevak96@gmail.com\", \"Dmitry\", \"Kozinets\",  \"RDEyMzQ1Njc=\")");
        }
        if (isEmpty("Rooms")) {
            execute("INSERT INTO Rooms (UId, RoomNumber, Category, Price, Capacity, State)\n" +
                    "VALUES\n" +
                    "(\"" + generateUId() + "\", 3, 'King Suite', 2000, 'Quadriple', 'Available'),\n" +
                    "(\"" + generateUId() + "\", 22, 'De Luxe', 896, 'Double', 'Available'),\n" +
                    "(\"" + generateUId() + "\", 32, 'Suite', 700, 'Double', 'Available'),\n" +
                    "(\"" + generateUId() + "\", 47, 'De Luxe', 950, 'Quadriple', 'Available'),\n" +
                    "(\"" + generateUId() + "\", 60, 'Suite', 670, 'Double', 'Available'),\n" +
                    "(\"" + generateUId() + "\", 900, 'President Suite', 1400, 'Triple', 'Available')\n")
            ;
        }
        if (isEmpty("Images")) {
            imageDir = imageDir.replace("\\", "\\" + "\\");
            execute("INSERT INTO Images (UId, Url, RoomId)\n" +
                    "VALUES \n" +
                    // room # 3
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\1730.png") + "\", 1),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\101145754_p.png") + "\", 1),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\decorar-salon-pequeno-moderno-chimenea-preciosa.png") + "\", 1),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\StyleHive.com-Herbeau-Medicis-Weathered-Copper-Bathtub.png") + "\", 1),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\The-Most-Beautiful-Living-Rooms-in-Paris-2.png") + "\", 1),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\Tommy_Bahama_Royal_Kahala_Canopy_Bed.png") + "\", 1),\n" +

                    // room # 22
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\1400960237316.png") + "\", 2),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\1474119329995.png") + "\", 2),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\living-room-with-natural-light.png") + "\", 2),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\saZinebeli.png") + "\", 2),\n" +

                    //room # 32
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\2cd535acd292ffe8537c03601f60cd960a1a8102.png") + "\", 3),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\1400954399039.png") + "\", 3),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\beautiful-rooms-for-the-book-loving-soul-2-4870-1445527471-9_dblbig.png") + "\", 3),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\image.png") + "\", 3),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\maxresdefault.png") + "\", 3),\n" +

                    // room # 47
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\20-Beautiful-Kitchens-with-Dark-Kitchen-Cabinets-Design-1.png") + "\", 4),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\beautiful-bedroom-art-design-ipc253.png") + "\", 4),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\Beautiful-Jacuzzi-Bathtubs.png") + "\", 4),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\dark-floors-fashionable-decorating-beautiful-living-rooms-pictures-fancy-beautiful-living-rooms.png") + "\", 4),\n" +

                    // room # 60
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\6a0133ecb5beb5970b0168e73f0f9a970c-800wi.png") + "\", 5),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\31456-Abstract-Book-Room.png") + "\", 5),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\2677294c72eb0a4708e94c8d6bcf53e50c2c4816.png") + "\", 5),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\Cdw7NqUWszDl.png") + "\", 5),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\modern-family-room-portland-best-interior-design.preview.png") + "\", 5),\n" +

                    // room # 900
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\673ef892761eb6f69bd7e9567eae28be-dining-room-tables-tablescapes.png") + "\", 6),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\201732_idip13a_01_PH142160.png") + "\", 6),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\INgHEJ-Z5KFx.png") + "\", 6),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\traditional-bedroom.png") + "\", 6),\n" +
                    "(\"" + generateUId() + "\", \"" + imageDir.concat("\\\\traditional-living-room.png") + "\", 6)\n"
            );
        }
    }

    public void createDBIfNotExist() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DBConstants.URL_WITHOUT_DB)) {
            connection.setClientInfo("user", DBConstants.USER);
            connection.setClientInfo("password", DBConstants.PASSWORD);

            Statement statement = connection.createStatement();
            statement.execute("CREATE DATABASE IF NOT EXISTS HotelDB");
        }
    }

    public void execute(String query) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.execute();
        }
    }

    private String imageDir = new AddRoomServlet().getImagesDir();

    public boolean isEmpty(String table) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table);
            return !resultSet.next();
        }
    }

    public boolean containsLogin(String table) throws SQLException {
        String field = (table.equals("Emails") ? "Email" : "Login");
        String querySelectFromEmailsByLogin = "SELECT * FROM " + table + " WHERE " + field + " LIKE \"amely.honey@gmail.com\"";
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(querySelectFromEmailsByLogin);
            return resultSet.next();
        }
    }

    public boolean findUser(String login) {
        String querySelectByLogin = "SELECT UserPassword FROM Users WHERE Login LIKE ?";// + "\"" + login + "\"";
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(querySelectByLogin);
            preparedStatement.setString(1, login);
            ResultSet resultSet = preparedStatement.executeQuery();
            return (resultSet.next());
        } catch (SQLException e) {
            e.fillInStackTrace();
            return false;
        }
    }

    public void changeRoleByLogin(String login, String role) throws SQLException {
        String queryUpdateUsersRole = "UPDATE Users SET Role = ? WHERE Login LIKE ?";
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(queryUpdateUsersRole);
            preparedStatement.setString(1, role);
            preparedStatement.setString(2, login);
            preparedStatement.executeUpdate();
        }
    }

    public User getUserByLogin(String userLogin) {
        String querySelectUserByLogin = "SELECT * FROM Users WHERE Login LIKE ?";
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(querySelectUserByLogin);
            preparedStatement.setString(1, userLogin);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            String uId = resultSet.getString("UId");
            int id = resultSet.getInt("Id");
            String firstName = resultSet.getString("FirstName");
            String lastName = resultSet.getString("LastName");
            String login = resultSet.getString("Login");
            String password = resultSet.getString("UserPassword");
            String role = resultSet.getString("Role");
            User user = new User(uId, id, Role.valueOf(role.toUpperCase()), firstName, lastName, login, password);

            return user;
        } catch (SQLException e) {
            e.fillInStackTrace();
            return null;
        }
    }

    public int getId(String string, String query) throws SQLException {
        int id = -1;
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, string);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            id = resultSet.getInt("Id");
        }
        return id;
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        String querySelectAllUsers = "SELECT * FROM Users";

        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(querySelectAllUsers);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String uId = resultSet.getString("UId");
                int id = resultSet.getInt("Id");
                String firstName = resultSet.getString("FirstName");
                String lastName = resultSet.getString("LastName");
                String login = resultSet.getString("Login");
                String password = resultSet.getString("UserPassword");
                String role = resultSet.getString("Role");
                User user = new User(uId, id, Role.valueOf(role.toUpperCase()), firstName, lastName, login, password);
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            e.fillInStackTrace();
            return null;
        }
    }

    public String getUserPassword(String login, String table) {
        String querySelectPassword = "SELECT " + (table.equals("Users") ? "UserPassword" : "AdminPassword") + " FROM " + table +
                " WHERE " + (table.equals("Users") ? "Login" : "Email") + " LIKE \"" + login + "\"";

        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(querySelectPassword);
            resultSet.next();//must be set on next
            String password = resultSet.getString(table.equals("Users") ? "UserPassword" : "AdminPassword");
            return password;
        } catch (SQLException e) {
            e.fillInStackTrace();
            return null;
        }
    }

    private String generateUId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public void insertUser(String firstName, String lastName, String login, String password) throws SQLException {
        String queryInsertInUsers = "INSERT INTO Users (UId, Role, Login, FirstName, LastName, UserPassword) VALUES(?, ?, ?, ?, ?, ?)";
        String uId = generateUId();
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(queryInsertInUsers);
            preparedStatement.setString(1, uId);
            preparedStatement.setString(2, "Client");
            preparedStatement.setString(3, login);
            preparedStatement.setString(4, firstName);
            preparedStatement.setString(5, lastName);
            preparedStatement.setString(6, password);

            preparedStatement.executeUpdate();
        }
    }

    public void changePassword(String login, String password) throws SQLException {
        String queryUpdatePassword = "UPDATE Users SET UserPassword = ? WHERE Login LIKE ?";
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(queryUpdatePassword);
            preparedStatement.setString(1, password);
            preparedStatement.setString(2, login);
            preparedStatement.executeUpdate();
        }
    }

    public void insertToken(String token) throws SQLException {
        String queryInsertInTokens = "INSERT INTO Tokens (UId, Content) VALUES (?, ?)";
        String uId = generateUId();
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(queryInsertInTokens);
            preparedStatement.setString(1, uId);
            preparedStatement.setString(2, token);
            preparedStatement.executeUpdate();
        }
    }

    public void insertUserToken(String login, String token) throws SQLException {
        String queryInsertInUserTokens = "INSERT INTO UserTokens (UId, UserID, TokenId) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            String userId = String.valueOf(getId(login, getQuerySelectIdFromUsers()));
            String tokenId = String.valueOf(getId(token, getQuerySelectIdFromTokens()));
            String uId = generateUId();

            PreparedStatement preparedStatement = connection.prepareStatement(queryInsertInUserTokens);
            preparedStatement.setString(1, uId);
            preparedStatement.setString(2, userId);
            preparedStatement.setString(3, tokenId);

            preparedStatement.executeUpdate();
        }
    }

    public String getLoginByToken(String token) throws SQLException {
        String querySelectLoginByToken = "SELECT Login FROM Tokens AS t LEFT JOIN UserTokens AS ut ON t.Id = ut.TokenId LEFT JOIN Users as u ON ut.UserId = u.Id WHERE Content LIKE ?";
        String login;
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(querySelectLoginByToken);
            preparedStatement.setString(1, token);
            ResultSet set = preparedStatement.executeQuery();
            set.next();

            login = set.getString("Login");
        }
        return login;
    }

    public void delete(String value, String query) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, value);
            preparedStatement.executeUpdate();
        }
    }

    public Room insertRoom(int number, String category, double price, String capacity, String state) throws SQLException {
        String queryInsertInRooms = "INSERT INTO Rooms (UId, RoomNumber, Category, Price, Capacity, State) VALUES (?, ?, ?, ?, ?, ?)";
        String uId = generateUId();
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(queryInsertInRooms);
            preparedStatement.setString(1, uId);
            preparedStatement.setInt(2, number);
            preparedStatement.setString(3, category);
            preparedStatement.setDouble(4, price);
            preparedStatement.setString(5, capacity);
            preparedStatement.setString(6, state);
            preparedStatement.executeUpdate();

            Category roomCategory = Category.valueOf(category.contains(" ") ? category.toUpperCase().replace(' ', '_') : category.toUpperCase());
            Capacity roomCapacity = Capacity.valueOf(capacity.contains(" ") ? capacity.toUpperCase().replace(' ', '_') : capacity.toUpperCase());
            State roomState = State.valueOf(state.toUpperCase());
            return new Room(number, price, roomCategory, roomCapacity, roomState);
        }
    }

    public int getId(int number, String query) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, ((number > 0) ? number : Math.abs(number)));
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            return resultSet.getInt("Id");
        }
    }

    public Image insertImage(String url, int roomId) throws SQLException {
        String queryInsertInImages = "INSERT INTO Images (UId, Url, RoomId) VALUES (?, ?, ?)";
        String uId = generateUId();
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(queryInsertInImages);
            preparedStatement.setString(1, uId);
            preparedStatement.setString(2, url);
            preparedStatement.setInt(3, roomId);
            preparedStatement.executeUpdate();

            return new Image(uId, url);
        }
    }

    public List<Room> getRooms() {
        String querySelectAllRooms = "SELECT * FROM ROOMS";
        List<Room> rooms = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(querySelectAllRooms);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int number = resultSet.getInt("RoomNumber");
                double price = resultSet.getDouble("Price");
                String category = resultSet.getString("Category").toUpperCase();
                Category roomCategory = Category.valueOf(category.contains(" ") ? category.replace(' ', '_') : category);
                String capacity = resultSet.getString("Capacity").toUpperCase();
                Capacity roomCapacity = Capacity.valueOf(capacity.contains(" ") ? capacity.replace(" ", "_") : capacity);
                State state = State.valueOf(resultSet.getString("State").toUpperCase());

                Room room = new Room(number, price, roomCategory, roomCapacity, state);
                rooms.add(room);
            }
            return rooms;
        } catch (SQLException sqle) {
            sqle.fillInStackTrace();
            return null;
        }
    }

    public void delete(int number, String query) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, number);
            preparedStatement.executeUpdate();
        }
    }

    public Room getRoomByNumber(int number) throws SQLException {
        String querySelectRoomByNumber = "SELECT * FROM Rooms WHERE RoomNumber = ?";
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(querySelectRoomByNumber);
            preparedStatement.setInt(1, number);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();

            Double price = resultSet.getDouble("Price");

            String category = resultSet.getString("Category").toUpperCase();
            Category roomCategory = Category.valueOf(category.contains(" ") ? category.replace(' ', '_') : category);

            String capacity = resultSet.getString("Capacity").toUpperCase();
            Capacity roomCapacity = Capacity.valueOf(capacity.contains(" ") ? capacity.replace(' ', '_') : capacity);

            String state = resultSet.getString("State").toUpperCase();
            State roomState = State.valueOf(state.contains(" ") ? state.replace(' ', '_') : state);

            return new Room(number, price, roomCategory, roomCapacity, roomState);
        }
    }

    public List<Image> getImagesByRoomNumber(int number) throws SQLException {
        String querySelectImagesByRoomNumber = "SELECT Url, i.UId, RoomId, RoomNumber FROM Images AS i RIGHT JOIN Rooms As r ON (i.RoomId = r.Id) WHERE RoomNumber = ?";
        List<Image> images = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(querySelectImagesByRoomNumber);
            preparedStatement.setInt(1, number);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String uId = resultSet.getString("i.UId");
                String url = resultSet.getString("Url");
                images.add(new Image(uId, url));
            }
        }
        return images;
    }

    public void updateRoomByNumber(int newNumber, String category, double price, String capacity, String state, int oldNumber) throws SQLException {
        String updateRoomByNumber = "UPDATE Rooms SET RoomNumber = ?," +
                "Category = ?, Price = ?, Capacity = ?, State = ? WHERE RoomNumber = ?";
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement(updateRoomByNumber);
            preparedStatement.setInt(1, newNumber);
            preparedStatement.setString(2, category);
            preparedStatement.setDouble(3, price);
            preparedStatement.setString(4, capacity);
            preparedStatement.setString(5, state);
            preparedStatement.setInt(6, oldNumber);

            preparedStatement.executeUpdate();
        }
    }

    // and use for occupied room
    public Room changeState(String state, int number) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE Rooms\n" +
                    "SET State = ? WHERE RoomNumber = ?");
            preparedStatement.setString(1, state);
            preparedStatement.setInt(2, number);
            preparedStatement.executeUpdate();

            Room room = getRoomByNumber(number);
            room.setState(State.valueOf(state.toUpperCase()));
            return  room;
        }
    }

    public void insertInJournal(String login, int roomNumber, Date moveIn, Date moveOut) throws SQLException {
        int roomId = getId(roomNumber, getQuerySelectIdFromRooms());
        int userId = getId(login, getQuerySelectIdFromUsers());

        try (Connection connection = DriverManager.getConnection(DBConstants.URL, DBConstants.USER, DBConstants.PASSWORD)) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO Journal (UId, RoomId, UserId, MoveInDate, MoveOutDate)\n" +
                    "VALUES \n" +
                    "(?, ?, ?, ?, ?)");
            preparedStatement.setString(1, generateUId());
            //!!!!!!!!!!!!!!!!!!! change room state !!!!!!!!!!!!!!!!!!!
            preparedStatement.setInt(2, roomId);
            preparedStatement.setInt(3, userId);
            preparedStatement.setDate(4, moveIn);
            preparedStatement.setDate(5, moveOut);
            preparedStatement.executeUpdate();
        }
    }
}