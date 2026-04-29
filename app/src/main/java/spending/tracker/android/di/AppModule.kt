package spending.tracker.android.di

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import spending.tracker.android.data.local.database.AppDatabase
import spending.tracker.android.data.local.prefs.SessionManager
import spending.tracker.android.data.remote.HttpClientFactory
import spending.tracker.android.data.remote.api.CategoryApi
import spending.tracker.android.data.remote.api.SpendingApi
import spending.tracker.android.data.remote.api.SubCategoryApi
import spending.tracker.android.data.remote.api.UserApi
import spending.tracker.android.data.repository.CategoryRepositoryImpl
import spending.tracker.android.data.repository.SpendingRepositoryImpl
import spending.tracker.android.data.repository.UserRepositoryImpl
import spending.tracker.android.domain.repository.CategoryRepository
import spending.tracker.android.domain.repository.SpendingRepository
import spending.tracker.android.domain.repository.UserRepository
import spending.tracker.android.domain.usecase.AddCategoryUseCase
import spending.tracker.android.domain.usecase.AddSpendingUseCase
import spending.tracker.android.domain.usecase.AddSubCategoryUseCase
import spending.tracker.android.domain.usecase.ClearUserUseCase
import spending.tracker.android.domain.usecase.DeleteCategoryUseCase
import spending.tracker.android.domain.usecase.DeleteSpendingUseCase
import spending.tracker.android.domain.usecase.DeleteSubCategoryUseCase
import spending.tracker.android.domain.usecase.ObserveCategoriesUseCase
import spending.tracker.android.domain.usecase.ObserveCurrentUserUseCase
import spending.tracker.android.domain.usecase.ObserveSpendingsUseCase
import spending.tracker.android.domain.usecase.ObserveSubCategoriesUseCase
import spending.tracker.android.domain.usecase.RefreshCategoriesUseCase
import spending.tracker.android.domain.usecase.RefreshSpendingsUseCase
import spending.tracker.android.domain.usecase.RefreshSubCategoriesUseCase
import spending.tracker.android.domain.usecase.SyncUserUseCase
import spending.tracker.android.domain.usecase.UpdateCategoryUseCase
import spending.tracker.android.domain.usecase.UpdateSpendingUseCase
import spending.tracker.android.domain.usecase.UpdateSubCategoryUseCase
import spending.tracker.android.presentation.screen.categories.CategoriesViewModel
import spending.tracker.android.presentation.screen.email.EmailEntryViewModel
import spending.tracker.android.presentation.screen.profile.ProfileViewModel
import spending.tracker.android.presentation.screen.spendings.AddSpendingViewModel
import spending.tracker.android.presentation.screen.spendings.SpendingsViewModel
import spending.tracker.android.presentation.screen.summary.SummaryViewModel

val appModule = module {
    // --- Database ---
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            // Пока MVP: при смене схемы (email-identity, категории/подкатегории)
            // безопасно пересоздаём БД, т.к. источник истины — backend.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().spendingDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().userDao() }

    // --- Preferences / Session ---
    single { SessionManager(androidContext()) }

    // --- Network ---
    single { HttpClientFactory.create() }
    single { SpendingApi(get()) }
    single { CategoryApi(get()) }
    single { SubCategoryApi(get()) }
    single { UserApi(get()) }

    // --- Repositories ---
    single<SpendingRepository> { SpendingRepositoryImpl(get(), get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get(), get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get(), get()) }

    // --- UseCases: Spending ---
    factoryOf(::ObserveSpendingsUseCase)
    factoryOf(::RefreshSpendingsUseCase)
    factoryOf(::AddSpendingUseCase)
    factoryOf(::UpdateSpendingUseCase)
    factoryOf(::DeleteSpendingUseCase)

    // --- UseCases: Category ---
    factoryOf(::ObserveCategoriesUseCase)
    factoryOf(::RefreshCategoriesUseCase)
    factoryOf(::AddCategoryUseCase)
    factoryOf(::UpdateCategoryUseCase)
    factoryOf(::DeleteCategoryUseCase)

    // --- UseCases: SubCategory ---
    factoryOf(::ObserveSubCategoriesUseCase)
    factoryOf(::RefreshSubCategoriesUseCase)
    factoryOf(::AddSubCategoryUseCase)
    factoryOf(::UpdateSubCategoryUseCase)
    factoryOf(::DeleteSubCategoryUseCase)

    // --- UseCases: User ---
    factoryOf(::ObserveCurrentUserUseCase)
    factoryOf(::SyncUserUseCase)
    factoryOf(::ClearUserUseCase)

    // --- ViewModels ---
    viewModelOf(::SpendingsViewModel)
    // AddSpendingViewModel принимает опциональный spendingId: Long? (режим Edit/Add).
    viewModel { (spendingId: Long?) ->
        AddSpendingViewModel(
            spendingId = spendingId,
            observeCurrentUser = get(),
            observeCategories = get(),
            observeSubCategories = get(),
            observeSpendings = get(),
            addSpending = get(),
            updateSpending = get(),
        )
    }
    viewModelOf(::CategoriesViewModel)
    viewModelOf(::SummaryViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::EmailEntryViewModel)
}
