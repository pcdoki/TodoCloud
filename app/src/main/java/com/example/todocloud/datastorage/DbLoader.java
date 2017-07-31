package com.example.todocloud.datastorage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.example.todocloud.data.Category;
import com.example.todocloud.data.List;
import com.example.todocloud.data.Todo;
import com.example.todocloud.data.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DbLoader {

	private SQLiteDatabase sqLiteDatabase;

  private final String[] todoColumns = new String[]{
      DbConstants.Todo.KEY_ROW_ID,
      DbConstants.Todo.KEY_TODO_ONLINE_ID,
      DbConstants.Todo.KEY_USER_ONLINE_ID,
      DbConstants.Todo.KEY_LIST_ONLINE_ID,
      DbConstants.Todo.KEY_TITLE,
      DbConstants.Todo.KEY_PRIORITY,
      DbConstants.Todo.KEY_DUE_DATE,
      DbConstants.Todo.KEY_REMINDER_DATETIME,
      DbConstants.Todo.KEY_DESCRIPTION,
      DbConstants.Todo.KEY_COMPLETED,
      DbConstants.Todo.KEY_ROW_VERSION,
      DbConstants.Todo.KEY_DELETED,
      DbConstants.Todo.KEY_DIRTY
  };
  private final String[] listColumns = new String[]{
      DbConstants.List.KEY_ROW_ID,
      DbConstants.List.KEY_LIST_ONLINE_ID,
      DbConstants.List.KEY_USER_ONLINE_ID,
      DbConstants.List.KEY_CATEGORY_ONLINE_ID,
      DbConstants.List.KEY_TITLE,
      DbConstants.List.KEY_ROW_VERSION,
      DbConstants.List.KEY_DELETED,
      DbConstants.List.KEY_DIRTY
  };
  private final String[] categoryColumns = new String[]{
      DbConstants.Category.KEY_ROW_ID,
      DbConstants.Category.KEY_CATEGORY_ONLINE_ID,
      DbConstants.Category.KEY_USER_ONLINE_ID,
      DbConstants.Category.KEY_TITLE,
      DbConstants.Category.KEY_ROW_VERSION,
      DbConstants.Category.KEY_DELETED,
      DbConstants.Category.KEY_DIRTY
  };
  private final String todoOrderBy = DbConstants.Todo.KEY_DUE_DATE
      + ", "
      + DbConstants.Todo.KEY_PRIORITY
      + " DESC"
      + ", "
      + DbConstants.Todo.KEY_TITLE;

  public DbLoader() {
	}

  private void open() {
    DbHelper dbHelper = DbHelper.getInstance();
    sqLiteDatabase = dbHelper.getWritableDatabase();
  }

  public void reCreateDb() {
    open();
    dropTables();
    createTables();
  }

  private void dropTables() {
    sqLiteDatabase.execSQL(DbConstants.User.DATABASE_DROP);
    sqLiteDatabase.execSQL(DbConstants.Todo.DATABASE_DROP);
    sqLiteDatabase.execSQL(DbConstants.List.DATABASE_DROP);
    sqLiteDatabase.execSQL(DbConstants.Category.DATABASE_DROP);
  }

  private void createTables() {
    sqLiteDatabase.execSQL(DbConstants.User.DATABASE_CREATE);
    sqLiteDatabase.execSQL(DbConstants.Todo.DATABASE_CREATE);
    sqLiteDatabase.execSQL(DbConstants.List.DATABASE_CREATE);
    sqLiteDatabase.execSQL(DbConstants.Category.DATABASE_CREATE);
  }

  // ----------------- User table methods ------------------------------------------------- //

  /**
   * @return the created User's _id, if User created successfully, -1 otherwise.
   */
  public long createUser(User user) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.User.KEY_USER_ONLINE_ID, user.getUserOnlineId());
    contentValues.put(DbConstants.User.KEY_NAME, user.getName());
    contentValues.put(DbConstants.User.KEY_EMAIL, user.getEmail());
    contentValues.put(DbConstants.User.KEY_API_KEY, user.getApiKey());
    return sqLiteDatabase.insert(DbConstants.User.DATABASE_TABLE, null, contentValues);
  }

  public boolean updateUser(User user) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.User.KEY_USER_ONLINE_ID, user.getUserOnlineId());
    contentValues.put(DbConstants.User.KEY_NAME, user.getName());
    contentValues.put(DbConstants.User.KEY_EMAIL, user.getEmail());
    contentValues.put(DbConstants.User.KEY_API_KEY, user.getApiKey());
    String whereClause = DbConstants.User.KEY_ROW_ID + "=" + user.get_id();
    return sqLiteDatabase.update(
        DbConstants.User.DATABASE_TABLE,
        contentValues,
        whereClause,
        null
    ) > 0;
  }

  /**
   * @return the current User or null
   */
  public User getUser() {
    open();
    String[] columns = new String[] {
        DbConstants.User.KEY_ROW_ID,
        DbConstants.User.KEY_USER_ONLINE_ID,
        DbConstants.User.KEY_NAME,
        DbConstants.User.KEY_EMAIL,
        DbConstants.User.KEY_API_KEY
    };
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.User.DATABASE_TABLE,
        columns,
        null, null, null, null, null
    );
    if (cursor.moveToFirst()) {
      User user = new User(cursor);
      cursor.close();
      return user;
    } else {
      cursor.close();
      return null;
    }
  }

  /**
   * @return current User's userOnlineId or null
   */
  public String getUserOnlineId() {
    open();
    String[] columns = {DbConstants.User.KEY_USER_ONLINE_ID};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.User.DATABASE_TABLE,
        columns,
        null, null, null, null, null);
    if (cursor.moveToFirst()) {
      String userOnlineId = cursor.getString(0);
      cursor.close();
      return userOnlineId;
    } else {
      cursor.close();
      return null;
    }
  }

  public String getApiKey() {
    open();
    String[] columns = {DbConstants.User.KEY_API_KEY};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.User.DATABASE_TABLE,
        columns,
        null, null, null, null, null);
    cursor.moveToFirst();
    String apiKey = cursor.getString(0);
    cursor.close();
    return apiKey;
  }

  // ----------------- Todo table methods ------------------------------------------------- //

  /**
   * @return the created Todo's _id, if Todo created successfully, -1 otherwise.
   */
	public long createTodo(Todo todo) {
		open();
    ContentValues contentValues = prepareTodoContentValues(todo);
    return sqLiteDatabase.insert(DbConstants.Todo.DATABASE_TABLE, null, contentValues);
	}

  public boolean updateTodo(Todo todo) {
    open();
    ContentValues contentValues = prepareTodoContentValues(todo);

    if (todo.get_id() != 0) {
      // The Todo has been modified offline, therefore todo_online_id is null in the local
      // database yet
      String whereClause = DbConstants.Todo.KEY_ROW_ID + "=" + todo.get_id();
      return sqLiteDatabase.update(
          DbConstants.Todo.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
    } else {
      // The Todo has been modified online, therefore _id is unknown yet
      String whereClause = DbConstants.Todo.KEY_TODO_ONLINE_ID
          + "='"
          + todo.getTodoOnlineId()
          + "'";
      return sqLiteDatabase.update(
          DbConstants.Todo.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
    }
  }

  @NonNull
  private ContentValues prepareTodoContentValues(Todo todo) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Todo.KEY_TODO_ONLINE_ID, todo.getTodoOnlineId());
    contentValues.put(DbConstants.Todo.KEY_USER_ONLINE_ID, todo.getUserOnlineId());
    if (todo.getListOnlineId() == null || todo.getListOnlineId().equals("")) {
      contentValues.putNull(DbConstants.Todo.KEY_LIST_ONLINE_ID);
    } else {
      contentValues.put(DbConstants.Todo.KEY_LIST_ONLINE_ID, todo.getListOnlineId());
    }
    contentValues.put(DbConstants.Todo.KEY_TITLE, todo.getTitle());
    contentValues.put(DbConstants.Todo.KEY_PRIORITY, todo.isPriority() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_DUE_DATE, todo.getDueDate());
    if (todo.getReminderDateTime() == null || todo.getReminderDateTime().equals("")) {
      contentValues.putNull(DbConstants.Todo.KEY_REMINDER_DATETIME);
    } else {
      contentValues.put(DbConstants.Todo.KEY_REMINDER_DATETIME, todo.getReminderDateTime());
    }
    if (todo.getDescription() == null || todo.getDescription().equals("")) {
      contentValues.putNull(DbConstants.Todo.KEY_DESCRIPTION);
    } else {
      contentValues.put(DbConstants.Todo.KEY_DESCRIPTION, todo.getDescription());
    }
    contentValues.put(DbConstants.Todo.KEY_COMPLETED, todo.isCompleted() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_ROW_VERSION, todo.getRowVersion());
    contentValues.put(DbConstants.Todo.KEY_DELETED, todo.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.Todo.KEY_DIRTY, todo.getDirty() ? 1 : 0);
    return contentValues;
  }

  public ArrayList<Todo> getTodos(String wherePrefix) {
    open();
    String standardWherePostfix = prepareStandardWherePostfix();
    String where = wherePrefix + standardWherePostfix;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        todoColumns,
        where,
        null, null, null,
        todoOrderBy
    );
    cursor.moveToFirst();
    ArrayList<Todo> todos = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Todo todo = new Todo(cursor);
      todos.add(todo);
      cursor.moveToNext();
    }
    cursor.close();
    return todos;
  }

  public ArrayList<Todo> getPredefinedListTodos(String where) {
    open();
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        todoColumns,
        where,
        null, null, null,
        todoOrderBy
    );
    cursor.moveToFirst();
    ArrayList<Todo> predefinedListTodos = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Todo todo = new Todo(cursor);
      predefinedListTodos.add(todo);
      cursor.moveToNext();
    }
    cursor.close();
    return predefinedListTodos;
  }

  @NonNull
  private String prepareStandardWherePostfix() {
    return " AND "
        + DbConstants.Todo.KEY_COMPLETED
        + "="
        + 0
        + " AND "
        + DbConstants.Todo.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "'"
        + " AND "
        + DbConstants.Todo.KEY_DELETED
        + "="
        + 0;
  }

  private String today() {
    String pattern = "yyyy.MM.dd.";
    Locale defaultLocale = Locale.getDefault();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
        pattern,
        defaultLocale
    );
    Date today = new Date();
    return simpleDateFormat.format(today);
  }

  private String prepareTodayPredefinedListWherePrefix() {
    return DbConstants.Todo.KEY_DUE_DATE
        + "='"
        + today()
        + "'";
  }

  @NonNull
  public String prepareTodayPredefinedListWhere() {
    String todayPredefinedListWherePrefix = prepareTodayPredefinedListWherePrefix();
    String standardWherePostfix = prepareStandardWherePostfix();
    return todayPredefinedListWherePrefix
        + standardWherePostfix;
  }

  private String prepareNext7DaysPredefinedListWherePrefix() {
    String pattern = "yyyy.MM.dd.";
    Locale defaultLocale = Locale.getDefault();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
        pattern,
        defaultLocale
    );
    Date today = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(today);

    StringBuilder whereStringBuilder = new StringBuilder();
    String todayString = simpleDateFormat.format(today);
    appendToday(whereStringBuilder, todayString);
    for (int i = 0; i < 6; i++) {
      String nextDayString = prepareNextDayStringWhere(simpleDateFormat, calendar);
      appendNextDay(whereStringBuilder, nextDayString);
    }
    prepareWhereStringBuilderPostfix(whereStringBuilder);
    String where = whereStringBuilder.toString();
    return where;
  }

  private void appendToday(StringBuilder whereStringBuilder, String todayString) {
    whereStringBuilder.append(
        "("
            + DbConstants.Todo.KEY_DUE_DATE
            + "='"
            + todayString
            + "' OR "
    );
  }

  private String prepareNextDayStringWhere(SimpleDateFormat simpleDateFormat, Calendar calendar) {
    calendar.roll(Calendar.DAY_OF_MONTH, true);
    Date nextDay = new Date();
    nextDay.setTime(calendar.getTimeInMillis());
    return simpleDateFormat.format(nextDay);
  }

  private void appendNextDay(StringBuilder whereStringBuilder, String nextDayString) {
    whereStringBuilder.append(
        DbConstants.Todo.KEY_DUE_DATE
            + "='"
            + nextDayString
            + "' OR "
    );
  }

  private void prepareWhereStringBuilderPostfix(StringBuilder whereStringBuilder) {
    whereStringBuilder.delete(whereStringBuilder.length()-4, whereStringBuilder.length());
    whereStringBuilder.append(')');
  }

  @NonNull
  public String prepareNext7DaysPredefinedListWhere() {
    String next7DaysWherePredefinedListPrefix = prepareNext7DaysPredefinedListWherePrefix();
    String standardWherePostfix = prepareStandardWherePostfix();
    return next7DaysWherePredefinedListPrefix
        + standardWherePostfix;
  }

  @NonNull
  public String prepareAllPredefinedListWhere() {
    return DbConstants.Todo.KEY_COMPLETED
        + "="
        + 0
        + " AND "
        + DbConstants.Todo.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "'"
        + " AND "
        + DbConstants.Todo.KEY_DELETED
        + "="
        + 0;
  }

  @NonNull
  public String prepareCompletedPredefinedListWhere() {
    return DbConstants.Todo.KEY_COMPLETED
        + "="
        + 1
        + " AND "
        + DbConstants.Todo.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "'"
        + " AND "
        + DbConstants.Todo.KEY_DELETED
        + "="
        + 0;
  }

  public String prepareSearchWhere(String queryText) {
    String searchWherePrefix = prepareSearchWherePrefix(queryText);
    String standardWherePostfix = prepareStandardWherePostfix();
    return searchWherePrefix
        + standardWherePostfix;
  }

  private String prepareSearchWherePrefix(String queryText) {
    return "("
        + DbConstants.Todo.KEY_TITLE
        + " LIKE '%"
        + queryText
        + "%'"
        + " OR "
        + DbConstants.Todo.KEY_DESCRIPTION
        + " LIKE '%"
        + queryText
        + "%')";
  }

  public ArrayList<Todo> getTodosWithReminder() {
    String where = DbConstants.Todo.KEY_REMINDER_DATETIME + "!= -1";
    return getTodos(where);
  }

  public ArrayList<Todo> getTodosByListOnlineId(String listOnlineId) {
    String where = DbConstants.Todo.KEY_LIST_ONLINE_ID + "='" + listOnlineId + "'";
    return getTodos(where);
  }

  private ArrayList<String> getTodoOnlineIdsByListOnlineId(String listOnlineId) {
    open();
    String[] columns = {DbConstants.Todo.KEY_TODO_ONLINE_ID};
    String where = DbConstants.Todo.KEY_LIST_ONLINE_ID + "='" + listOnlineId + "'";
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        columns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<String> todoOnlineIds = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      String todoOnlineId = cursor.getString(0);
      todoOnlineIds.add(todoOnlineId);
      cursor.moveToNext();
    }
    cursor.close();
    return todoOnlineIds;
  }

  public ArrayList<Todo> getTodosToUpdate() {
    open();
    String where = DbConstants.Todo.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.Todo.KEY_DIRTY
        + "="
        + 1
        + " AND "
        + DbConstants.Todo.KEY_ROW_VERSION
        + ">"
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        todoColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<Todo> todosToUpdate = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Todo todo = new Todo(cursor);
      todosToUpdate.add(todo);
      cursor.moveToNext();
    }
    cursor.close();
    return todosToUpdate;
  }

  public ArrayList<Todo> getTodosToInsert() {
    open();
    String where = DbConstants.Todo.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.Todo.KEY_DIRTY
        + "="
        + 1
        + " AND "
        + DbConstants.Todo.KEY_ROW_VERSION
        + "="
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        todoColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<Todo> todosToInsert = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Todo todo = new Todo(cursor);
      todosToInsert.add(todo);
      cursor.moveToNext();
    }
    cursor.close();
    return todosToInsert;
  }

  public boolean isTodoExists(String todoOnlineId) {
    open();
    String[] columns = {DbConstants.Todo.KEY_TODO_ONLINE_ID};
    String where = DbConstants.Todo.KEY_TODO_ONLINE_ID + "= ?";
    String[] whereArguments = {todoOnlineId};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        columns,
        where,
        whereArguments,
        null, null, null
    );
    boolean exists = cursor.getCount() > 0;
    cursor.close();
    return exists;
  }

  public int getLastTodoRowVersion() {
    open();
    String[] columns = {"MAX(" + DbConstants.Todo.KEY_ROW_VERSION + ")"};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE,
        columns, 
        null, null, null, null, null
    );
    cursor.moveToFirst();
    int row_version = cursor.getInt(0);
    cursor.close();
    return row_version;
  }

	public Todo getTodo(String todoOnlineId) {
    open();
    String where = DbConstants.Todo.KEY_TODO_ONLINE_ID + "='" + todoOnlineId + "'";
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE, 
        todoColumns,
        where,
        null, null, null, null
    );
    if (cursor.moveToFirst()) {
      Todo todo = new Todo(cursor);
      cursor.close();
      return todo;
    } else {
      cursor.close();
      return null;
    }
  }

  public Todo getTodo(Long _id) {
    open();
    String where = DbConstants.Todo.KEY_ROW_ID + "=" + _id;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Todo.DATABASE_TABLE, 
        todoColumns,
        where,
        null, null, null, null
    );
    if (cursor.moveToFirst()) {
      Todo todo = new Todo(cursor);
      cursor.close();
      return todo;
    } else {
      cursor.close();
      return null;
    }
  }

  public boolean softDeleteTodo(String todoOnlineId) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Todo.KEY_DELETED, 1);
    contentValues.put(DbConstants.Todo.KEY_DIRTY, 1);
    String whereClause = DbConstants.Todo.KEY_TODO_ONLINE_ID + "='" + todoOnlineId + "'";
    return sqLiteDatabase.update(
        DbConstants.Todo.DATABASE_TABLE, 
        contentValues,
        whereClause, 
        null
    ) > 0;
  }

  public boolean softDeleteTodo(Todo todo) {
    String todoOnlineId = todo.getTodoOnlineId();
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Todo.KEY_DELETED, 1);
    contentValues.put(DbConstants.Todo.KEY_DIRTY, 1);
    String whereClause = DbConstants.Todo.KEY_TODO_ONLINE_ID + "='" + todoOnlineId + "'";
    return sqLiteDatabase.update(
        DbConstants.Todo.DATABASE_TABLE, 
        contentValues,
        whereClause, 
        null
    ) > 0;
  }

  // ----------------- List table methods ------------------------------------------------- //

  /**
   * @return the created List's _id, if List created successfully, -1 otherwise.
   */
  public long createList(List list) {
    open();
    ContentValues contentValues = prepareListContentValues(list);
    return sqLiteDatabase.insert(DbConstants.List.DATABASE_TABLE, null, contentValues);
  }

  @NonNull
  private ContentValues prepareListContentValues(List list) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.List.KEY_LIST_ONLINE_ID, list.getListOnlineId());
    contentValues.put(DbConstants.List.KEY_USER_ONLINE_ID, list.getUserOnlineId());
    if (list.getCategoryOnlineId() == null || list.getCategoryOnlineId().equals("")) {
      contentValues.putNull(DbConstants.List.KEY_CATEGORY_ONLINE_ID);
    } else {
      contentValues.put(DbConstants.List.KEY_CATEGORY_ONLINE_ID, list.getCategoryOnlineId());
    }
    contentValues.put(DbConstants.List.KEY_TITLE, list.getTitle());
    contentValues.put(DbConstants.List.KEY_ROW_VERSION, list.getRowVersion());
    contentValues.put(DbConstants.List.KEY_DELETED, list.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.List.KEY_DIRTY, list.getDirty() ? 1 : 0);
    return contentValues;
  }

  public boolean updateList(List list) {
    open();
    ContentValues contentValues = prepareListContentValues(list);

    if (list.get_id() != 0) {
      // The List has been modified offline, therefore list_online_id is null in the local
      // database yet
      String whereClause = DbConstants.List.KEY_ROW_ID + "=" + list.get_id();
      return sqLiteDatabase.update(
          DbConstants.List.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
    } else {
      // The List has been modified online, therefore _id is unknown yet
      String whereClause = DbConstants.List.KEY_LIST_ONLINE_ID + "='" + list.getListOnlineId() + "'";
      return sqLiteDatabase.update(
          DbConstants.List.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
    }
  }

  public boolean softDeleteList(String listOnlineId) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.List.KEY_DELETED, 1);
    contentValues.put(DbConstants.List.KEY_DIRTY, 1);
    String whereClause = DbConstants.List.KEY_LIST_ONLINE_ID + "='" + listOnlineId + "'";
    return sqLiteDatabase.update(
        DbConstants.List.DATABASE_TABLE,
        contentValues,
        whereClause,
        null
    ) > 0;
  }

  public void softDeleteListAndRelatedTodos(String listOnlineId) {
    ArrayList<String> todoOnlineIds = getTodoOnlineIdsByListOnlineId(listOnlineId);
    boolean areRelatedTodos = !todoOnlineIds.isEmpty();
    if (areRelatedTodos) {
      for (String todoOnlineId : todoOnlineIds) {
        softDeleteTodo(todoOnlineId);
      }
    }
    softDeleteList(listOnlineId);
  }

  public ArrayList<List> getListsNotInCategory() {
    open();
    String where = DbConstants.List.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.List.KEY_CATEGORY_ONLINE_ID
        + " IS NULL"
        + " AND "
        + DbConstants.List.KEY_DELETED
        + "="
        + 0;
    String orderBy = DbConstants.List.KEY_TITLE;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        listColumns,
        where,
        null, null, null,
        orderBy
    );
    cursor.moveToFirst();
    ArrayList<List> listsNotInCategory = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      List list = new List(cursor);
      listsNotInCategory.add(list);
      cursor.moveToNext();
    }
    cursor.close();
    return listsNotInCategory;
  }

  public ArrayList<List> getListsByCategoryOnlineId(String categoryOnlineId) {
    open();
    String where = DbConstants.List.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.List.KEY_CATEGORY_ONLINE_ID
        + "='"
        + categoryOnlineId
        + "'"
        + " AND "
        + DbConstants.List.KEY_DELETED
        + "="
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        listColumns,
        where,
        null, null, null,
        DbConstants.List.KEY_TITLE
    );
    cursor.moveToFirst();
    ArrayList<List> lists = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      List list = new List(cursor);
      lists.add(list);
      cursor.moveToNext();
    }
    cursor.close();
    return lists;
  }

  public boolean isListExists(String listOnlineId) {
    open();
    String[] columns = {DbConstants.List.KEY_LIST_ONLINE_ID};
    String where = DbConstants.List.KEY_LIST_ONLINE_ID + "= ?";
    String[] whereArguments = {listOnlineId};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        columns,
        where,
        whereArguments,
        null, null, null
    );
    boolean exists = cursor.getCount() > 0;
    cursor.close();
    return exists;
  }

  public int getLastListRowVersion() {
    open();
    String[] columns = {"MAX(" + DbConstants.List.KEY_ROW_VERSION + ")"};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        columns,
        null, null, null, null, null
    );
    cursor.moveToFirst();
    int row_version = cursor.getInt(0);
    cursor.close();
    return row_version;
  }

  public ArrayList<List> getListsToUpdate() {
    open();
    String where = DbConstants.List.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.List.KEY_DIRTY
        + "="
        + 1
        + " AND "
        + DbConstants.List.KEY_ROW_VERSION
        + ">"
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        listColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<List> listsToUpdate = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      List list = new List(cursor);
      listsToUpdate.add(list);
      cursor.moveToNext();
    }
    cursor.close();
    return listsToUpdate;
  }

  public ArrayList<List> getListsToInsert() {
    open();
    String where = DbConstants.List.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.List.KEY_DIRTY
        + "="
        + 1
        + " AND "
        + DbConstants.List.KEY_ROW_VERSION
        + "="
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.List.DATABASE_TABLE,
        listColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<List> listsToInsert = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      List list = new List(cursor);
      listsToInsert.add(list);
      cursor.moveToNext();
    }
    cursor.close();
    return listsToInsert;
  }

  // ----------------- Category table methods ---------------------------------------------- //

  /**
   * @return the created Category's _id, if Category created successfully, -1 otherwise.
   */
  public long createCategory(Category category) {
    open();
    ContentValues contentValues = prepareCategoryContentValues(category);
    return sqLiteDatabase.insert(DbConstants.Category.DATABASE_TABLE, null, contentValues);
  }

  public boolean softDeleteCategory(String categoryOnlineId) {
    open();
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Category.KEY_DELETED, 1);
    contentValues.put(DbConstants.Category.KEY_DIRTY, 1);
    String whereClause = DbConstants.Category.KEY_CATEGORY_ONLINE_ID
        + "='"
        + categoryOnlineId
        + "'";
    return sqLiteDatabase.update(
        DbConstants.Category.DATABASE_TABLE,
        contentValues,
        whereClause,
        null
    ) > 0;
  }

  public void softDeleteCategoryAndListsAndTodos(String categoryOnlineId) {
    ArrayList<List> lists = getListsByCategoryOnlineId(categoryOnlineId);
    boolean areRelatedLists = !lists.isEmpty();
    if (areRelatedLists) {
      for (List list:lists) {
        softDeleteListAndRelatedTodos(list.getListOnlineId());
      }
    }
    softDeleteCategory(categoryOnlineId);
  }

  public boolean updateCategory(Category category) {
    open();
    ContentValues contentValues = prepareCategoryContentValues(category);

    if (category.get_id() != 0) {
      // The Category has been modified offline, therefore category_online_id is null in the local
      // database yet
      String whereClause = DbConstants.Category.KEY_ROW_ID + "=" + category.get_id();
      return sqLiteDatabase.update(
          DbConstants.Category.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
    } else {
      // The Category has been modified online, therefore _id is unknown yet
      String whereClause = DbConstants.Category.KEY_CATEGORY_ONLINE_ID
          + "='"
          + category.getCategoryOnlineId()
          + "'";
      return sqLiteDatabase.update(
          DbConstants.Category.DATABASE_TABLE,
          contentValues,
          whereClause,
          null
      ) > 0;
    }
  }

  @NonNull
  private ContentValues prepareCategoryContentValues(Category category) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(DbConstants.Category.KEY_CATEGORY_ONLINE_ID, category.getCategoryOnlineId());
    contentValues.put(DbConstants.Category.KEY_USER_ONLINE_ID, category.getUserOnlineId());
    contentValues.put(DbConstants.Category.KEY_TITLE, category.getTitle());
    contentValues.put(DbConstants.Category.KEY_ROW_VERSION, category.getRowVersion());
    contentValues.put(DbConstants.Category.KEY_DELETED, category.getDeleted() ? 1 : 0);
    contentValues.put(DbConstants.Category.KEY_DIRTY, category.getDirty() ? 1 : 0);
    return contentValues;
  }

  public Category getCategoryByCategoryOnlineId(String categoryOnlineId) {
    open();
    String where = DbConstants.Category.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "'"
        + " AND "
        + DbConstants.Category.KEY_DELETED
        + "="
        + 0
        + " AND "
        + DbConstants.Category.KEY_CATEGORY_ONLINE_ID
        + "='"
        + categoryOnlineId
        + "'";
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        categoryColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    Category category = new Category(cursor);
    cursor.close();
    return category;
  }

  public ArrayList<Category> getCategories() {
    open();
    String where = DbConstants.Category.KEY_USER_ONLINE_ID + "='" + getUserOnlineId() + "'" + " AND " +
        DbConstants.Category.KEY_DELETED + "=" + 0;
    String orderBy = DbConstants.Category.KEY_TITLE;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        categoryColumns,
        where,
        null, null, null,
        orderBy
    );
    cursor.moveToFirst();
    ArrayList<Category> categories = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Category category = new Category(cursor);
      categories.add(category);
      cursor.moveToNext();
    }
    cursor.close();
    return categories;
  }

  public ArrayList<Category> getCategoriesToUpdate() {
    open();
    String where = DbConstants.Category.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.Category.KEY_DIRTY
        + "="
        + 1
        + " AND "
        + DbConstants.Category.KEY_ROW_VERSION
        + ">"
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        categoryColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<Category> categoriesToUpdate = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Category category = new Category(cursor);
      categoriesToUpdate.add(category);
      cursor.moveToNext();
    }
    cursor.close();
    return categoriesToUpdate;
  }

  public ArrayList<Category> getCategoriesToInsert() {
    open();
    String where = DbConstants.Category.KEY_USER_ONLINE_ID
        + "='"
        + getUserOnlineId()
        + "' AND "
        + DbConstants.Category.KEY_DIRTY
        + "=" + 1
        + " AND "
        + DbConstants.Category.KEY_ROW_VERSION
        + "="
        + 0;
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        categoryColumns,
        where,
        null, null, null, null
    );
    cursor.moveToFirst();
    ArrayList<Category> categoriesToInsert = new ArrayList<>();
    while (!cursor.isAfterLast()) {
      Category category = new Category(cursor);
      categoriesToInsert.add(category);
      cursor.moveToNext();
    }
    cursor.close();
    return categoriesToInsert;
  }

  public boolean isCategoryExists(String categoryOnlineId) {
    open();
    String[] columns = {DbConstants.Category.KEY_CATEGORY_ONLINE_ID};
    String where = DbConstants.Category.KEY_CATEGORY_ONLINE_ID + "= ?";
    String[] whereArguments = {categoryOnlineId};
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        columns,
        where,
        whereArguments,
        null, null, null
    );
    boolean exists = cursor.getCount() > 0;
    cursor.close();
    return exists;
  }

  public int getLastCategoryRowVersion() {
    open();
    String[] columns = {
        "MAX("
            + DbConstants.Category.KEY_ROW_VERSION
            + ")"
    };
    Cursor cursor = sqLiteDatabase.query(
        DbConstants.Category.DATABASE_TABLE,
        columns,
        null, null, null, null, null
    );
    cursor.moveToFirst();
    int row_version = cursor.getInt(0);
    cursor.close();
    return row_version;
  }

}
