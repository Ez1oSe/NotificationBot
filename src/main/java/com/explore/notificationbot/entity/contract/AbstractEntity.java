package com.explore.notificationbot.entity.contract;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Getter
@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;

}
