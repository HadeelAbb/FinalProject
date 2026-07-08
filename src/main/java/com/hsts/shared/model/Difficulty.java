package com.hsts.shared.model;

import java.io.Serializable;

/**
 * Matches the `difficulty` ENUM column in Partner 1's questions table.
 */
public enum Difficulty implements Serializable {
    EASY,
    MEDIUM,
    HARD
}
