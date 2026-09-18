package com.example.orders.repository;

import com.example.orders.domain.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
@Profile("jdbc")
public class JdbcOrderRepository implements OrderRepository {

    private static final String INSERT_SQL = """
            INSERT INTO orders(id, email, phone, subtotal, total)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, email, phone, subtotal, total
            FROM orders
            WHERE id = ?
            """;

    private final DataSource dataSource;

    public JdbcOrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Order order) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(INSERT_SQL)) {

            statement.setLong(1, order.id());
            statement.setString(2, order.email());
            statement.setString(3, order.phone());
            statement.setBigDecimal(4, order.subtotal());
            statement.setBigDecimal(5, order.total());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot save order " + order.id(), e
            );
        }
    }

    @Override
    public Optional<Order> findById(long id) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            statement.setLong(1, id);

            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapOrder(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot find order " + id, e
            );
        }
    }

    private Order mapOrder(ResultSet resultSet) throws SQLException {
        return new Order(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getBigDecimal("subtotal"),
                resultSet.getBigDecimal("total")
        );
    }
}
