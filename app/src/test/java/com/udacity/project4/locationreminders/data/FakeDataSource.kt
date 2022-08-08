package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO> = mutableListOf()): ReminderDataSource {

    private var shouldReturnError = false
    fun setShouldReturnError(shouldReturn: Boolean) {
        this.shouldReturnError = shouldReturn
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (shouldReturnError) {
            Result.Error("testing 123: error")
        } else {
            Result.Success(ArrayList(reminders))
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (shouldReturnError) {
            Result.Error("testing 123: error")
        } else {
            val reminder = reminders.find { rem ->
                rem.id == id
            }
            if (null != reminder) {
                Result.Success(reminder)
            } else {
                Result.Error("testing 123: error")
            }
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}