package spending.tracker.android.domain.model

data class User(
    val id: Long,
    val email: String,
    val googleSheetsId: String?
)
