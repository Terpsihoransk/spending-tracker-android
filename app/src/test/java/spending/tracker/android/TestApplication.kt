package spending.tracker.android

import android.app.Application

/**
 * Пустой Application для unit-тестов под Robolectric.
 *
 * В проде используется `SpendingTrackerApp`, который поднимает Koin,
 * но для DAO-тестов Koin не нужен, а повторный `startKoin` в пределах
 * одного JVM-процесса падает с `KoinApplicationAlreadyStartedException`.
 */
class TestApplication : Application()
