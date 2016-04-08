package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.ConflictResolutionType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Adds the UNIQUE keyword to a database column.
 */
@Retention(CLASS) @Target(FIELD)
public @interface ReplaceUniqueConstraint {
  /**
   * Defines conflict resolution algorithm.
   */
  ConflictResolutionType onConflict() default ConflictResolutionType.REPLACE;

}
