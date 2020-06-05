package org.personal.coupleapp.data

data class PlanData(
    val id: Int?,
    val couple_id:Int,
    val is_public: Boolean,
    val title: String,
    val memo:String?,
    val date_type: String,
    val location: String?,
    val start_time: Long,
    val end_time: Long,
    val repeat_type: String,
    val notification_time: String?
)