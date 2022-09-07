package io.quarkus.it.panache.reactive;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Cat extends PanacheEntity {

    String name;
    @ManyToOne
    CatOwner owner;

    public Cat(CatOwner owner) {
        this.owner = owner;
    }

    public Cat(String name, CatOwner owner) {
        this.name = name;
        this.owner = owner;
    }

    public Cat() {
    }
}
