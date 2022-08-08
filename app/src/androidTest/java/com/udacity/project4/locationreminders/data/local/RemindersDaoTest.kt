package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private val tenReminderList = arrayListOf<ReminderDTO>()

    @Before
    fun build_database_and_create_fake_data() {
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
    }

    @After
    fun close_database() {
        database.close()
    }

    @Test
    fun save_reminders_into_database() = runBlockingTest {
        tenReminderList.forEach {
            database.reminderDao().saveReminder(it)
        }

        val remindersFromDb = database.reminderDao().getReminders()

        assertThat(remindersFromDb, `is`(notNullValue()))
        assertThat(remindersFromDb.size, `is`(10))
    }

    @Test
    fun assert_reminders_on_database() = runBlockingTest {
        tenReminderList.forEach {
            database.reminderDao().saveReminder(it)
        }

        val allDatabaseReminders = database.reminderDao().getReminders()
        val randomReminderPosition = (1..10).random()
        val randomReminder = allDatabaseReminders[randomReminderPosition - 1]

        assertThat(randomReminder, notNullValue())
        assertThat(randomReminder.latitude, `is`(randomReminderPosition + .987))
    }

    @Test
    fun delete_all_from_database() = runBlockingTest {
        tenReminderList.forEach {
            database.reminderDao().saveReminder(it)
        }

        val allRemindersBeforeDelete = database.reminderDao().getReminders()
        assert(allRemindersBeforeDelete.isNotEmpty())

        database.reminderDao().deleteAllReminders()

        val allRemindersAfterDelete = database.reminderDao().getReminders()
        assertThat(allRemindersAfterDelete, `is`(emptyList()))
    }

}