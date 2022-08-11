package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: ReminderDataSource

    @Before
    fun preload() {
        stopKoin()
        val module = module {
            viewModel { RemindersListViewModel(getApplicationContext(), get()) }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(getApplicationContext()) }
        }
        startKoin {
            androidContext(getApplicationContext())
            modules(module)
        }
        repository = get().koin.get()
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun goFrom_add_to_Save() {
        val testScene = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val mockNavController = mock(NavController::class.java)
        testScene.onFragment {Navigation.setViewNavController(it.view!!, mockNavController)}
        onView(withId(R.id.addReminderFAB)).perform(click())
        verify(mockNavController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun check_data_displayed_on_screen() {
        val tempReminder = ReminderDTO("Reminder", "reminder", "reminder", 0.123, 4.567)
        runBlocking {
            repository.saveReminder(tempReminder)
        }
        launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        onView(withText(tempReminder.title)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
    }

    @Test
    fun check_to_error_messages() {
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }


}