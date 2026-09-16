package com.example.anniversarycountdown.ui

import androidx.annotation.DrawableRes
import com.example.anniversarycountdown.R
import com.example.anniversarycountdown.data.AnniversaryCategory

internal fun AnniversaryCategory.label(): String = when (this) {
    AnniversaryCategory.BIRTHDAY -> "生日"
    AnniversaryCategory.LOVE -> "愛情"
    AnniversaryCategory.FAMILY -> "家庭"
    AnniversaryCategory.TRAVEL -> "旅行"
    AnniversaryCategory.WORK -> "工作"
    AnniversaryCategory.OTHER -> "其他"
}

@DrawableRes
internal fun AnniversaryCategory.iconRes(): Int = when (this) {
    AnniversaryCategory.BIRTHDAY -> R.drawable.ic_category_birthday
    AnniversaryCategory.LOVE -> R.drawable.ic_category_love
    AnniversaryCategory.FAMILY -> R.drawable.ic_category_family
    AnniversaryCategory.TRAVEL -> R.drawable.ic_category_travel
    AnniversaryCategory.WORK -> R.drawable.ic_category_work
    AnniversaryCategory.OTHER -> R.drawable.ic_category_other
}
