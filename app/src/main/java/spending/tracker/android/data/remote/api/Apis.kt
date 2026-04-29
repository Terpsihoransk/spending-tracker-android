package spending.tracker.android.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import spending.tracker.android.data.remote.dto.CategoryDto
import spending.tracker.android.data.remote.dto.CreateSpendingRequest
import spending.tracker.android.data.remote.dto.SpendingDto
import spending.tracker.android.data.remote.dto.SubCategoryDto
import spending.tracker.android.data.remote.dto.UserDto

class SpendingApi(private val client: HttpClient) {
    suspend fun getSpendings(userId: Long): List<SpendingDto> =
        client.get("spending") {
            parameter("userId", userId)
        }.body()

    suspend fun createSpending(request: CreateSpendingRequest): SpendingDto =
        client.post("spending") {
            setBody(request)
        }.body()

    suspend fun deleteSpending(id: Long) {
        client.delete("spending/$id")
    }
}

class CategoryApi(private val client: HttpClient) {
    suspend fun getCategories(userId: Long): List<CategoryDto> =
        client.get("categories") {
            parameter("userId", userId)
        }.body()

    suspend fun getSubCategories(categoryId: Long): List<SubCategoryDto> =
        client.get("subcategories") {
            parameter("categoryId", categoryId)
        }.body()
}

class UserApi(private val client: HttpClient) {
    /**
     * Возвращает пользователя по email или `null`, если пользователь не найден (HTTP 404).
     * Любые другие ошибки пробрасываются наверх.
     */
    suspend fun getUser(email: String): UserDto? = try {
        client.get("users") {
            parameter("email", email)
        }.body<UserDto>()
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.NotFound) null else throw e
    }
}
