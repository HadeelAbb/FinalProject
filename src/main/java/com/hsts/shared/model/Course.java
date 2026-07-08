package com.hsts.shared.model;

import java.io.Serializable;

/**
 * Client-side copy of Partner 1's Course entity (PDOM).
 * Course numbers/names are managed by an external system per the spec -
 * this client never creates/edits Course objects, only displays them and
 * uses their id as a foreign key when creating/searching Questions.
 */
public class Course implements Serializable {

    private String id;
    private String name;

    public Course() {
    }

    public Course(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course course)) return false;
        return id != null && id.equals(course.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return id + " - " + name;
    }
}
