package com.explore.notificationbot.entity;

import com.explore.notificationbot.entity.contract.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification extends AbstractEntity {

    @Column(name = "title")
    String title;

    @Column(name = "description")
    String description;

    @Enumerated(EnumType.STRING)
    Status status;

    @Column(name = "seconds")
    Integer seconds;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;
}
