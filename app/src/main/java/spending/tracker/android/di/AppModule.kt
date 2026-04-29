package spending.tracker.android.di

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import spending.tracker.android.data.local.database.AppDatabase
import spending.tracker.android.data.remote.HttpClientFactory
import spending.tracker.android.data.remote.api.CategoryApi
import spending.tracker.android.data.remote.api.SpendingApi
import spending.tracker.android.data.remote.api.UserApi
import spending.tracker.android.data.repository.CategoryRepositoryImpl
import spending.tracker.android.data.repository.SpendingRepositoryImpl
import spending.tracker.android.data.repository.UserRepositoryImpl
import spending.tracker.android.domain.repository.CategoryRepository
import spending.tracker.android.domain.repository.SpendingRepository
import spending.tracker.android.domain.repository.UserRepository
import spending.tracker.android.domain.usecase.AddSpendingUseCase
import spending.tracker.android.domain.usecase.ClearUserUseCase
import spending.tracker.android.domain.usecase.DeleteSpendingUseCase
import spending.tracker.android.domain.usecase.ObserveCategoriesUseCase
import spending.tracker.android.domain.usecase.ObserveCurrentUserUseCase
import spending.tracker.android.domain.usecase.ObserveSpendingsUseCase
import spending.tracker.android.domain.usecase.ObserveSubCategoriesUseCase
import spending.tracker.android.domain.usecase.RefreshCategoriesUseCase
import spending.tracker.android.domain.usecase.RefreshSpendingsUseCase
import spending.tracker.android.domain.usecase.RefreshSubCategoriesUseCase
import spending.tracker.android.domain.usecase.SyncUserUseCase

val appModule = module {
    // --- Database ---
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }
    single { get<AppDatabase>().spendingDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().userDao() }

    // --- Network ---
    single { HttpClientFactory.create() }
    single { SpendingApi(get()) }
    single { CategoryApi(get()) }
    single { UserApi(get()) }

    // --- Repositories ---
    single<SpendingRepository> { SpendingRepositoryImpl(get(), get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }

    // --- UseCases ---
    factoryOf(::ObserveSpendingsUseCase)
    factoryOf(::RefreshSpendingsUseCase)
    factoryOf(::AddSpendingUseCase)
    factoryOf(::DeleteSpendingUseCase)

    factoryOf(::ObserveCategoriesUseCase)
    factoryOf(::RefreshCategoriesUseCase)
    factoryOf(::ObserveSubCategoriesUseCase)
    factoryOf(::RefreshSubCategoriesUseCase)

    factoryOf(::ObserveCurrentUserUseCase)
    factoryOf(::SyncUserUseCase)
    factoryOf(::ClearUserUseCase)

    // --- ViewModels будут добавлены на этапе 7 ---
}
