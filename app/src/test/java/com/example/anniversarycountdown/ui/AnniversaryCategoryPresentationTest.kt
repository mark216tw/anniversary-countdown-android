package com.example.anniversarycountdown.ui

import com.example.anniversarycountdown.data.AnniversaryCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnniversaryCategoryPresentationTest {
    @Test
    fun everyCategoryHasExpectedLabelAndUniqueIcon() {
        assertEquals(
            listOf("生日", "愛情", "家庭", "旅行", "工作", "其他"),
            AnniversaryCategory.entries.map(AnniversaryCategory::label),
        )

        val icons = AnniversaryCategory.entries.map(AnniversaryCategory::iconRes)
        assertEquals(AnniversaryCategory.entries.size, icons.distinct().size)
        assertTrue(icons.all { it != 0 })
    }
}
