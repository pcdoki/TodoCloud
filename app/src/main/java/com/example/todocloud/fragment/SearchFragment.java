package com.example.todocloud.fragment;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.SearchView;

import com.example.todocloud.R;
import com.example.todocloud.adapter.TodoAdapter;
import com.example.todocloud.app.AppController;
import com.example.todocloud.data.Todo;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.datastorage.asynctask.UpdateAdapterTask;
import com.example.todocloud.listener.RecyclerViewOnItemTouchListener;
import com.example.todocloud.receiver.ReminderSetter;

import java.util.ArrayList;

public class SearchFragment extends Fragment implements
    ModifyTodoFragment.IModifyTodoFragment,
    ConfirmDeleteDialogFragment.IConfirmDeleteDialogFragment {

  private DbLoader dbLoader;
  private TodoAdapter todoAdapter;
  private RecyclerView recyclerView;
  private SearchView searchView;
  private ISearchFragment listener;
  private ActionMode actionMode;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ISearchFragment) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    dbLoader = new DbLoader();
    todoAdapter = new TodoAdapter(dbLoader);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.fragment_search, container, false);
    prepareRecyclerView(view);
    applyClickEvents();
    applySwipeToDismiss();
    return view;
  }

  private void prepareRecyclerView(View view) {
    recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(
        getContext().getApplicationContext()
    );
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(todoAdapter);
  }

  private void applyClickEvents() {
    recyclerView.addOnItemTouchListener(new RecyclerViewOnItemTouchListener(
        getContext().getApplicationContext(),
        recyclerView,
        new RecyclerViewOnItemTouchListener.OnClickListener() {

          @Override
          public void onClick(View childView, int childViewAdapterPosition) {
            if (!isActionMode()) {
              hideSoftInput();
              openModifyTodoFragment(childViewAdapterPosition);
            } else {
              todoAdapter.toggleSelection(childViewAdapterPosition);

              if (areSelectedItems()) {
                actionMode.invalidate();
              } else {
                actionMode.finish();
              }
            }
          }

          @Override
          public void onLongClick(View childView, int childViewAdapterPosition) {
            if (!isActionMode()) {
              listener.onStartActionMode(callback);
              todoAdapter.toggleSelection(childViewAdapterPosition);
              actionMode.invalidate();
            }
          }

        }
        )
    );
  }

  private void applySwipeToDismiss() {
    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
        0, ItemTouchHelper.END
    ) {

      @Override
      public boolean onMove(
          RecyclerView recyclerView,
          RecyclerView.ViewHolder viewHolder,
          RecyclerView.ViewHolder target
      ) {
        return false;
      }

      @Override
      public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        Todo swipedTodo = getSwipedTodo(viewHolder);
        toggleCompleted(swipedTodo);
        updateTodo(swipedTodo);
        todoAdapter.removeTodoFromAdapter((TodoAdapter.ItemViewHolder) viewHolder);
        handleReminderService(swipedTodo);
      }

      @Override
      public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int swipeFlags;
        if (AppController.isActionMode()) swipeFlags = 0;
        else swipeFlags = ItemTouchHelper.END;
        return makeMovementFlags(0, swipeFlags);
      }

    };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
    itemTouchHelper.attachToRecyclerView(recyclerView);
  }

  private Todo getSwipedTodo(RecyclerView.ViewHolder viewHolder) {
    int swipedTodoAdapterPosition = viewHolder.getAdapterPosition();
    return todoAdapter.getTodo(swipedTodoAdapterPosition);
  }

  private void toggleCompleted(Todo todo) {
    todo.setCompleted(!todo.isCompleted());
  }

  private void updateTodo(Todo todo) {
    todo.setDirty(true);
    dbLoader.updateTodo(todo);
  }

  private void handleReminderService(Todo todo) {
    if (todo.isCompleted()) {
      ReminderSetter.cancelReminderService(todo);
    } else if (isSetReminder(todo)) {
      ReminderSetter.createReminderService(todo);
    }
  }

  private boolean areSelectedItems() {
    return todoAdapter.getSelectedItemCount() > 0;
  }

  private boolean isActionMode() {
    return actionMode != null;
  }

  private void openModifyTodoFragment(int childViewAdapterPosition) {
    Todo todo = todoAdapter.getTodo(childViewAdapterPosition);
    listener.onClickTodo(todo, this);
  }

  @Override
  public void onResume() {
    super.onResume();
    listener.onSetActionBarTitle("");
    prepareSearchViewAfterModifyTodo();
  }

  private void prepareSearchViewAfterModifyTodo() {
    if (searchView != null && recyclerView != null) {
      searchView.post(new Runnable() {
        @Override
        public void run() {
          restoreQueryTextState();
          recyclerView.requestFocusFromTouch();
          searchView.clearFocus();
          hideSoftInput();
        }
      });
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    restoreQueryTextState();
  }

  private void restoreQueryTextState() {
    if (searchView != null) {
      String queryText = getArguments().getString("queryText");
      searchView.setQuery(queryText, false);
    }
  }

  private ActionMode.Callback callback = new ActionMode.Callback() {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      setActionMode(mode);
      mode.getMenuInflater().inflate(R.menu.todo, menu);
      preventTypeIntoSearchView();

      return true;
    }

    private void preventTypeIntoSearchView() {
      if (searchView != null && recyclerView != null) {
        recyclerView.requestFocusFromTouch();
      }
      hideSoftInput();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      String title = prepareTitle();
      actionMode.setTitle(title);

      return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      int actionItemId = item.getItemId();

      switch (actionItemId) {
        case R.id.itemDelete:
          openConfirmDeleteTodosDialog();
          break;
      }

      return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      todoAdapter.clearSelection();
      setActionMode(null);
    }

    private String prepareTitle() {
      int selectedItemCount = todoAdapter.getSelectedItemCount();
      String title = selectedItemCount + " " + getString(R.string.selected);
      return title;
    }

  };

  private void setActionMode(ActionMode actionMode) {
    this.actionMode = actionMode;
    AppController.setActionMode(actionMode);
  }

  private void openConfirmDeleteTodosDialog() {
    ArrayList<Todo> selectedTodos = todoAdapter.getSelectedTodos();
    Bundle arguments = new Bundle();
    arguments.putString("itemType", "todo");
    arguments.putParcelableArrayList("itemsToDelete", selectedTodos);
    openConfirmDeleteDialogFragment(arguments);
  }

  private void openConfirmDeleteDialogFragment(Bundle arguments) {
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
  }

  private void updateTodoAdapter() {
    if (todoAdapter == null) {
      todoAdapter = new TodoAdapter(dbLoader);
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, todoAdapter);
    updateAdapterTask.execute(getArguments());
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.search_options_menu, menu);
    prepareSearchView(menu);
  }

  private void prepareSearchView(Menu menu) {
    MenuItem searchMenuItem = menu.findItem(R.id.itemSearch);
    searchView = (SearchView) searchMenuItem.getActionView();
    SearchManager searchManager = (SearchManager) getActivity()
        .getSystemService(Context.SEARCH_SERVICE);
    SearchableInfo searchableInfo = searchManager.getSearchableInfo(
        getActivity().getComponentName()
    );
    searchView.setSearchableInfo(searchableInfo);
    searchView.setMaxWidth(Integer.MAX_VALUE);
    searchView.setIconified(false);
    searchView.setFocusable(true);
    searchView.requestFocusFromTouch();
    disableSearchViewCloseButton();
    removeSearchViewUnderline();
    removeSearchViewHintIcon();
    applyOnQueryTextEvents();
  }

  private void removeSearchViewUnderline() {
    int searchPlateId = searchView.getContext().getResources().getIdentifier(
        "android:id/search_plate", null, null
    );
    View searchPlate = searchView.findViewById(searchPlateId);
    if (searchPlate != null) {
      searchPlate.setBackgroundResource(0);
    }
  }

  private void removeSearchViewHintIcon() {
    if (searchView != null) {
      int searchMagIconId = searchView.getContext().getResources().getIdentifier(
          "android:id/search_mag_icon", null, null
      );
      View searchMagIcon = searchView.findViewById(searchMagIconId);
      if (searchMagIcon != null) {
        searchView.setIconifiedByDefault(false);
        searchMagIcon.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
      }
    }
  }

  private void disableSearchViewCloseButton() {
    searchView.setOnCloseListener(new SearchView.OnCloseListener() {
      @Override
      public boolean onClose() {
        return true;
      }
    });
  }

  private void applyOnQueryTextEvents() {
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

      @Override
      public boolean onQueryTextSubmit(String query) {
        preventToExecuteQueryTextSubmitTwice();
        return true;
      }

      private void preventToExecuteQueryTextSubmitTwice() {
        if (searchView != null) {
          searchView.clearFocus();
          hideSoftInput();
        }
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        saveQueryTextState(newText);
        if (!newText.isEmpty()) {
          showSearchResults(newText);
        } else {
          clearSearchResults();
        }
        return true;
      }

      private void saveQueryTextState(String queryText) {
        Bundle arguments = new Bundle();
        arguments.putString("queryText", queryText);
        Bundle currentArguments = getArguments();
        currentArguments.putAll(arguments);
      }

      private void showSearchResults(String newText) {
        setUpdateTodoAdapterArguments(newText);
        updateTodoAdapter();
      }

      private void clearSearchResults() {
        todoAdapter.clear();
      }

      private void setUpdateTodoAdapterArguments(String queryText) {
        String where = dbLoader.prepareSearchWhere(queryText);
        Bundle arguments = new Bundle();
        arguments.putString("selectFromDB", where);
        Bundle currentArguments = getArguments();
        currentArguments.putAll(arguments);
      }

    });
  }

  private void hideSoftInput() {
    InputMethodManager inputMethodManager =
        (InputMethodManager) getActivity().getSystemService(
            Context.INPUT_METHOD_SERVICE
        );
    View currentlyFocusedView = getActivity().getCurrentFocus();
    if (currentlyFocusedView != null) {
      IBinder windowToken = currentlyFocusedView.getWindowToken();
      inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
    }
  }

  private boolean isSetReminder(Todo todo) {
    return !todo.getReminderDateTime().equals("-1");
  }

  private boolean isNotCompleted(Todo todo) {
    return !todo.isCompleted();
  }

  @Override
  public void onModifyTodo(Todo todo) {
    dbLoader.updateTodo(todo);
    updateTodoAdapter();

    if (isSetReminder(todo)) {
      if (shouldCreateReminderService(todo)) {
        ReminderSetter.createReminderService(todo);
      }
    } else {
      ReminderSetter.cancelReminderService(todo);
    }
  }

  private boolean shouldCreateReminderService(Todo todoToModify) {
    return isNotCompleted(todoToModify) && isNotDeleted(todoToModify);
  }

  private boolean isNotDeleted(Todo todo) {
    return !todo.getDeleted();
  }

  @Override
  public void onSoftDelete(String onlineId, String itemType) {
    Todo todoToSoftDelete = dbLoader.getTodo(onlineId);
    dbLoader.softDeleteTodo(todoToSoftDelete);
    updateTodoAdapter();
    ReminderSetter.cancelReminderService(todoToSoftDelete);
    actionMode.finish();
  }

  @Override
  public void onSoftDelete(ArrayList itemsToDelete, String itemType) {
    ArrayList<Todo> todosToSoftDelete = itemsToDelete;
    for (Todo todoToSoftDelete:todosToSoftDelete) {
      dbLoader.softDeleteTodo(todoToSoftDelete);
      ReminderSetter.cancelReminderService(todoToSoftDelete);
    }
    updateTodoAdapter();
    actionMode.finish();
  }

  public interface ISearchFragment {
    void onSetActionBarTitle(String actionBarTitle);
    void onStartActionMode(ActionMode.Callback callback);
    void onClickTodo(Todo todo, Fragment targetFragment);
  }

}
