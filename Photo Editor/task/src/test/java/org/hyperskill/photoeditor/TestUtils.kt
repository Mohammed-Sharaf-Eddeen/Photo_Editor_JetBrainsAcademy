package org.hyperskill.photoeditor

import android.app.Activity
import android.view.View
import org.junit.Assert
import kotlin.math.abs


object TestUtils {

    inline fun <reified T> Activity.findViewByString(idString: String): T {
        val id = this.resources.getIdentifier(idString, "id", this.packageName)
        val view: View? = this.findViewById(id)

        val idNotFoundMessage = "View with id \"$idString\" was not found"
        val wrongClassMessage = "View with id \"$idString\" is not from expected class. " +
                "Expected ${T::class.java.simpleName} found ${view?.javaClass?.simpleName}"

        Assert.assertNotNull(idNotFoundMessage, view)
        Assert.assertTrue(wrongClassMessage, view is T)

        return view as T
    }

    fun assertColorsValues(message: String, expected: Triple<Int, Int, Int>, actual: Triple<Int, Int, Int>, marginError: Int) {
        val messageWrongValuesFormat = "%s expected: <(%d, %d, %d)> actual: <(%d, %d, %d)>"
        val (expectedRed, expectedGreen, expectedBlue) = expected
        val (actualRed, actualGreen, actualBlue) = actual

        val messageWrongValues = messageWrongValuesFormat.format( message,
            expectedRed, expectedGreen, expectedBlue,
            actualRed, actualGreen, actualBlue
        )

        Assert.assertTrue(messageWrongValues, abs(expectedRed - actualRed) <= marginError)
        Assert.assertTrue(messageWrongValues, abs(expectedGreen - actualGreen) <= marginError)
        Assert.assertTrue(messageWrongValues, abs(expectedBlue - actualBlue) <= marginError)
    }
}
