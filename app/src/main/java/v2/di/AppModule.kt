package v2.di

import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import v2.data.repository.DeviceApprovalRepositoryImpl
import v2.data.repository.QRCodeRepositoryImpl
import v2.domain.repository.DeviceApprovalRepository
import v2.domain.repository.QRCodeRepository
import v2.utils.DataSeeder
import v2.utils.UserPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi
            .Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    @Provides
    @Singleton
    fun provideQRCodeRepository(
        firestore: FirebaseFirestore,
        moshi: Moshi,
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
    ): QRCodeRepository = QRCodeRepositoryImpl(firestore, moshi, context)

    @Provides
    @Singleton
    fun provideDataSeeder(repository: QRCodeRepository): DataSeeder = DataSeeder(repository)

    @Provides
    @Singleton
    fun provideUserPreferences(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
    ): UserPreferences = UserPreferences(context)

    @Provides
    @Singleton
    fun provideDeviceApprovalRepository(firestore: FirebaseFirestore): DeviceApprovalRepository = DeviceApprovalRepositoryImpl(firestore)
} 
