package de.lukaspieper.crypto.pink

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
public class ExampleInstrumentedTest {
    @Test
    public fun useAppContext() {
        val stringFromJNI = NativeLib().stringFromJNI()

        assertEquals("Hello from C++", stringFromJNI)
    }
}