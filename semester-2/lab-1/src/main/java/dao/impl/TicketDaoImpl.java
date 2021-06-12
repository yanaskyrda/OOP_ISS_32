package dao.impl;

import dao.TicketDao;
import dao.mapper.TicketMapper;
import model.flight.Flight;
import model.ticket.Status;
import model.ticket.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TicketDaoImpl implements TicketDao {

    private final Logger logger = LogManager.getLogger(TicketDaoImpl.class);
    private final Connection connection;
    private final ResourceBundle bundle = ResourceBundle.getBundle("sql");
    private final TicketMapper ticketMapper = new TicketMapper();
    private final FlightDaoImpl flightDao;

    public TicketDaoImpl(Connection connection) {
        this.connection = connection;
        flightDao = new FlightDaoImpl(connection);
    }

    @Override
    public Optional<Ticket> create(Ticket entity) {
        ResultSet set;
        try {
            PreparedStatement statement = connection.prepareStatement(bundle.getString("ticket.create"),
                    Statement.RETURN_GENERATED_KEYS);

            statement.setBoolean(1, entity.getHaveBaggage());
            statement.setInt(2, entity.getBaggagePrice());
            statement.setBoolean(3, entity.getHavePriorityRegister());
            statement.setInt(4, entity.getPriorityRegisterPrice());
            statement.setLong(5, entity.getFlightId());
            statement.setInt(6, entity.getFlightPrice());
            statement.setString(7, entity.getSeat());
            statement.setString(8, entity.getUsername());
            statement.setString(9, entity.getStatus());
            statement.executeUpdate();

            set = statement.getGeneratedKeys();
            if (set.next()) {
                entity.setId(set.getLong("id"));
            }
            return Optional.of(entity);


        } catch (SQLException e) {
            logger.warn("Ticket can`t be created: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Ticket findById(long id) {
        return null;
    }

    @Override
    public List<Ticket> findAll() {
        List<Ticket> ticketList = new ArrayList<>();
        ResultSet set;
        try {
            PreparedStatement statement = connection.prepareStatement(bundle.getString("ticket.admin.find_all"));
            set = statement.executeQuery();
            while (set.next()) {
                Ticket ticket = ticketMapper.getTicketFromResultSet(set);
                ticket.setFlightId(set.getLong("flight_id"));
                ticketList.add(ticket);
            }
        } catch (SQLException e) {
            logger.warn("Tickets can`t be find: {}", e.getMessage());
        }
        return ticketList;
    }

    @Override
    public List<Ticket> findTicketsByUser(String username) {
        List<Ticket> ticketList = new ArrayList<>();
        ResultSet set;
        try {
            PreparedStatement statement = connection.prepareStatement(bundle.getString("ticket.find_by_user"));
            statement.setString(1, username);
            set = statement.executeQuery();
            while (set.next()) {
                Ticket ticket = ticketMapper.getTicketFromResultSet(set);
                ticket.setFlightId(set.getLong("flight_id"));
                ticketList.add(ticket);
            }

        } catch (SQLException e) {
            logger.warn("Tickets can`t be find by user: {}", e.getMessage());
        }
        return ticketList;
    }

    @Override
    public Optional<Ticket> update(Ticket entity) {
        try {
            connection.setAutoCommit(false);

            TransactionManager.beginTransaction(connection);

            Flight flight = flightDao.findById(entity.getFlightId());

            updateTicketByFlightId(flight.getId(), entity.getId());

            TransactionManager.commitTransaction(connection);

            return Optional.of(entity);

        } catch (SQLException e) {
            TransactionManager.rollbackTransaction(connection);
            logger.warn("Ticket could not be updated: {}", e.getMessage());

            return Optional.empty();
        }
    }

    private void updateTicketByFlightId(long flightId, long ticketId) {
        try {
            PreparedStatement secondStatement = connection.prepareStatement(bundle.getString("ticket.update"));
            secondStatement.setLong(1, flightId);
            secondStatement.setString(2, Status.CONFIRMED.getName());
            secondStatement.setLong(3, ticketId);
            secondStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ticket could not be updated: {}", e.getMessage());
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }


}
