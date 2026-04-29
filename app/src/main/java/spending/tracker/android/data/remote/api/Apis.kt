package spending.tracker.android.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import spending.tracker.android.data.remote.dto.CategoryRequest
import spending.tracker.android.data.remote.dto.CategoryResponse
import spending.tracker.android.data.remote.dto.SpendingRequest
import spending.tracker.android.data.remote.dto.SpendingResponse
import spending.tracker.android.data.remote.dto.SubCategoryRequest
import spending.tracker.android.data.remote.dto.SubCategoryResponse
import spending.tracker.android.data.remote.dto.UserRequest
import spending.tracker.android.data.remote.dto.UserResponse

private const val HEADER_USER_EMAIL = "X-User-Email"

private fun HttpRequestBuilder.userEmail(email: String) {
    header(HEADER_USER_EMAIL, email)
}

// ============ Spending ============

class SpendingApi(private val client: HttpClient) {
    suspend fun getSpendings(userEmail: String): List<SpendingResponse> =
        client.get("spending") { userEmail(userEmail) }.body()

    suspend fun getSpending(userEmail: String, id: Long): SpendingResponse =
        client.get("spending/$id") { userEmail(userEmail) }.body()

    suspend fun createSpending(userEmail: String, request: SpendingRequest): SpendingResponse =
        client.post("spending") {
            userEmail(userEmail)
            setBody(request)
        }.body()

    suspend fun updateSpending(userEmail: String, id: Long, request: SpendingRequest): SpendingResponse =
        client.put("spending/$id") {
            userEmail(userEmail)
            setBody(request)
        }.body()

    suspend fun deleteSpending(userEmail: String, id: Long) {
        client.delete("spending/$id") { userEmail(userEmail) }
    }
}

// ============ Category ============

class CategoryApi(private val client: HttpClient) {
    suspend fun getCategories(userEmail: String): List<CategoryResponse> =
        client.get("categories") { userEmail(userEmail) }.body()

    suspend fun getCategory(userEmail: String, id: Long): CategoryResponse =
        client.get("categories/$id") { userEmail(userEmail) }.body()

    suspend fun createCategory(userEmail: String, request: CategoryRequest): CategoryResponse =
        client.post("categories") {
            userEmail(userEmail)
            setBody(request)
        }.body()

    suspend fun updateCategory(userEmail: String, id: Long, request: CategoryRequest): CategoryResponse =
        client.put("categories/$id") {
            userEmail(userEmail)
            setBody(request)
        }.body()

    suspend fun deleteCategory(userEmail: String, id: Long) {
        client.delete("categories/$id") { userEmail(userEmail) }
    }
}

// ============ SubCategory ============

class SubCategoryApi(private val client: HttpClient) {
    suspend fun getSubCategories(userEmail: String, categoryId: Long): List<SubCategoryResponse> =
        client.get("subcategories") {
            userEmail(userEmail)
            parameter("categoryId", categoryId)
        }.body()

    suspend fun createSubCategory(userEmail: String, request: SubCategoryRequest): SubCategoryResponse =
        client.post("subcategories") {
            userEmail(userEmail)
            setBody(request)
        }.body()

    suspend fun updateSubCategory(userEmail: String, id: Long, request: SubCategoryRequest): SubCategoryResponse =
        client.put("subcategories/$id") {
            userEmail(userEmail)
            setBody(request)
        }.body()

    suspend fun deleteSubCategory(userEmail: String, id: Long) {
        client.delete("subcategories/$id") { userEmail(userEmail) }
    }
}

// ============ User ============

class UserApi(private val client: HttpClient) {
    /**
     * Возвращает пользователя по email или `null`, если пользователь не найден.
     *
     * На бэке GET `/user` возвращает список всех пользователей,
     * поэтому ищем нужного вручную по email.
     */
    suspend fun getUserByEmail(email: String): UserResponse? = try {
        val all = client.get("user").body<List<UserResponse>>()
        all.firstOrNull { it.email.equals(email, ignoreCase = true) }
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.NotFound) null else throw e
    }

    /** Создать пользователя. */
    suspend fun createUser(request: UserRequest): UserResponse =
        client.post("user") { setBody(request) }.body()
}
