package com.example.core.database

import kotlinx.coroutines.flow.Flow

class NovaPulseRepository(private val database: AppDatabase) {
    val taskDao: TaskDao = database.taskDao()
    val expenseDao: ExpenseDao = database.expenseDao()
    val notificationLogDao: NotificationLogDao = database.notificationLogDao()

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val upcomingTasks: Flow<List<TaskEntity>> = taskDao.getUpcomingTasks(5)
    val pendingTaskCount: Flow<Int> = taskDao.getPendingCount()
    val completedTaskCount: Flow<Int> = taskDao.getCompletedCount()

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val totalSpent: Flow<Double?> = expenseDao.getTotalSpent()
    val recentExpenses: Flow<List<ExpenseEntity>> = expenseDao.getRecentExpenses(10)

    val recentLogs: Flow<List<NotificationLogEntity>> = notificationLogDao.getRecentLogs(50)
    val totalIntercepted: Flow<Int> = notificationLogDao.getTotalInterceptedCount()
    val regexMatched: Flow<Int> = notificationLogDao.getRegexMatchedCount()

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insert(task)

    suspend fun setTaskCompleted(id: Long, completed: Boolean) {
        taskDao.setCompleted(id, completed)
    }

    suspend fun deleteTask(id: Long) {
        taskDao.deleteById(id)
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insert(expense)

    suspend fun deleteExpense(id: Long) {
        expenseDao.deleteById(id)
    }

    suspend fun insertLog(log: NotificationLogEntity): Long = notificationLogDao.insert(log)

    suspend fun deleteLog(id: Long) {
        notificationLogDao.deleteById(id)
    }

    suspend fun clearLogs() {
        notificationLogDao.clearAll()
    }

    suspend fun clearAllData() {
        taskDao.clearAll()
        expenseDao.clearAll()
        notificationLogDao.clearAll()
    }
}
