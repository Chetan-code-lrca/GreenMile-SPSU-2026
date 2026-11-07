package com.spsu.greenmile.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DepartmentNormalizerTest {
    @Test
    fun recognizesCommonDepartmentCodesAndFullNames() {
        assertEquals("CSE", DepartmentNormalizer.normalize("cse"))
        assertEquals("CSE", DepartmentNormalizer.normalize("Computer Science & Engineering (AI/ML)"))
        assertEquals("ECE", DepartmentNormalizer.normalize("Electronics & Communication Engineering"))
        assertEquals("ME", DepartmentNormalizer.normalize("Mechanical Engineering"))
        assertEquals("CE", DepartmentNormalizer.normalize("Civil Engineering"))
        assertEquals("IT", DepartmentNormalizer.normalize("Information Technology"))
        assertEquals("EEE", DepartmentNormalizer.normalize("Electrical & Electronics Engineering"))
        assertEquals("MCA", DepartmentNormalizer.normalize("Master of Computer Applications"))
        assertEquals("MBA", DepartmentNormalizer.normalize("Master of Business Administration"))
    }

    @Test
    fun shortAliasesRequireExactTokenMatches() {
        assertEquals("CSE", DepartmentNormalizer.normalize("CS"))
        assertEquals("ME", DepartmentNormalizer.normalize("ME"))
        assertEquals("CE", DepartmentNormalizer.normalize("CE"))
        assertEquals("IT", DepartmentNormalizer.normalize("IT"))
        assertEquals("EEE", DepartmentNormalizer.normalize("EEE"))
    }

    @Test
    fun unrelatedNamesAreNotMisclassifiedBySubstring() {
        assertEquals("PHYSICS", DepartmentNormalizer.normalize("Physics"))
        assertEquals("MANAGEMENT", DepartmentNormalizer.normalize("Management"))
        assertEquals("ARCHITECTURE", DepartmentNormalizer.normalize("Architecture"))
        assertEquals("MATHEMATICS", DepartmentNormalizer.normalize("Mathematics"))
    }

    @Test
    fun unknownNamesArePreservedAndWhitespaceIsTrimmed() {
        assertEquals("ROBOTICS", DepartmentNormalizer.normalize("  Robotics  "))
        assertEquals("", DepartmentNormalizer.normalize("   "))
    }
}
