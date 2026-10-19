package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DialerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dialer", appName)
    }

    @Test
    fun testT9SearchMatches() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = DialerRepository(context)

        // 666 -> Mom (6=M, 6=O, 6=M)
        val momMatches = repository.searchT9("666")
        assertTrue(momMatches.any { it.name == "Mom" })

        // 2539 -> Alex (2=A, 5=L, 3=E, 9=X)
        val alexMatches = repository.searchT9("2539")
        assertTrue(alexMatches.any { it.name.startsWith("Alex") })
    }
}
