package com.lobotino.collector;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MyCollectionsFragment extends Fragment {

    private DbHandler dbHandler;
    private SQLiteDatabase mDb;
    private String[] galleryPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private View rootView;
    private Context context;
    private int pictureSize, screenWidth, externalMargins, topMargin, botMargin, puddingsSize, firstTopMargin, lastLeftId, lastRightId, tempId;
    private GradientDrawable gradientDrawable;
    private RelativeLayout layout;
    private ScrollView scrollView;
    private Button buttonBack;
    private ActionBar actionBar;
    private String pathToImage;

    public String fragmentStatus = "collections";

    public int currentCollection = 0;
    public int currentSection = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_my_collections, container, false);
        context = getActivity().getBaseContext();
        dbHandler = NavigationActivity.dbHandler;
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        pictureSize = Math.round((float) (screenWidth / 3));
        layout = (RelativeLayout) rootView.findViewById(R.id.relative_layout_1);
        scrollView = (ScrollView) rootView.findViewById(R.id.scroll_view_id_1);
        actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        try {
            dbHandler.updateDataBase();
        } catch (IOException mIOException) {
            throw new Error("UnableToUpdateDatabase");
        }
        try {
            mDb = dbHandler.getWritableDatabase();
        } catch (SQLException mSQLException) {
            throw mSQLException;
        }

        externalMargins = screenWidth / 11;
        topMargin = screenWidth / 9;
        botMargin = screenWidth / 16;
        puddingsSize = pictureSize / 30; //15
        firstTopMargin = screenWidth / 16;

        gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(15);
        //gradientDrawable.setColor(Color.parseColor("#180c28"));
        gradientDrawable.setColors(new int[]{Color.parseColor("#5d000e"), Color.parseColor("#430014")});


        buttonBack = new Button(context);
        int buttonSize = screenWidth/12;
        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(buttonSize, buttonSize);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        int margin =screenWidth/60;
        buttonParams.setMargins(0, margin, 0, 0);
        buttonBack.setText("");
        buttonBack.setLayoutParams(buttonParams);
        buttonBack.setId(View.generateViewId());
        buttonBack.setBackgroundResource(R.drawable.ic_action_name);

        drawAllUserCollections();

        return rootView;
    }

    private TextView getTextViewBySide(String text, int countImages)
    {
        RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(pictureSize, ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView textView = new TextView(context);
        textView.setTextColor(Color.parseColor("#ffffff"));
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);

        if (countImages % 2 == 0) {
            lastLeftId = tempId;
            textViewParams.addRule(RelativeLayout.BELOW, lastLeftId);
            textViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            textViewParams.setMargins(externalMargins, 5, 0, botMargin);
        } else {
            lastRightId = tempId;
            textViewParams.addRule(RelativeLayout.BELOW, lastRightId);
            textViewParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            textViewParams.setMargins(0, 5, externalMargins, botMargin);
        }
        textView.setLayoutParams(textViewParams);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        return textView;
    }

    private RelativeLayout.LayoutParams getImageParamsBySide(int countImages)
    {
        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(pictureSize, pictureSize);
        if (countImages % 2 == 0) {
            if (lastLeftId == -1) {
                imageParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                imageParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                imageParams.setMargins(externalMargins, firstTopMargin, 0, 0);
            } else {
                imageParams.addRule(RelativeLayout.BELOW, lastLeftId);
                imageParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                imageParams.setMargins(externalMargins, topMargin, 0, 0);
            }
        } else {
            if (lastRightId == -1) {
                imageParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                imageParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                imageParams.setMargins(0, firstTopMargin, externalMargins, 0);
            } else {
                imageParams.addRule(RelativeLayout.BELOW, lastRightId);
                imageParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                imageParams.setMargins(0, topMargin, externalMargins, 0);
            }
        }
        return imageParams;
    }

    public void drawAllUserItems(final int sectionId)
    {
        currentSection = sectionId;
        fragmentStatus = "items";
        layout.removeAllViews();
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawAllUserSections(currentCollection);
            }
        });
        layout.addView(buttonBack);
        scrollView.scrollTo(0, 0);

        Cursor cursorCurrentSection = mDb.query(DbHandler.TABLE_SECTIONS, new String[]{DbHandler.KEY_SECTION_NAME}, DbHandler.KEY_SECTION_ID + " = " + sectionId, null, null, null, null);
        if(cursorCurrentSection.moveToFirst())
            actionBar.setTitle(cursorCurrentSection.getString(cursorCurrentSection.getColumnIndex(DbHandler.KEY_SECTION_NAME)));
        cursorCurrentSection.close();

        Cursor cursorInventoryItems = mDb.query(DbHandler.TABLE_USERS_ITEMS, null, DbHandler.KEY_USER_ID + " = ?", new String[]{DbHandler.USER_ID + ""}, null, null, null);
        int itemIdIndex = cursorInventoryItems.getColumnIndex(DbHandler.KEY_ITEM_ID);
        List<Integer> listItemsId = new ArrayList<Integer>();
        if (cursorInventoryItems.moveToFirst()) {
            do {
                listItemsId.add(cursorInventoryItems.getInt(itemIdIndex));
            } while (cursorInventoryItems.moveToNext());
        }
        cursorInventoryItems.close();

        String columns[] = new String[]{DbHandler.KEY_ITEM_ID, DbHandler.KEY_ITEM_SECTION_ID, DbHandler.KEY_ITEM_NAME, DbHandler.KEY_ITEM_IMAGE_PATH};
        String selection =  DbHandler.KEY_ITEM_SECTION_ID + " = " + sectionId;

        Cursor cursorItemInSection = mDb.query(DbHandler.TABLE_ITEMS, columns, selection, null, null, null, null);

        int pathIndex = cursorItemInSection.getColumnIndex(DbHandler.KEY_ITEM_IMAGE_PATH);
        int nameIndex = cursorItemInSection.getColumnIndex(DbHandler.KEY_ITEM_NAME);
        itemIdIndex = cursorItemInSection.getColumnIndex(DbHandler.KEY_ITEM_ID);

        lastLeftId = -1;
        lastRightId = -1;
        int countImages = 0;
        int currentId = 0;
        if (EasyPermissions.hasPermissions(context, galleryPermissions)) {
            if (cursorItemInSection.moveToFirst()) {
                do {
                    int currentItemId = cursorItemInSection.getInt(itemIdIndex);
                    if(listItemsId.contains(currentItemId)){
                        pathToImage = cursorItemInSection.getString(pathIndex);

                        ImageView currentImageView = new ImageView(context);
                        currentImageView.setBackground(gradientDrawable);
                        currentImageView.setLayoutParams(getImageParamsBySide(countImages));
                        currentImageView.setPadding(puddingsSize, puddingsSize, puddingsSize, puddingsSize);
                        tempId = View.generateViewId();
                        currentImageView.setId(tempId);

                        Object offer[] = {pathToImage, currentImageView, context, pictureSize};
                        DownloadScaledImage downloadScaledImage = new DownloadScaledImage();
                        downloadScaledImage.execute(offer);

                        layout.addView(currentImageView, currentId++);
                        layout.addView(getTextViewBySide(cursorItemInSection.getString(nameIndex), countImages), currentId++);
                        countImages++;
                    }
                } while (cursorItemInSection.moveToNext());
                cursorItemInSection.close();
            }
        } else {
            EasyPermissions.requestPermissions(this, "Access for storage",
                    101, galleryPermissions);
        }
    }

    public void drawAllUserSections(int collectionId)
    {
        currentCollection = collectionId;
        fragmentStatus = "sections";
        layout.removeAllViews();
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawAllUserCollections();
            }
        });
        layout.addView(buttonBack);
        scrollView.scrollTo(0, 0);

        Cursor cursorCurrentCollection = mDb.query(DbHandler.TABLE_COLLECTIONS, new String[]{DbHandler.KEY_COLLECTION_NAME}, DbHandler.KEY_COLLECTION_ID + " = " + collectionId, null, null, null, null);
        if(cursorCurrentCollection.moveToFirst())
            actionBar.setTitle(cursorCurrentCollection.getString(cursorCurrentCollection.getColumnIndex(DbHandler.KEY_COLLECTION_NAME)));
        cursorCurrentCollection.close();

        String columns[] = new String[]{DbHandler.KEY_ITEM_ID, DbHandler.KEY_ITEM_SECTION_ID, DbHandler.KEY_ITEM_NAME, DbHandler.KEY_ITEM_IMAGE_PATH};
        Cursor cursorInventoryItems = mDb.query(DbHandler.TABLE_USERS_ITEMS, null, DbHandler.KEY_USER_ID + " = ?", new String[]{DbHandler.USER_ID + ""}, null, null, null);
        int itemIdIndex = cursorInventoryItems.getColumnIndex(DbHandler.KEY_ITEM_ID);
        List<Integer> listItemsId = new ArrayList<Integer>();
        if (cursorInventoryItems.moveToFirst()) {
            do {
                listItemsId.add(cursorInventoryItems.getInt(itemIdIndex));
            } while (cursorInventoryItems.moveToNext());
        }
        cursorInventoryItems.close();


        Cursor cursorUserItems = mDb.query(DbHandler.TABLE_ITEMS, columns, null, null, null, null, null);
        int sectionIdIndex = cursorUserItems.getColumnIndex(DbHandler.KEY_ITEM_SECTION_ID);
        int pathIndex = cursorUserItems.getColumnIndex(DbHandler.KEY_ITEM_IMAGE_PATH);
        itemIdIndex = cursorUserItems.getColumnIndex(DbHandler.KEY_ITEM_ID);
        List<Integer> listSectionsId = new ArrayList<Integer>();
        if(cursorUserItems.moveToFirst())
        {
            do {
                int currentSectionId = cursorUserItems.getInt(sectionIdIndex);
                int currentItemId = cursorUserItems.getInt(itemIdIndex);
                if (listItemsId.contains(currentItemId) && !listSectionsId.contains(currentSectionId)) {
                    listSectionsId.add(currentSectionId);
                }
            } while(cursorUserItems.moveToNext());
        }

        String selection =  DbHandler.KEY_SECTION_COLLECTION_ID + " = " + collectionId;
        columns = new String[]{DbHandler.KEY_SECTION_ID, DbHandler.KEY_SECTION_NAME, DbHandler.KEY_SECTION_COLLECTION_ID};
        Cursor cursorUserSections = mDb.query(DbHandler.TABLE_SECTIONS, columns, selection, null, null, null, null);

        int nameIndex = cursorUserSections.getColumnIndex(DbHandler.KEY_SECTION_NAME);
        int idSectionIndex = cursorUserSections.getColumnIndex(DbHandler.KEY_SECTION_ID);
        lastLeftId = -1;
        lastRightId = -1;
        int countImages = 0;
        int currentId = 0;
        if (EasyPermissions.hasPermissions(context, galleryPermissions)) {
            if (cursorUserSections.moveToFirst()) {
                do {
                    final int currentSectionId = cursorUserSections.getInt(idSectionIndex);
                    if(listSectionsId.contains(currentSectionId)) {
                        pathToImage = "";
                        if(cursorUserItems.moveToFirst())
                        {
                            do {
                                int cursorSectionId = cursorUserItems.getInt(sectionIdIndex);
                                int cursorItemId = cursorUserItems.getInt(itemIdIndex);
                                if(listItemsId.contains(cursorItemId)){
                                    if (currentSectionId == cursorSectionId) {
                                        pathToImage = cursorUserItems.getString(pathIndex);
                                        break;
                                    }
                                }
                            } while(cursorUserItems.moveToNext());
                        }
                        ImageView currentImageView = new ImageView(context);
                        currentImageView.setBackground(gradientDrawable);
                        currentImageView.setLayoutParams(getImageParamsBySide(countImages));
                        currentImageView.setPadding(puddingsSize, puddingsSize, puddingsSize, puddingsSize);
                        currentImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                drawAllUserItems(currentSectionId);
                            }
                        });

                        tempId = View.generateViewId();
                        currentImageView.setId(tempId);

                        Object offer[] = {pathToImage, currentImageView, context, pictureSize};
                        DownloadScaledImage downloadScaledImage = new DownloadScaledImage();
                        downloadScaledImage.execute(offer);

                        layout.addView(currentImageView, currentId++);
                        layout.addView(getTextViewBySide(cursorUserSections.getString(nameIndex), countImages), currentId++);
                        countImages++;
                    }
                } while (cursorUserSections.moveToNext());
                cursorUserItems.close();
                cursorUserSections.close();
            }
        } else {
            EasyPermissions.requestPermissions(this, "Access for storage",
                    101, galleryPermissions);
        }
    }

    public void drawAllUserCollections()
    {
        fragmentStatus = "collections";
        layout.removeAllViews();
        scrollView.scrollTo(0, 0);
        actionBar.setTitle("Мои коллекции");

        String columns[] = new String[]{DbHandler.KEY_ITEM_ID, DbHandler.KEY_ITEM_SECTION_ID, DbHandler.KEY_ITEM_NAME, DbHandler.KEY_ITEM_IMAGE_PATH};
        Cursor cursorInventoryItems = mDb.query(DbHandler.TABLE_USERS_ITEMS, null, DbHandler.KEY_USER_ID + " = ?", new String[]{DbHandler.USER_ID + ""}, null, null, null);
        int itemIdIndex = cursorInventoryItems.getColumnIndex(DbHandler.KEY_ITEM_ID);
        List<Integer> userItemsIds = new ArrayList<Integer>();
        if (cursorInventoryItems.moveToFirst()) {
            do {
                userItemsIds.add(cursorInventoryItems.getInt(itemIdIndex));
            } while (cursorInventoryItems.moveToNext());
        }
        cursorInventoryItems.close();


        Cursor cursorUserItems = mDb.query(DbHandler.TABLE_ITEMS, columns, null, null, null, null, null);
        int sectionIdIndex = cursorUserItems.getColumnIndex(DbHandler.KEY_ITEM_SECTION_ID);
        itemIdIndex = cursorUserItems.getColumnIndex(DbHandler.KEY_ITEM_ID);
        List<Integer> userSectionsIds = new ArrayList<Integer>();
        if(cursorUserItems.moveToFirst())
        {
            do {
                int currentSectionId = cursorUserItems.getInt(sectionIdIndex);
                int currentItemId = cursorUserItems.getInt(itemIdIndex);
                if (userItemsIds.contains(currentItemId) && !userSectionsIds.contains(currentSectionId)) {
                    userSectionsIds.add(currentSectionId);
                }
            } while(cursorUserItems.moveToNext());
        }

        columns = new String[]{DbHandler.KEY_SECTION_ID, DbHandler.KEY_SECTION_NAME, DbHandler.KEY_SECTION_COLLECTION_ID};
        Cursor cursorUserSections = mDb.query(DbHandler.TABLE_SECTIONS, columns, null, null, null, null, null);
        int idCollIndex = cursorUserSections.getColumnIndex(DbHandler.KEY_SECTION_COLLECTION_ID);
        int idCurSecIndex = cursorUserSections.getColumnIndex(DbHandler.KEY_SECTION_ID);
        List<Integer> userCollectionsIds = new ArrayList<Integer>();
        if(cursorUserSections.moveToFirst())
        {
            do{
                int currentCollectionId = cursorUserSections.getInt(idCollIndex);
                int currentSectionIndex = cursorUserSections.getInt(idCurSecIndex);
                if(userSectionsIds.contains(currentSectionIndex) && !userCollectionsIds.contains(currentCollectionId))
                {
                    userCollectionsIds.add(currentCollectionId);
                }
            }while(cursorUserSections.moveToNext());
        }
        cursorUserSections.close();

        columns = new String[]{DbHandler.KEY_COLLECTION_ID, DbHandler.KEY_COLLECTION_NAME};
        Cursor cursorUserCollections = mDb.query(DbHandler.TABLE_COLLECTIONS, columns, null, null, null, null, null);

        lastLeftId = -1;
        lastRightId = -1;
        int countImages = 0;
        int currentId = 0;

        int idCollectionsIndex = cursorUserCollections.getColumnIndex(DbHandler.KEY_COLLECTION_ID);
        if (EasyPermissions.hasPermissions(context, galleryPermissions)) {
            if (cursorUserCollections.moveToFirst()) {
                for(int i = 0; i < userCollectionsIds.size(); i++) {
                    final int currentCollId = cursorUserCollections.getInt(idCollectionsIndex);

                    if (userCollectionsIds.contains(currentCollId)) {
                        pathToImage = "";
                        Cursor tempSectionsCursor = mDb.query(DbHandler.TABLE_SECTIONS, null, DbHandler.KEY_SECTION_COLLECTION_ID + " = " + userCollectionsIds.get(i), null, null, null, null);
                        if(tempSectionsCursor.moveToFirst()) {
                            int tempSectionId = tempSectionsCursor.getInt(tempSectionsCursor.getColumnIndex(DbHandler.KEY_SECTION_ID));
                            Cursor tempItemsCursor = mDb.query(DbHandler.TABLE_ITEMS, null, DbHandler.KEY_ITEM_SECTION_ID + " = " + tempSectionId, null, null, null, null);
                            if(tempItemsCursor.moveToFirst()) {
                                pathToImage = tempItemsCursor.getString(tempItemsCursor.getColumnIndex(DbHandler.KEY_ITEM_IMAGE_PATH));
                            }
                            tempItemsCursor.close();
                        }
                        tempSectionsCursor.close();

                        ImageView currentImageView = new ImageView(context);
                        currentImageView.setBackground(gradientDrawable);
                        currentImageView.setLayoutParams(getImageParamsBySide(countImages));
                        currentImageView.setPadding(puddingsSize, puddingsSize, puddingsSize, puddingsSize);
                        currentImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                drawAllUserSections(currentCollId);
                            }
                        });

                        tempId = View.generateViewId();
                        currentImageView.setId(tempId);

                        Object offer[] = {pathToImage, currentImageView, context, pictureSize};
                        DownloadScaledImage downloadScaledImage = new DownloadScaledImage();
                        downloadScaledImage.execute(offer);

                        layout.addView(currentImageView, currentId++);
                        layout.addView(getTextViewBySide(cursorUserCollections.getString(cursorUserCollections.getColumnIndex(DbHandler.KEY_COLLECTION_NAME)), countImages), currentId++);
                        countImages++;
                        cursorUserCollections.moveToNext();
                    }
                }
                cursorUserItems.close();
                cursorUserCollections.close();
            }
        } else {
            EasyPermissions.requestPermissions(this, "Access for storage",
                    101, galleryPermissions);
        }
    }
}