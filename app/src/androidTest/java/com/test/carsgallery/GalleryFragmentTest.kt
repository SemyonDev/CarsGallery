package com.test.carsgallery

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.test.carsgallery.di.DataModule
import com.test.carsgallery.domain.exception.NetworkException
import com.test.carsgallery.domain.model.ImageItem
import com.test.carsgallery.domain.repository.GalleryRepository
import com.test.carsgallery.presentation.MainActivity
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented UI tests for [com.test.carsgallery.presentation.screens.gallery.GalleryFragment].
 *
 * [UninstallModules] removes the production [DataModule] so that the [BindValue]-annotated
 * [repository] fake is the only binding for [GalleryRepository] in the test component.
 *
 * ### Why [FakeGalleryRepository] instead of MockK
 * MockK requires a JVMTI native agent (`libmockkjvmtiagent.so`) that is unavailable on
 * Android's ART runtime. The agent is loaded at class-init time, which causes
 * [io.mockk.proxy.MockKAgentException] before any test code runs — even though
 * [GalleryRepository] is an interface and wouldn't need inline mocking. A hand-written
 * [FakeGalleryRepository] avoids the issue entirely.
 *
 * Run with: `./gradlew connectedDebugAndroidTest`
 */
@HiltAndroidTest
@UninstallModules(DataModule::class)
@RunWith(AndroidJUnit4::class)
class GalleryFragmentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    @JvmField
    val repository: GalleryRepository = FakeGalleryRepository()

    private val fake get() = repository as FakeGalleryRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // ── Success state ────────────────────────────────────────────────────────

    @Test
    fun galleryShowsRecyclerViewOnSuccess() {
        fake.enqueueSuccess(fakeItems())

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
    }

    @Test
    fun galleryHidesErrorContainerOnSuccess() {
        fake.enqueueSuccess(fakeItems())

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.errorContainer)).check(
            matches(org.hamcrest.Matchers.not(isDisplayed()))
        )
    }

    // ── Error state ──────────────────────────────────────────────────────────

    @Test
    fun galleryShowsNetworkErrorMessage() {
        fake.enqueueFailure(NetworkException(IOException("no connection")))

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.errorContainer)).check(matches(isDisplayed()))
        onView(withId(R.id.viewError)).check(matches(withText(R.string.error_network)))
    }

    @Test
    fun galleryShowsRetryButtonOnError() {
        fake.enqueueFailure(RuntimeException("unexpected"))

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.buttonRetry)).check(matches(isDisplayed()))
    }

    @Test
    fun retryButtonTriggersReload() {
        // First call fails, second call (triggered by retry button) succeeds.
        fake.enqueueFailure(RuntimeException("transient"))
        fake.enqueueSuccess(fakeItems())

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.buttonRetry)).perform(click())

        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
    }

    // ── Pull-to-refresh ──────────────────────────────────────────────────────

    @Test
    fun pullToRefreshReloadsGallery() {
        fake.enqueueSuccess(fakeItems())
        fake.enqueueSuccess(fakeItems()) // second call after swipe

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.swipeRefreshLayout)).perform(swipeDown())

        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    @Test
    fun clickingItemNavigatesToDetailScreen() {
        fake.enqueueSuccess(fakeItems())

        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.recyclerView))
            .perform(
                androidx.test.espresso.contrib.RecyclerViewActions
                    .actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(
                        0, click()
                    )
            )

        onView(withId(R.id.textViewId)).check(matches(isDisplayed()))
    }

    // ── Cache invalidation ───────────────────────────────────────────────────

    @Test
    fun clearCacheMenuItemIsVisibleInOverflow() {
        fake.enqueueSuccess(fakeItems())

        ActivityScenario.launch(MainActivity::class.java)

        // Both menu items have showAsAction="never" so they live in the overflow menu.
        // Open the overflow by clicking the standalone Toolbar's "More options" button,
        // then verify the item is present by its title string.
        onView(withContentDescription("More options")).perform(click())
        onView(withText(R.string.clear_cache)).check(matches(isDisplayed()))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun fakeItems() = listOf(
        ImageItem(id = "id_1", imageUrl = "https://example.com/1.jpg"),
        ImageItem(id = "id_2", imageUrl = "https://example.com/2.jpg"),
        ImageItem(id = "id_3", imageUrl = "https://example.com/3.jpg"),
    )
}
