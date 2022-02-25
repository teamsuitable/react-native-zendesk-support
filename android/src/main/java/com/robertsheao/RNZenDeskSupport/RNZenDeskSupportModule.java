/**
 * Created by Patrick O'Connor on 8/30/17.
 * https://github.com/RobertSheaO/react-native-zendesk-support
 */

package com.robertsheao.RNZenDeskSupport;

import android.app.Activity;
import android.os.Build;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.zendesk.service.ErrorResponse;
import com.zendesk.service.ZendeskCallback;

import java.util.ArrayList;
import java.util.List;

import zendesk.configurations.Configuration;
import zendesk.core.AnonymousIdentity;
import zendesk.core.Zendesk;
import zendesk.support.Article;
import zendesk.support.CreateRequest;
import zendesk.support.HelpCenterProvider;
import zendesk.support.Request;
import zendesk.support.RequestProvider;
import zendesk.support.Section;
import zendesk.support.Support;
import zendesk.support.guide.ViewArticleActivity;
import zendesk.support.request.RequestActivity;
import zendesk.support.request.RequestConfiguration;
import zendesk.support.requestlist.RequestListActivity;
import zendesk.support.guide.HelpCenterActivity;

public class RNZenDeskSupportModule extends ReactContextBaseJavaModule {

  HelpCenterProvider HelpCenterProvider;
  RequestProvider requestProvider;

