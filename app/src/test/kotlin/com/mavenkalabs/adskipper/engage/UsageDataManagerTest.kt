package com.mavenkalabs.adskipper.engage

import android.content.Context
import android.content.SharedPreferences
import com.mavenkalabs.adskipper.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class UsageDataManagerTest {
    companion object {
        private const val USAGE_DATA_PREF_NAME = "usage_data"

    }
    @Mock
    private lateinit var mockContext : Context
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    @Mock
    private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var mocksCloseable: AutoCloseable

    @Before
    fun setUp()  {
        mocksCloseable = MockitoAnnotations.openMocks(this)

        Mockito.doReturn(UsageDataManager::class.java.packageName)
            .whenever(mockContext).packageName
        Mockito.doReturn(USAGE_DATA_PREF_NAME)
            .whenever(mockContext).getString(eq(R.string.pref_key_usage_data))
        Mockito.doReturn(mockSharedPreferences).whenever(mockContext)
            .getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())
        Mockito.doReturn(mockSharedPreferencesEditor).whenever(mockSharedPreferences)
            .edit()
    }

    @After
    fun tearDown() {
        mocksCloseable.close()
    }

    @Test
    fun verifyWithNoExistingUsageData() {
        assertNotNull(UsageDataManager(mockContext))
    }

    @Test
    fun verifyWithExistingBadUsageData() {
        Mockito.doReturn("").whenever(mockSharedPreferences)
            .getString(eq(USAGE_DATA_PREF_NAME), ArgumentMatchers.isNull<String>())

        val usageDataManager = UsageDataManager(mockContext)

        assertNotNull(usageDataManager)
        assertEquals(
            0,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIVE)
        )
    }


    @Test
    fun verifySaveAndGetUsageData() {
        val usageDataManager = UsageDataManager(mockContext)
        usageDataManager.logSkip(mockContext)
        usageDataManager.logAd(mockContext)

        assertEquals(
            1,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIVE)
        )
        assertEquals(
            1,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIVE)
        )
        assertEquals(
            0,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_S1)
        )
        assertEquals(
            0,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_S1)
        )
        assertEquals(
            0,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME)
        )
        assertEquals(
            0,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIFE_TIME)
        )

        usageDataManager.takeSnapshot(mockContext)

        assertEquals(
            0,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIVE)
        )
        assertEquals(
            0,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIVE)
        )
        assertEquals(
            1,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_S1)
        )
        assertEquals(1, usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_S1))
        assertEquals(
            1,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME)
        )
        assertEquals(
            1,
            usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIFE_TIME)
        )

        val usageDataStringCaptor = argumentCaptor<String>()
        verify(mockSharedPreferencesEditor, times(3)).putString(eq(USAGE_DATA_PREF_NAME), usageDataStringCaptor.capture(),)

        Mockito.doReturn(usageDataStringCaptor.lastValue).whenever(mockSharedPreferences)
            .getString(eq(USAGE_DATA_PREF_NAME), ArgumentMatchers.isNull<String>())

        val usageDataManagerReloaded = UsageDataManager(mockContext)

        assertEquals(
            0,
            usageDataManagerReloaded.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIVE)
        )
        assertEquals(
            0,
            usageDataManagerReloaded.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIVE)
        )
        assertEquals(
            1,
            usageDataManagerReloaded.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_S1)
        )
        assertEquals(
            1,
            usageDataManagerReloaded.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_S1)
        )
        assertEquals(
            1,
            usageDataManagerReloaded.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME)
        )
        assertEquals(
            1,
            usageDataManagerReloaded.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIFE_TIME)
        )
    }
}