package de.raphael.macro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetLogicTest {
    private val t = Targets(kcal = 2000, protein = 150, carbs = 200, fat = 60)

    @Test fun shareIsCappedAndZeroWithoutTarget() {
        assertEquals(0.5f, shareOf(50f, 100), 0.001f)
        assertEquals(1.5f, shareOf(500f, 100), 0.001f)
        assertEquals(0f, shareOf(50f, 0), 0.001f)
    }

    @Test fun kcalTextCoversAllCases() {
        assertEquals("noch 640 kcal", kcalLeftText(1360f, 2000))
        assertEquals("Ziel erreicht", kcalLeftText(2030f, 2000))
        assertEquals("120 kcal drüber", kcalLeftText(2120f, 2000))
        assertEquals("900 kcal gegessen", kcalLeftText(900f, null))
    }

    @Test fun proteinGapOnlyFromTenGrams() {
        assertEquals("Eiweiß: noch 70 g", proteinGapText(Sum(p = 80f), t))
        assertNull(proteinGapText(Sum(p = 145f), t))
        assertNull(proteinGapText(Sum(p = 10f), null))
    }
}
