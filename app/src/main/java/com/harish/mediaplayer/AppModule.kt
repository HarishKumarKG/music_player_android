package com.harish.mediaplayer

import com.harish.mediaplayer.screen.main.repository.CounterRepository
import com.harish.mediaplayer.screen.main.repository.CounterRepositoryImpl
import com.harish.mediaplayer.screen.main.usecase.CounterUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideCounterRepository(): CounterRepository {
        return CounterRepositoryImpl()
    }

    @Provides
    fun provideCounterUseCase(repository: CounterRepository): CounterUseCase {
        return CounterUseCase(repository)
    }
}