package com.likethesalad.placeholder.models

import java.io.File

data class VariantResPaths(
    val variantName: String,
    val paths: Set<File>
)