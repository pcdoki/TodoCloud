package com.example.todocloud.datasynchronizer;

import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.example.todocloud.app.AppConfig;
import com.example.todocloud.app.AppController;
import com.example.todocloud.data.Category;
import com.example.todocloud.datastorage.DbLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CategoryDataSynchronizer extends DataSynchronizer {

  private static final String TAG = CategoryDataSynchronizer.class.getSimpleName();

  private OnSyncCategoryDataListener onSyncCategoryDataListener;
  private DbLoader dbLoader;

  public CategoryDataSynchronizer(DbLoader dbLoader) {
    this.dbLoader = dbLoader;
  }

  public void setOnSyncCategoryDataListener(
      OnSyncCategoryDataListener onSyncCategoryDataListener
  ) {
    this.onSyncCategoryDataListener = onSyncCategoryDataListener;
  }

  public void getCategories() {
    String tag_string_request = "request_get_categories";

    String url = prepareGetCategoriesUrl();
    StringRequest getCategoriesRequest = new StringRequest(
        Request.Method.GET,
        url,
        new Response.Listener<String>() {

          @Override
          public void onResponse(String response) {
            Log.d(TAG, "Get Categories Response: " + response);
            try {
              JSONObject jsonResponse = new JSONObject(response);
              boolean error = jsonResponse.getBoolean("error");

              if (!error) {
                ArrayList<Category> categories = getCategories(jsonResponse);
                if (!categories.isEmpty()) {
                  updateCategoriesInLocalDatabase(categories);
                }
                onSyncCategoryDataListener.onFinishGetCategories();
              } else {
                String message = jsonResponse.getString("message");
                Log.d(TAG, "Error Message: " + message);
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

          @NonNull
          private ArrayList<Category> getCategories(JSONObject jsonResponse) throws JSONException {
            JSONArray jsonCategories = jsonResponse.getJSONArray("categories");
            ArrayList<Category> categories = new ArrayList<>();

            for (int i = 0; i < jsonCategories.length(); i++) {
              JSONObject jsonCategory = jsonCategories.getJSONObject(i);
              Category category = new Category(jsonCategory);
              categories.add(category);
            }
            return categories;
          }

          private void updateCategoriesInLocalDatabase(ArrayList<Category> categories) {
            for (Category category : categories) {
              boolean exists = dbLoader.isCategoryExists(category.getCategoryOnlineId());
              if (!exists) {
                dbLoader.createCategory(category);
              } else {
                dbLoader.updateCategory(category);
              }
            }
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Get Categories Error: " + errorMessage);
            if (errorMessage != null) {
              onSyncCategoryDataListener.onSyncError(errorMessage);
            }
          }

        }
    ) {

      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", dbLoader.getApiKey());
        return headers;
      }

    };

    AppController.getInstance().addToRequestQueue(getCategoriesRequest, tag_string_request);
  }

  public void updateCategories() {
    ArrayList<Category> categoriesToUpdate = dbLoader.getCategoriesToUpdate();

    if (!categoriesToUpdate.isEmpty()) {
      String tag_json_object_request = "request_update_category";
      for (final Category categoryToUpdate : categoriesToUpdate) {
        JSONObject jsonRequest = new JSONObject();
        try {
          putCategoryData(categoryToUpdate, jsonRequest);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        JsonObjectRequest updateCategoriesRequest = new JsonObjectRequest(
            JsonObjectRequest.Method.PUT,
            AppConfig.URL_UPDATE_CATEGORY,
            jsonRequest,
            new Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                Log.d(TAG, "Update Category Response: " + response);
                try {
                  boolean error = response.getBoolean("error");

                  if (!error) {
                    makeCategoryUpToDate(response);
                  } else {
                    String message = response.getString("message");
                    Log.d(TAG, "Error Message: " + message);
                  }

                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              private void makeCategoryUpToDate(JSONObject response) throws JSONException {
                categoryToUpdate.setRowVersion(response.getInt("row_version"));
                categoryToUpdate.setDirty(false);
                dbLoader.updateCategory(categoryToUpdate);
              }

            },
            new Response.ErrorListener() {

              @Override
              public void onErrorResponse(VolleyError error) {
                String errorMessage = error.getMessage();
                Log.e(TAG, "Update Category Error: " + errorMessage);
                if (errorMessage != null) {
                  onSyncCategoryDataListener.onSyncError(errorMessage);
                }
              }

            }
        ) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("authorization", dbLoader.getApiKey());
            return headers;
          }

        };

        AppController.getInstance().addToRequestQueue(updateCategoriesRequest, tag_json_object_request);
      }
    }
    onSyncCategoryDataListener.onFinishUpdateCategories();
  }

  @NonNull
  private String prepareGetCategoriesUrl() {
    int end = AppConfig.URL_GET_CATEGORIES.lastIndexOf(":");
    return AppConfig.URL_GET_CATEGORIES.substring(0, end) +
        dbLoader.getCategoryRowVersion();
  }

  private void putCategoryData(
      Category categoryData,
      JSONObject jsonRequest
  ) throws JSONException {
    jsonRequest.put("category_online_id", categoryData.getCategoryOnlineId().trim());
    jsonRequest.put("title", categoryData.getTitle().trim());
    jsonRequest.put("deleted", categoryData.getDeleted() ? 1 : 0);
  }

  public interface OnSyncCategoryDataListener {
    void onFinishGetCategories();
    void onFinishUpdateCategories();
    void onSyncError(String errorMessage);
  }

}
