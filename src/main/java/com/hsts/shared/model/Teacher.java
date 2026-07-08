package com.hsts.shared.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side copy of Partner 1's Teacher entity (PDOM).
 * courses holds the actual Course objects this teacher teaches - per spec
 * (3.1), a teacher can only create/edit questions for courses she teaches,
 * so the GUI uses this list to restrict the course selector.
 */
public class Teacher extends User {

    private List<Course> courses = new ArrayList<>();

    public Teacher() {
    }

    public Teacher(String id, String firstName, String lastName, String email, List<Course> courses) {
        super(id, firstName, lastName, email, "TEACHER");
        this.courses = courses != null ? courses : new ArrayList<>();
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    public boolean teaches(String courseId) {
        return courses.stream().anyMatch(c -> c.getId().equals(courseId));
    }
}
