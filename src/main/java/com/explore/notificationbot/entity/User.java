package com.explore.notificationbot.entity;

import com.explore.notificationbot.entity.contract.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bot_users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends AbstractEntity {

    @Column(name = "chat_id", unique = true, nullable = false)
    Long chatId;

    @Column(name = "first_name", nullable = false)
    String firstName;

    @Enumerated(EnumType.STRING)
    Action action;

    @Column(name = "registered_at", nullable = false)
    LocalDateTime registeredAt;

    @OneToMany
    Set<Notification> notifications;

    @Column(name = "current_notification_id")
    UUID currentNotification;

}
