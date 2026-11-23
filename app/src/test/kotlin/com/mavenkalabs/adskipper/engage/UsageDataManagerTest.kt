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
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

        Mockito.doReturn(mockContext)
            .whenever(mockContext).applicationContext
        Mockito.doReturn(UsageDataManager::class.java.packageName)
            .whenever(mockContext).packageName
        Mockito.doReturn(USAGE_DATA_PREF_NAME)
            .whenever(mockContext).getString(eq(R.string.pref_key_usage_data))
        Mockito.doReturn(mockSharedPreferences).whenever(mockContext)
            .getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())
        Mockito.doReturn(mockSharedPreferencesEditor).whenever(mockSharedPreferences)
            .edit()

        UsageDataManager.init(mockContext)
    }

    @After
    fun tearDown() {
        UsageDataManager.reset()

        mocksCloseable.close()
    }

    @Test
    fun verifyFailureWithNoInit() {
        UsageDataManager.reset()

        assertFailsWith<IllegalStateException>() {
            UsageDataManager.logAd()
        }
    }

    @Test
    fun verifyWithNoExistingUsageData() {
        assertEquals(
            0,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIVE)
        )
    }

    @Test
    fun verifyWithExistingBadUsageData() {
        Mockito.doReturn("").whenever(mockSharedPreferences)
            .getString(eq(USAGE_DATA_PREF_NAME), ArgumentMatchers.isNull<String>())

        assertEquals(
            0,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIVE)
        )
    }


    @Test
    fun verifySaveAndGetUsageData() {
        UsageDataManager.logSkip()
        UsageDataManager.logAd()

        assertEquals(
            1,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIVE)
        )
        assertEquals(
            1,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIVE)
        )
        assertEquals(
            0,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_S1)
        )
        assertEquals(
            0,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_S1)
        )
        assertEquals(
            1,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME)
        )
        assertEquals(
            1,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIFE_TIME)
        )

        UsageDataManager.takeSnapshot()

        assertEquals(
            0,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIVE)
        )
        assertEquals(
            0,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIVE)
        )
        assertEquals(
            1,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_S1)
        )
        assertEquals(1, UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_S1))
        assertEquals(
            1,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME)
        )
        assertEquals(
            1,
            UsageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIFE_TIME)
        )
    }
}