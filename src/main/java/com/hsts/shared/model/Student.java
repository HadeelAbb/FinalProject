package com.hsts.shared.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side copy of Partner 1's Student entity (PDOM).
 * courses holds the courses this student is enrolled in - used to filter
 * which exams (SUC-6) and bot (SUC-13/14/15) are visible to her.
 */
public class Student extends User {

    private List<Course> courses = new ArrayList<>();

    public Student() {
    }

    public Student(String id, String firstName, String lastName, String email, List<Course> courses) {
        super(id, firstName, lastName, email, "STUDENT");
        this.courses = courses != null ? courses : new ArrayList<>();
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    public boolean enrolledIn(String courseId) {
        return courses.stream().anyMatch(c -> c.getId().equals(courseId));
    }
}
