package com.example.orders.entitie;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentEntity {
    @Id
    private Long id;

    @Column(nullable = false, length = 255)
    @Getter(AccessLevel.NONE)
    private String name;
    // Коллекция хранится строками отдельной таблицы, не значением одной колонки.
    @ManyToMany
    @JoinTable(name = "student_courses",
            joinColumns = @JoinColumn(name = "student_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "course_id", nullable = false))
    private Set<CourseEntity> courses = new LinkedHashSet<>();

    public StudentEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addCourse(CourseEntity course) {
        courses.add(java.util.Objects.requireNonNull(course));
    }
}
