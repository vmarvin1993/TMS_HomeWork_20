import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *  Реализовать методы в DbUserStorage используя JDBC:
 *  //save
 *  //updateNameById
 *  //updatePasswordById
 *  //deleteById
 *  //getAll
 *  //getAllByName
 *  //getByUsername
 *  //getAllByStreet
 *  //exist
 *  //existById
 *
 */
public class DbUserStorage {
    private Connection connection;

    public DbUserStorage(String url) {
        try {
            this.connection = DriverManager.getConnection(url);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void save(User user) {
        try {
            connection.setAutoCommit(false);
            PreparedStatement preparedStatement = connection.prepareStatement("insert into users values (default, ?, ?, ?) returning id");
            preparedStatement.setString(1, user.getName());
            preparedStatement.setString(2, user.getUsername());
            preparedStatement.setString(3, user.getPassword());
            ResultSet resultSet3 = preparedStatement.executeQuery();
            resultSet3.next();
            int userId = resultSet3.getInt(1);

            for (Address address : user.getAddresses()) {
                PreparedStatement preparedStatement1 = connection.prepareStatement("insert into addresses values (default, ?, ?)");
                preparedStatement1.setString(1, address.getStreet());
                preparedStatement1.setInt(2, userId);
                preparedStatement1.execute();
            }
            for (Phone phone : user.getPhones()) {
                PreparedStatement preparedStatement2 = connection.prepareStatement("insert into phones values (default, ?, ?)");
                preparedStatement2.setString(1, phone.getPhone());
                preparedStatement2.setInt(2, userId);
                preparedStatement2.execute();
            }
            connection.commit();
        } catch (SQLException throwables) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            throwables.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    public void updateNameById(int id, String name) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("update users set name = ? where id = ?")) {
            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, id);
            preparedStatement.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void updatePasswordById(int id, String password) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("update users set password = ? where id = ?")) {
            preparedStatement.setString(1, password);
            preparedStatement.setInt(2, id);
            preparedStatement.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void deleteById(int id) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("delete from users where id = ?");
             PreparedStatement addressStatement = connection.prepareStatement("delete from addresses where user_id = ?");
             PreparedStatement phoneStatement = connection.prepareStatement("delete from phones where user_id = ?")) {
            preparedStatement.setInt(1, id);
            preparedStatement.execute();
            addressStatement.setInt(1, id);
            addressStatement.execute();
            phoneStatement.setInt(1, id);
            phoneStatement.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public Optional<List<User>> getAll() {
        if (exist()) {
            List<User> users = new ArrayList<>();
            try (PreparedStatement preparedStatement = connection.prepareStatement("select * from users ")) {
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    int id = resultSet.getInt(1);
                    String name = resultSet.getString(2);
                    String username = resultSet.getString(3);
                    String password = resultSet.getString(4);
                    users.add(new User(id, name, username, password, getAddressesByUserId(id), getPhonesByUserId(id)));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.of(users);
        }
        return Optional.empty();
    }

    public Optional<List<String>> getAllByName() {
        List<String> usersByName = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("select * from users")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                usersByName.add(resultSet.getString(2));
            }
            if (!usersByName.isEmpty()) {
                return Optional.of(usersByName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<User> getByUsername(String username) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("select * from users where username = ?")) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                String username1 = resultSet.getString(3);
                String password = resultSet.getString(4);
                return Optional.of(new User(id, name, username1, password, getAddressesByUserId(id), getPhonesByUserId(id)));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<List<User>> getAllByStreet(String street) {

        List<User> users = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement("select * from addresses where street = ?");
             PreparedStatement usersStatement = connection.prepareStatement("select * from users where id = ?")) {
            preparedStatement.setString(1, street);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                List<Address> addresses = new ArrayList<>();

                int id = resultSet.getInt(1);
                int user_id = resultSet.getInt(3);

                addresses.add(new Address(id, street));
                usersStatement.setInt(1, user_id);
                ResultSet resultSet1 = usersStatement.executeQuery();

                if (resultSet1.next()) {
                    String name = resultSet1.getString(2);
                    String username = resultSet1.getString(3);
                    String password = resultSet1.getString(4);
                    users.add(new User(user_id, name, username, password, addresses, getPhonesByUserId(user_id)));
                }
            }
            if (!users.isEmpty()) {
                return Optional.of(users);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean exist() {
        try (PreparedStatement preparedStatement = connection.prepareStatement("select * from users")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    public boolean existById(int id) {

        try (PreparedStatement preparedStatement = connection.prepareStatement("select * from users where id = ?")) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    private List<Address> getAddressesByUserId(int id) {
        List<Address> addresses = new ArrayList<>();
        try (PreparedStatement prepareStatement = connection.prepareStatement("select * from addresses where user_id = ?")) {
            prepareStatement.setInt(1, id);
            ResultSet resultSet = prepareStatement.executeQuery();

            while (resultSet.next()) {
                int address_id = resultSet.getInt(1);
                String street = resultSet.getString(2);
                addresses.add(new Address(address_id, street));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return addresses;
    }

    private List<Phone> getPhonesByUserId(int id) {

        List<Phone> phones = new ArrayList<>();

        try (PreparedStatement prepareStatement = connection.prepareStatement("select * from phones where user_id = ?")) {
            prepareStatement.setInt(1, id);
            ResultSet resultSet = prepareStatement.executeQuery();

            while (resultSet.next()) {

                int phones_id = resultSet.getInt(1);
                String number = resultSet.getString(2);
                phones.add(new Phone(phones_id, number));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return phones;
    }
}
