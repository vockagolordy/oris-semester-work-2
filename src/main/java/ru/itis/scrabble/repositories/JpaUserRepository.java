package ru.itis.scrabble.repositories;

import jakarta.persistence.EntityManager;
import ru.itis.scrabble.models.User;

public class JpaUserRepository implements UserRepository {
    private final EntityManager em;

    public JpaUserRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public User save(User user) {
        em.getTransaction().begin();
        if (user.getId() == null) {
            em.persist(user);
        } else {
            user = em.merge(user);
        }
        em.getTransaction().commit();
        return user;
    }

    @Override
    public User findByUsername(String username) {
        return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public User findById(Long id) {
        return em.find(User.class, id);
    }

    @Override
    public void update(User user) {
        save(user); // В JPA merge используется для обновления
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username) != null;
    }
}