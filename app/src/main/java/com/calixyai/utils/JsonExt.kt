package com.calixyai.utils

fun List<String>.toJsonString(): String = joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
