package com.hxlxz.tomatoclock

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    @Suppress("unused")
    abstract fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler

    companion object {
        @Provides
        @Singleton
        fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
            return SettingsDataStore(context)
        }

        @Provides
        @Singleton
        fun provideCoroutineDispatcher(): CoroutineDispatcher {
            // 【CR-3修复】计时轮询（startCountdown while(true)）是 CPU 调度任务，
            // 不应占用 IO dispatcher 线程池（上限64个）。改用 Default dispatcher。
            return Dispatchers.Default
        }

        @Provides
        @Singleton
        fun provideApplicationScope(dispatcher: CoroutineDispatcher): CoroutineScope {
            return CoroutineScope(SupervisorJob() + dispatcher)
        }
    }
}
