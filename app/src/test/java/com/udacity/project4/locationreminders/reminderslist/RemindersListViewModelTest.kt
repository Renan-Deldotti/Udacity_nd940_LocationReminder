package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    @Before
    fun build_repository() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @Test
    fun get_reminders_with_error_message() = runBlockingTest {
        fakeDataSource.setShouldReturnError(true)
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showSnackBar.value, `is`("testing 123: error"))
    }

    @Test
    fun check_no_data() = runBlockingTest {
        fakeDataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showNoData.value, `is`(true))
    }

    @Test
    fun check_with_data() = runBlockingTest {
        val reminder = ReminderDTO("Test", "test","test", 0.123, 0.456)
        fakeDataSource.saveReminder(reminder)
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showNoData.value, `is`(false))
    }

    @Test
    fun check_load_reminders() = runBlockingTest {
        val reminder = ReminderDTO("Test", "test","test", 0.123, 0.456)
        fakeDataSource.deleteAllReminders()
        fakeDataSource.saveReminder(reminder)
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showLoading.value, `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(remindersListViewModel.showNoData.value, `is`(false))
    }

    @After
    fun close_and_finish() {
        stopKoin()
    }

    class MainCoroutineRule(private val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher())
        : TestWatcher(), TestCoroutineScope by TestCoroutineScope(dispatcher){
        override fun starting(description: Description) {
            super.starting(description)
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            super.finished(description)
            cleanupTestCoroutines()
            Dispatchers.resetMain()
        }
    }
}