package de.raphael.macro.data

import org.junit.Assert.*
import org.junit.Test

class MacroLogicTest {

    private fun baseSettings(): Settings = Settings(weightKg = 80f, maintenance = 2000)

    @Test
    fun targetsNullWhenWeightMissing() {
        val s = Settings(maintenance = 2000)
        assertNull(Prefs.targets(s))
    }

    @Test
    fun targetsNullWhenMaintenanceMissing() {
        val s = Settings(weightKg = 80f)
        assertNull(Prefs.targets(s))
    }

    @Test
    fun targetsRecomp() {
        val t = Prefs.targets(baseSettings(), Focus.RECOMP)
        assertNotNull(t)
        assertEquals(1800, t!!.kcal)
        assertEquals(160, t.protein)
        assertEquals(178, t.carbs)
        assertEquals(50, t.fat)
    }

    @Test
    fun targetsCut() {
        val t = Prefs.targets(baseSettings(), Focus.CUT)
        assertNotNull(t)
        assertEquals(1600, t!!.kcal)
        assertEquals(176, t.protein)
        assertEquals(125, t.carbs)
        assertEquals(44, t.fat)
    }

    @Test
    fun targetsBulk() {
        val t = Prefs.targets(baseSettings(), Focus.BULK)
        assertNotNull(t)
        assertEquals(2200, t!!.kcal)
        assertEquals(144, t.protein)
        assertEquals(269, t.carbs)
        assertEquals(61, t.fat)
    }

    @Test
    fun targetsMaintain() {
        val t = Prefs.targets(baseSettings(), Focus.MAINTAIN)
        assertNotNull(t)
        assertEquals(2000, t!!.kcal)
        assertEquals(128, t.protein)
        assertEquals(246, t.carbs)
        assertEquals(56, t.fat)
    }

    @Test
    fun targetsCustomPctAndProt() {
        val s = Settings(weightKg = 70f, maintenance = 2500).copy(
            pct = mapOf(Focus.RECOMP to -5),
            prot = mapOf(Focus.RECOMP to 2.5f)
        )

        val t = Prefs.targets(s, Focus.RECOMP)
        assertNotNull(t)
        assertEquals(2375, t!!.kcal)
        assertEquals(175, t.protein)
        assertEquals(270, t.carbs)
        assertEquals(66, t.fat)
    }

    @Test
    fun targetsCarbsNeverNegative() {
        val s = Settings(weightKg = 100f, maintenance = 1000, focus = Focus.CUT)

        val t = Prefs.targets(s)
        assertNotNull(t)
        assertEquals(800, t!!.kcal)
        assertEquals(220, t.protein)
        assertEquals(0, t.carbs)
        assertEquals(22, t.fat)
    }

    @Test
    fun sumPlus() {
        val a = Sum(10f, 2f, 3f, 4f)
        val b = Sum(5f, 1f, 2f, 1f)
        val r = a + b

        assertEquals(15f, r.kcal, 0.01f)
        assertEquals(3f, r.p, 0.01f)
        assertEquals(5f, r.c, 0.01f)
        assertEquals(5f, r.f, 0.01f)
    }

    private fun testFood(): Food = Food(
        id = 1L,
        name = "Test",
        kcal = 200f,
        p = 10f,
        c = 30f,
        f = 5f,
        portion = 100f,
        fav = false,
        lastUsed = 0L,
        barcode = null
    )

    @Test
    fun foodSumForZero() {
        val s = testFood().sumFor(0f)

        assertEquals(0f, s.kcal, 0.01f)
        assertEquals(0f, s.p, 0.01f)
        assertEquals(0f, s.c, 0.01f)
        assertEquals(0f, s.f, 0.01f)
    }

    @Test
    fun foodSumFor100() {
        val s = testFood().sumFor(100f)

        assertEquals(200f, s.kcal, 0.01f)
        assertEquals(10f, s.p, 0.01f)
        assertEquals(30f, s.c, 0.01f)
        assertEquals(5f, s.f, 0.01f)
    }

    @Test
    fun foodSumFor250() {
        val s = testFood().sumFor(250f)

        assertEquals(500f, s.kcal, 0.01f)
        assertEquals(25f, s.p, 0.01f)
        assertEquals(75f, s.c, 0.01f)
        assertEquals(12.5f, s.f, 0.01f)
    }
}
