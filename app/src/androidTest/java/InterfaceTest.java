import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.Surface;
import android.widget.ImageView;

import com.iristick.camera.template.R;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.Until;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class InterfaceTest {
    private UiDevice device;

    @Before
    public void setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.pressHome();
        final String launcherPackage = device.getLauncherPackageName();
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), 1000);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.iristick.camera.template");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        device.wait(Until.hasObject(By.pkg("com.iristick.camera.template").depth(0)), 5000);
    }

    @Test
    public void testRedSquare() throws UiObjectNotFoundException {
        UiObject imageView = device.findObject(new UiSelector().resourceId("com.iristick.camera.template:id/image_view"));
        Rect bounds = imageView.getVisibleBounds();
        int centerX = bounds.centerX();
        int centerY = bounds.centerY();
        Bitmap screenshot = getScreenshot();
        int pixelColor = screenshot.getPixel(centerX, centerY);
        assertEquals("The pixel color is not red", Color.RED, pixelColor);
    }

    @Test
    public void testInterfaceAfterClick() throws UiObjectNotFoundException {
        UiObject imageView = device.findObject(new UiSelector().resourceId("com.iristick.camera.template:id/image_view"));
        Rect bounds = imageView.getVisibleBounds();
        int centerX = bounds.centerX();
        int centerY = bounds.centerY();
        Bitmap screenshot = getScreenshot();
        int pixelColorBeforeClick = screenshot.getPixel(centerX, centerY);
        assertEquals("The pixel color is not red before click", Color.RED, pixelColorBeforeClick);
        device.click(centerX, centerY);
        screenshot = getScreenshot();
        int pixelColorAfterClick = screenshot.getPixel(centerX, centerY);
        assertNotEquals("The pixel color is still red after click", Color.RED, pixelColorAfterClick);
    }

    private Bitmap getScreenshot() {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        return uiAutomation.takeScreenshot();
    }

}
