package com.steeshock.streetworkout.data.mappers

import com.steeshock.streetworkout.data.model.CategoryDto
import com.steeshock.streetworkout.data.model.UserDto
import com.steeshock.streetworkout.domain.entity.Category
import com.steeshock.streetworkout.domain.entity.User

fun CategoryDto.mapToEntity(): Category {
    return Category(
        categoryId = this.categoryId,
        categoryName = this.categoryName,
        isSelected = this.isSelected,
    )
}
fun Category.mapToDto(): CategoryDto {
    return CategoryDto(
        categoryId = this.categoryId,
        categoryName = this.categoryName,
        isSelected = this.isSelected,
    )
}