  public RNZenDeskSupportModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return "RNZenDeskSupport";
  }

  private static long[] toLongArray(ArrayList<?> values) {
    long[] arr = new long[values.size()];
    for (int i = 0; i < values.size(); i++)
      arr[i] = Long.parseLong((String) values.get(i));
    return arr;
  }

  @ReactMethod
  public void initialize(ReadableMap config) {
    String appId = config.getString("appId");
    String zendeskUrl = config.getString("zendeskUrl");
    String clientId = config.getString("clientId");
    Zendesk.INSTANCE.init(getReactApplicationContext(), zendeskUrl, appId, clientId);
    Support.INSTANCE.init(Zendesk.INSTANCE);

  }

  @ReactMethod
  public void setupIdentity(ReadableMap identity) {
    AnonymousIdentity.Builder builder = new AnonymousIdentity.Builder();

    if (identity != null && identity.hasKey("customerEmail")) {
      builder.withEmailIdentifier(identity.getString("customerEmail"));
    }

    if (identity != null && identity.hasKey("customerName")) {
      builder.withNameIdentifier(identity.getString("customerName"));
    }

    Zendesk.INSTANCE.setIdentity(builder.build());
  }

  @ReactMethod
  public void showHelpCenterWithOptions(ReadableMap options) {
    HelpCenterActivityBuilder.create()
            .withOptions(options)
            .show(getReactApplicationContext());
  }

  @ReactMethod
  public void showCategoriesWithOptions(ReadableArray categoryIds, ReadableMap options) {
    HelpCenterActivityBuilder.create()
            .withOptions(options)
            .withArticlesForCategoryIds(categoryIds)
            .show(getReactApplicationContext());
  }

  @ReactMethod
  public void showSectionsWithOptions(ReadableArray sectionIds, ReadableMap options) {
    List<Long> sections = new ArrayList<>();

    // Iterate through the array
    for (Long section: toLongArray(sectionIds.toArrayList())) {
      // Add each element into the list
      sections.add(section);
    }

    HelpCenterActivity.builder()
            .withArticlesForSectionIds(sections)
            .show(getReactApplicationContext());
  }

  @ReactMethod
  public void showLabelsWithOptions(ReadableArray labels, ReadableMap options) {
    HelpCenterActivityBuilder.create()
            .withOptions(options)
            .withLabelNames(labels)
            .show(getReactApplicationContext());
  }

  @ReactMethod
  public void showHelpCenter() {
    showHelpCenterWithOptions(null);
  }

  @ReactMethod
  public void showCategories(ReadableArray categoryIds) {
    showCategoriesWithOptions(categoryIds, null);
  }

  @ReactMethod
  public void showSection(String article) {
    Long articleID = Long.valueOf(article);
    ViewArticleActivity.builder(articleID)
            .show(getReactApplicationContext());
  }

  @ReactMethod
  public void showSections(ReadableArray sectionIds) {
    showSectionsWithOptions(sectionIds, null);
  }

  @ReactMethod
  public void showLabels(ReadableArray labels) {
    showLabelsWithOptions(labels, null);
  }

  @ReactMethod
  public void callSupport(ReadableMap customFields) {
    Activity activity = getCurrentActivity();

    if(activity != null){
      RequestActivity.builder()
              .withTags("mobile", Build.BRAND, String.valueOf(Build.VERSION.SDK_INT), Build.MODEL, "Android")
              .show(getReactApplicationContext());
    }
  }

  @ReactMethod
  public void supportHistory() {

    Activity activity = getCurrentActivity();

    Configuration requestActivityConfig = RequestActivity.builder()
            .withTags("mobile", Build.BRAND, "API" + String.valueOf(Build.VERSION.SDK_INT), Build.MODEL, "Android")
            .config();

    RequestListActivity.builder()
            .show(getReactApplicationContext(), requestActivityConfig);
  }

  // PROVIDER METHODS

  @ReactMethod
  public void createHCProvider() {
    HelpCenterProvider = Support.INSTANCE.provider().helpCenterProvider();
  }

  @ReactMethod
  public void createRequestProvider() {
    requestProvider = Support.INSTANCE.provider().requestProvider();
  }

  @ReactMethod
  public void createRequest(String ticketInfo, String subject, final Promise promise) {
    CreateRequest ticket = new CreateRequest();
    List<String> tags = new ArrayList<>();
    tags.add("mobile");
    tags.add(Build.BRAND);
    tags.add("API" + Build.VERSION.SDK_INT);
    tags.add(Build.MODEL);
    tags.add("Android");
    ticket.setTags(tags);
    ticket.setSubject(subject);
    ticket.setDescription(ticketInfo);

    requestProvider.createRequest(ticket, new ZendeskCallback<Request>() {
      @Override
      public void onSuccess(Request request) {
        WritableArray ticket = new WritableNativeArray();
        ticket.pushString(request.getId());
        ticket.pushString(request.getRequesterId().toString());
        promise.resolve(ticket);
      }

      @Override
      public void onError(ErrorResponse errorResponse) {
        promise.reject(errorResponse.getReason());
      }
    });
  }

  @ReactMethod
  public void getSection(String section, final Promise promise) {
    HelpCenterProvider.getArticles(Long.valueOf(section), new ZendeskCallback<List<Article>>() {
      @Override
      public void onSuccess(List<Article> section) {

        WritableArray sectionData = Arguments.createArray();

        for (Article article: section) {
          WritableArray articleInfo = new WritableNativeArray();
          articleInfo.pushString(article.getTitle());
          articleInfo.pushString(article.getId().toString());
          sectionData.pushArray(articleInfo);
        }
        WritableArray error = new WritableNativeArray();
        error.pushNull();
        promise.resolve(sectionData);
      }

      @Override
      public void onError(ErrorResponse errorResponse) {
        promise.reject(errorResponse.getReason());
      }
    });
  }

  @ReactMethod
  public void getCategory(String categoryID, final Promise promise) {
    HelpCenterProvider.getSections(Long.valueOf(categoryID), new ZendeskCallback<List<Section>>() {
      @Override
      public void onSuccess(List<Section> sections) {
        WritableArray sectionData = Arguments.createArray();

        for(Section section: sections){
          WritableArray sectionNameAndID = new WritableNativeArray();
          sectionNameAndID.pushString(section.getName());
          sectionNameAndID.pushString(section.getId().toString());
          sectionData.pushArray(sectionNameAndID);
        }

        promise.resolve(sectionData);
      }

      @Override
      public void onError(ErrorResponse errorResponse) {
        promise.resolve(errorResponse.getReason());
      }
    });
  }

  @ReactMethod
  public void getArticle(String article, final Promise promise) {
    HelpCenterProvider.getArticle(Long.valueOf(article), new ZendeskCallback<Article>() {
      @Override
      public void onSuccess(Article article) {

        WritableArray articleInfo = new WritableNativeArray();
        articleInfo.pushString(article.getTitle());
        articleInfo.pushString(article.getHtmlUrl());

        promise.resolve(articleInfo);
      }

      @Override
      public void onError(ErrorResponse errorResponse) {
        promise.reject(errorResponse.getReason());
      }

    });
  }

}
