package com.explore.notificationbot.repository;

import com.explore.notificationbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<User, UUID> {
    User findByChatId(Long chatId);
    boolean existsByChatId(Long chatId);
}
