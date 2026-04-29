package spending.tracker.android

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import spending.tracker.android.di.appModule

class SpendingTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@SpendingTrackerApp)
            modules(appModule)
        }
    }
}
