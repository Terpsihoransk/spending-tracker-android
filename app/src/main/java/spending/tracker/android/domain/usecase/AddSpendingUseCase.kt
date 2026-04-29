package spending.tracker.android.domain.usecase

import spending.tracker.android.domain.model.Spending
import spending.tracker.android.domain.repository.SpendingRepository

class AddSpendingUseCase(private val repository: SpendingRepository) {
    suspend operator fun invoke(spending: Spending): Result<Spending> = repository.addSpending(spending)
}
