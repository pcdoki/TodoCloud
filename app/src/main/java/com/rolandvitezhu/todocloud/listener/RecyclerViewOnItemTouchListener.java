package com.rolandvitezhu.todocloud.listener;

import android.content.Context;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewOnItemTouchListener implements RecyclerView.OnItemTouchListener {

  private OnClickListener onClickListener;
  private GestureDetector gestureDetector;

  public RecyclerViewOnItemTouchListener(
      Context context,
      final RecyclerView recyclerView,
      final OnClickListener onClickListener
  ) {
    this.onClickListener = onClickListener;
    gestureDetector = new GestureDetector(
        context,
        new GestureDetector.SimpleOnGestureListener() {

          @Override
          public boolean onSingleTapUp(MotionEvent e) {
            return true;
          }

          @Override
          public void onLongPress(MotionEvent e) {
            View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
            int childViewAdapterPosition = recyclerView.getChildAdapterPosition(childView);
            if (childView != null && onClickListener != null) {
              onClickListener.onLongClick(childView, childViewAdapterPosition);
            }
          }

        }
    );
  }

  @Override
  public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
    View childView = rv.findChildViewUnder(e.getX(), e.getY());
    int childViewAdapterPosition = rv.getChildAdapterPosition(childView);
    if (childView != null && onClickListener != null && gestureDetector.onTouchEvent(e)) {
      View nonViewGroupView = findNonViewGroupViewUnder(
          childView,
          (int) e.getRawX(),
          (int) e.getRawY()
      );
      if (!touchCheckBoxCompleted(nonViewGroupView) || AppController.Companion.isActionMode())
        onClickListener.onClick(childView, childViewAdapterPosition);
    }
    return false;
  }

  private boolean touchCheckBoxCompleted(View nonViewGroupView) {
    return nonViewGroupView != null && nonViewGroupView.getId() == R.id.checkbox_todo_completed;
  }

  private View findNonViewGroupViewUnder(View parentView, int rawX, int rawY) {
    if (isViewGroup(parentView)) {
      ViewGroup viewGroup = (ViewGroup) parentView;
      for (int i=0; i<viewGroup.getChildCount(); i++) {
        View childView = viewGroup.getChildAt(i);
        View nonViewGroupView = findNonViewGroupViewUnder(childView, rawX, rawY);
        if (nonViewGroupView != null) {
          return nonViewGroupView;
        }
      }
      return null;
    } else {
      return getNonViewGroupViewUnderIfAny(parentView, rawX, rawY);
    }
  }

  @Nullable
  private View getNonViewGroupViewUnderIfAny(View nonViewGroupView, int rawX, int rawY) {
    Rect nonViewGroupViewRectangle = getRect(nonViewGroupView);
    if (isNonViewGroupViewUnder(nonViewGroupViewRectangle, rawX, rawY)) {
      return nonViewGroupView;
    } else {
      return null;
    }
  }

  private boolean isNonViewGroupViewUnder(Rect parentViewRectangle, int rawX, int rawY) {
    return parentViewRectangle.contains(rawX, rawY);
  }

  private boolean isViewGroup(View parentView) {
    return parentView instanceof ViewGroup;
  }

  @NonNull
  private Rect getRect(View parentView) {
    Rect rectangle = new Rect();
    parentView.getGlobalVisibleRect(rectangle);
    return rectangle;
  }

  @Override
  public void onTouchEvent(RecyclerView rv, MotionEvent e) {

  }

  @Override
  public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

  }

  public interface OnClickListener {
    void onClick(View childView, int childViewAdapterPosition);
    void onLongClick(View childView, int childViewAdapterPosition);
  }

}
