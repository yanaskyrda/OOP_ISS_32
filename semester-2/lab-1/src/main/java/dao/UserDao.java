package dao;

import model.user.User;

import java.util.Optional;

public interface UserDao extends GenericDao<User> {
    Optional<User> findByUserName(String username);
}
