package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import kotlin.coroutines.coroutineContext

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private val tenReminderList = arrayListOf<ReminderDTO>()

    @Before
    fun build_database_fake_data_and_repository() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        for (i in 1..10) {
            tenReminderList.add(
                ReminderDTO(
                    "Test $i",
                    "Description $i",
                    "Location Test $i",
                    i + .987,
                    i + .654
                )
            )
        }

        remindersLocalRepository = RemindersLocalRepository(database.reminderDao())
    }

    @After
    fun close_database() {
        database.close()
    }

    @Test
    fun repository_save_reminders() = runBlocking {
        tenReminderList.forEach {
            remindersLocalRepository.saveReminder(it)
        }

        val remindersFromRepository = remindersLocalRepository.getReminders()

        assertThat(remindersFromRepository, `is`(notNullValue()))
        assertThat(remindersFromRepository is Result.Success, `is`(true))
    }

    @Test
    fun repository_save_and_get_reminder() = runBlocking {
        tenReminderList.forEach {
            remindersLocalRepository.saveReminder(it)
        }

        val reminderFiveStatus = remindersLocalRepository.getReminder(tenReminderList[5].id)

        assertThat(reminderFiveStatus, `is`(notNullValue()))
        assertThat(reminderFiveStatus is Result.Success, `is`(true))

        val reminderFive = (reminderFiveStatus as Result.Success).data
        assertThat(reminderFive, `is`(notNullValue()))
        assertThat(reminderFive.latitude, `is`(6 + .987))
    }

    @Test
    fun repository_delete_all_reminders() = runBlocking {
        tenReminderList.forEach {
            remindersLocalRepository.saveReminder(it)
        }

        remindersLocalRepository.deleteAllReminders()

        val remindersFromRepositoryAfterDelete = remindersLocalRepository.getReminder(tenReminderList[2].id)

        assertThat(remindersFromRepositoryAfterDelete is Result.Error, `is`(true))

        val message = (remindersFromRepositoryAfterDelete as Result.Error).message
        assertThat(message, `is`("Reminder not found!"))
    }
}