package com.tippingpoint.pedastudio.api

import com.tippingpoint.pedastudio.data.TeacherAccount

data class PlanGenerationResult(
    val plan: org.json.JSONObject,
    val account: TeacherAccount?,
)
