package com.lobotino.collector.async_tasks.collections;

import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.CardView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lobotino.collector.R;
import com.lobotino.collector.async_tasks.AsyncSetItemStatus;
import com.lobotino.collector.fragments.CurrentItemFragment;
import com.lobotino.collector.utils.DbHandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.lobotino.collector.activities.MainActivity.dbHandler;
import static com.lobotino.collector.fragments.CollectionsFragment.clearOffers;
import static com.lobotino.collector.fragments.CollectionsFragment.collectionTitle;
import static com.lobotino.collector.fragments.CollectionsFragment.countImages;
import static com.lobotino.collector.fragments.CollectionsFragment.currentId;
import static com.lobotino.collector.fragments.CollectionsFragment.dp;
import static com.lobotino.collector.fragments.CollectionsFragment.firstTopMargin;
import static com.lobotino.collector.fragments.CollectionsFragment.fragmentType;
import static com.lobotino.collector.fragments.CollectionsFragment.lastLeftId;
import static com.lobotino.collector.fragments.CollectionsFragment.lastRightId;
import static com.lobotino.collector.fragments.CollectionsFragment.sectionTitle;
import static com.lobotino.collector.fragments.CollectionsFragment.tempId;
import static com.lobotino.collector.fragments.CollectionsFragment.checkImageSize;
import static com.lobotino.collector.fragments.CollectionsFragment.externalMargins;
import static com.lobotino.collector.fragments.CollectionsFragment.maxImageSize;
import static com.lobotino.collector.fragments.CollectionsFragment.topMargin;


public class AsyncCurrentItem extends AsyncTask<String, Void, Bitmap> {
    private String SQL;
    private String name, desc, status;
    private int collectionId, secId, itemId, userId = -1;
    private boolean inMyCollection = false;
    private Context context;
    private FragmentManager fragmentManager;
//    private RelativeLayout layout;
    private SQLiteDatabase mDb;
    private ActionBar actionBar;
    private RelativeLayout layout;

    public AsyncCurrentItem(RelativeLayout layout, int itemId, int secId, String status,
                            Context context, SQLiteDatabase mDb, FragmentManager fm, ActionBar actionBar) {
        this.layout = layout;
        this.itemId = itemId;
        this.secId = secId;
        this.status = status;
        this.actionBar = actionBar;
        this.fragmentManager = fm;
        this.context = context;
        this.mDb = mDb;
    }

    public AsyncCurrentItem( RelativeLayout layout, int itemId, int secId, String name, String status,
                            Context context, SQLiteDatabase mDb, FragmentManager fm,  ActionBar actionBar) {
        this.layout = layout;
        this.itemId = itemId;
        this.name = name;
        this.status = status;
        this.secId = secId;
        this.actionBar = actionBar;
        this.fragmentManager = fm;
        this.context = context;
        this.mDb = mDb;
    }

    public AsyncCurrentItem(RelativeLayout layout, int itemId, int secId, int collectionId, String name, String status,
                            Context context, SQLiteDatabase mDb, FragmentManager fm, ActionBar actionBar) {
        this.layout = layout;
        this.itemId = itemId;
        this.name = name;
        this.status = status;
        this.collectionId = collectionId;
        this.secId = secId;
        this.actionBar = actionBar;
        this.fragmentManager = fm;
        this.context = context;
        this.mDb = mDb;
    }

    public void setUserId(int id) {
        userId = id;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize - 1;
    }

    @Override
    protected Bitmap doInBackground(String... query) {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            Statement st = null;
            ResultSet rs = null;
            try {
                Connection connection = dbHandler.getConnection(context);

                Cursor cursor = mDb.query(DbHandler.TABLE_ITEMS, null, DbHandler.KEY_ID + " = " + itemId, null, null, null, null);
                int count = cursor.getCount();
                byte[] blob = null;
                String dateOfChangeClient = "";
                if (count > 0) {
                    if (cursor.moveToFirst()) {
                        blob = cursor.getBlob(cursor.getColumnIndex(DbHandler.KEY_MINI_IMAGE));
                        if (status.equals("item")) {
                            name = cursor.getString(cursor.getColumnIndex(DbHandler.KEY_NAME));
                        }
                    }
                    if (connection != null) {
                        SQL = "SELECT " + DbHandler.KEY_DATE_OF_CHANGE + " FROM " + DbHandler.TABLE_ITEMS + " WHERE " + DbHandler.KEY_ID + " = " + this.itemId;
                        st = connection.createStatement();
                        rs = st.executeQuery(SQL);

                        if (rs != null && !isCancelled()) {
                            rs.next();
                            String dateOfChangeServer = rs.getString(1);
                            DateFormat orig = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss");
                            Date dateClient, dateServer;
                            try {
                                dateOfChangeClient = cursor.getString(cursor.getColumnIndex(DbHandler.KEY_DATE_OF_CHANGE));
                                dateClient = orig.parse(dateOfChangeClient);
                                dateServer = orig.parse(dateOfChangeServer);
                                if (dateServer.after(dateClient)) {
                                    SQL = "SELECT * FROM " + DbHandler.TABLE_ITEMS + " WHERE " + DbHandler.KEY_ID + " = " + this.itemId;
                                    Statement st1 = connection.createStatement();
                                    ResultSet rs1 = st1.executeQuery(SQL);

                                    if (rs1 != null && !isCancelled()) {
                                        rs1.next();
                                        int id = rs1.getInt(1);
                                        String tname = rs1.getString(2);
                                        if (status.equals("item")) name = tname;
                                        String desc = rs1.getString(3);
                                        int collectionId = rs1.getInt(5);
                                        blob = rs1.getBytes(7);
                                        String serverDateStr = rs1.getString(8);

                                        ContentValues contentValues = new ContentValues();
                                        contentValues.put(DbHandler.KEY_ID, id);
                                        contentValues.put(DbHandler.KEY_SECTION_ID, secId);
                                        contentValues.put(DbHandler.KEY_NAME, tname);
                                        contentValues.put(DbHandler.KEY_DESCRIPTION, desc);
                                        contentValues.put(DbHandler.KEY_COLLECTION_ID, collectionId);
                                        contentValues.put(DbHandler.KEY_MINI_IMAGE, blob);
                                        contentValues.put(DbHandler.KEY_DATE_OF_CHANGE, serverDateStr);

                                        SQL = "SELECT " + DbHandler.KEY_ITEM_STATUS + " FROM " + DbHandler.TABLE_USERS_ITEMS + " WHERE " + DbHandler.KEY_USER_ID + " = " + DbHandler.USER_ID + " AND " + DbHandler.KEY_ITEM_ID + " = " + id;
                                        Statement st2 = connection.createStatement();
                                        ResultSet rs2 = st2.executeQuery(SQL);
                                        String itemStatus = "";
                                        if (rs2 != null) {
                                            if (rs2.next()) {
                                                itemStatus = rs2.getString(1);
                                                contentValues.put(DbHandler.KEY_ITEM_STATUS, itemStatus);
                                                mDb.update(DbHandler.TABLE_ITEMS, contentValues, DbHandler.KEY_ID + " = " + id, null);
                                            }
                                            rs2.close();
                                        }
                                        st2.close();

                                        rs1.close();
                                    }
                                    st1.close();
                                }
                            } catch (ParseException e) {
                                SQL = "SELECT * FROM " + DbHandler.TABLE_ITEMS + " WHERE " + DbHandler.KEY_ID + " = " + this.itemId;
                                Statement st1 = connection.createStatement();
                                ResultSet rs1 = st1.executeQuery(SQL);

                                if (rs1 != null && !isCancelled()) {
                                    rs1.next();
                                    int id = rs1.getInt(1);
                                    String tname = rs1.getString(2);
                                    if (status.equals("item")) name = tname;
                                    String desc = rs1.getString(3);
                                    int collectionId = rs1.getInt(5);
                                    blob = rs1.getBytes(7);
                                    String serverDateStr = rs1.getString(8);

                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put(DbHandler.KEY_ID, id);
                                    contentValues.put(DbHandler.KEY_SECTION_ID, secId);
                                    contentValues.put(DbHandler.KEY_NAME, tname);
                                    contentValues.put(DbHandler.KEY_DESCRIPTION, desc);
                                    contentValues.put(DbHandler.KEY_COLLECTION_ID, collectionId);
                                    contentValues.put(DbHandler.KEY_MINI_IMAGE, blob);

                                    contentValues.put(DbHandler.KEY_DATE_OF_CHANGE, serverDateStr);

                                    if(DbHandler.USER_ID != 1) {
                                        SQL = "SELECT " + DbHandler.KEY_ITEM_STATUS + " FROM " + DbHandler.TABLE_USERS_ITEMS + " WHERE " + DbHandler.KEY_USER_ID + " = " + DbHandler.USER_ID + " AND " + DbHandler.KEY_ITEM_ID + " = " + id;
                                        Statement st2 = connection.createStatement();
                                        ResultSet rs2 = st2.executeQuery(SQL);
                                        String itemStatus;
                                        if (rs2 != null) {
                                            if (rs2.next()) {
                                                itemStatus = rs2.getString(1);
                                                contentValues.put(DbHandler.KEY_ITEM_STATUS, itemStatus);
                                                mDb.update(DbHandler.TABLE_ITEMS, contentValues, DbHandler.KEY_ID + " = " + id, null);
                                            }
                                            rs2.close();
                                        }
                                        st2.close();
                                    }

                                    rs1.close();
                                }
                                st1.close();
                            }
                            rs.close();
                        }
                        st.close();
                    }
                } else {
                    if (connection != null) {
                        SQL = "SELECT " + DbHandler.KEY_ID + ", " +
                                DbHandler.KEY_NAME + ", " +
                                DbHandler.KEY_DESCRIPTION + ", " +
                                DbHandler.KEY_COLLECTION_ID + ", " +
                                DbHandler.KEY_MINI_IMAGE + ", " +
                                DbHandler.KEY_DATE_OF_CHANGE +
                                " FROM " + DbHandler.TABLE_ITEMS + " WHERE " + DbHandler.KEY_ID + " = " + this.itemId;
                        st = connection.createStatement();
                        rs = st.executeQuery(SQL);

                        if (rs != null && !isCancelled()) {
                            rs.next();
                            int id = rs.getInt(1);
                            String tname = rs.getString(2);
                            if (status.equals("item")) name = tname;
                            String desc = rs.getString(3);
                            int collectionId = rs.getInt(4);
                            blob = rs.getBytes(5);
                            String serverDateStr = rs.getString(6);

                            ContentValues contentValues = new ContentValues();
                            contentValues.put(DbHandler.KEY_ID, id);
                            contentValues.put(DbHandler.KEY_SECTION_ID, secId);
                            contentValues.put(DbHandler.KEY_NAME, tname);
                            contentValues.put(DbHandler.KEY_DESCRIPTION, desc);
                            contentValues.put(DbHandler.KEY_COLLECTION_ID, collectionId);
                            contentValues.put(DbHandler.KEY_MINI_IMAGE, blob);
                            contentValues.put(DbHandler.KEY_ITEM_STATUS, DbHandler.STATUS_MISS);
                            contentValues.put(DbHandler.KEY_DATE_OF_CHANGE, serverDateStr);
                            mDb.insert(DbHandler.TABLE_ITEMS, null, contentValues);

                            rs.close();
                            st.close();
                        }
                    }
                }

                cursor.close();
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(blob, 0, blob.length, o);


                int size = calculateInSampleSize(o, maxImageSize, maxImageSize);
                if (size < 1) size = 1;
                o = new BitmapFactory.Options();
                o.inSampleSize = size;
                o.inPreferredConfig = Bitmap.Config.RGB_565;

                return BitmapFactory.decodeByteArray(blob, 0, blob.length, o);
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (st != null) st.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if (bitmap != null) {

            final ImageView imageView = new ImageView(context);
            imageView.setImageBitmap(bitmap);
//            imageView.setBackground(gradientBackground);
//            imageView.setLayoutParams(getCardParams(countImages));
//            imageView.setPadding(puddingsSize, puddingsSize, puddingsSize, puddingsSize);

//            imageView.setId(tempId);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clearOffers();
                    switch (status) {
                        case "item": {
                            CurrentItemFragment currentItemFragment = new CurrentItemFragment();
                            Bundle bundle = new Bundle();
                            bundle.putInt("id", itemId);
                            bundle.putInt("itemId", itemId);
                            bundle.putInt("sectionId", secId);
                            bundle.putString(DbHandler.COL_TYPE, fragmentType);
                            bundle.putString("collectionTitle", collectionTitle);
                            bundle.putString("sectionTitle", sectionTitle);
                            currentItemFragment.setArguments(bundle);
                            fragmentManager.beginTransaction().replace(R.id.content_frame, currentItemFragment).commit();
                            break;
                        }
                        case "section": {
                            AsyncDrawAllItems drawAllItems = new AsyncDrawAllItems(layout, name, secId, context, mDb, actionBar,  fragmentManager, userId);
                            drawAllItems.execute();
                            break;
                        }
                        case "collection": {
                            AsyncDrawAllSections drawAllSections = new AsyncDrawAllSections(layout, collectionId, name, context, mDb, actionBar, fragmentManager, userId);
                            drawAllSections.execute();
                            break;
                        }
                    }
                }
            });

            if (status.equals("item") && DbHandler.isUserLogin()) {
                imageView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Cursor cursor = mDb.query(DbHandler.TABLE_ITEMS, new String[]{DbHandler.KEY_ITEM_STATUS}, DbHandler.KEY_ID + " = " + itemId, null, null, null, null);
                        if (cursor.moveToFirst()) {
                            String currentStatus = cursor.getString(cursor.getColumnIndex(DbHandler.KEY_ITEM_STATUS));
                            String setStatus = currentStatus.equals(DbHandler.STATUS_IN) || currentStatus.equals(DbHandler.STATUS_TRADE) ? DbHandler.STATUS_MISS : DbHandler.STATUS_IN;
                            inMyCollection = setStatus.equals(DbHandler.STATUS_IN);
                            AsyncSetItemStatus asyncSetItemStatus = new AsyncSetItemStatus(itemId, context);
                            asyncSetItemStatus.execute(setStatus);

                            setInMyCollection(imageView);
                        }
                        cursor.close();
                        return false;
                    }
                });

                if (fragmentType.equals(DbHandler.COM_COLLECTIONS)) {
                    Cursor cursor = mDb.query(DbHandler.TABLE_ITEMS, new String[]{DbHandler.KEY_ITEM_STATUS}, DbHandler.KEY_ID + " = " + itemId, null, null, null, null);
                    if (cursor.moveToFirst()) {
                        String currentStatus = cursor.getString(cursor.getColumnIndex(DbHandler.KEY_ITEM_STATUS));
                        if (currentStatus.equals(DbHandler.STATUS_IN) || currentStatus.equals(DbHandler.STATUS_TRADE))
                            setInMyCollection(imageView);
                    }
                    cursor.close();
                }
            }

            if (inMyCollection) setInMyCollection(imageView);



            CardView cardView = new CardView(context);
            tempId = View.generateViewId();
            cardView.setId(tempId);
            cardView.setLayoutParams(getCardParams(countImages++, bitmap));
            cardView.addView(imageView);

            layout.addView(cardView, currentId++);
//            layout.addView(getNewTextView(name, cardView));
//            layout.addView(, currentId++);
//            countImages++;
        }
    }

    private CardView getNewTextView(String text, CardView imageCardView)
    {
        TextView textView = new TextView(context);
        textView.setTextColor(Color.parseColor("#000000"));
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(Typeface.DEFAULT_BOLD);

        RelativeLayout.LayoutParams cardViewParams = new RelativeLayout.LayoutParams(maxImageSize, ViewGroup.LayoutParams.WRAP_CONTENT);
        CardView cardView = new CardView(context);
        cardView.addView(textView);
        cardViewParams.addRule(RelativeLayout.BELOW, imageCardView.getId());

//        cardViewParams.addRule(RelativeLayout., imageCardView.getId());
        cardViewParams.addRule(countImages % 2 != 0 ? RelativeLayout.ALIGN_PARENT_START : RelativeLayout.ALIGN_PARENT_RIGHT);
        cardViewParams.setMargins(externalMargins, (int)dp * 4, externalMargins, 0);
        cardView.setBackgroundColor(Color.parseColor("#8EE0E0E0"));
        cardView.setLayoutParams(cardViewParams);

        return cardView;
    }

    private int getMaxWidth(Bitmap bitmap){
        return bitmap.getWidth() > maxImageSize ? maxImageSize : bitmap.getWidth();
    }

    private int getMaxHeight(Bitmap bitmap){
        double width = getMaxWidth(bitmap);
        double k = width/bitmap.getWidth();
        return (int)(bitmap.getHeight() * k);
    }

    private RelativeLayout.LayoutParams getCardParams(int countImages, Bitmap bitmap)
    {


        RelativeLayout.LayoutParams cardParams = new RelativeLayout.LayoutParams(getMaxWidth(bitmap), getMaxHeight(bitmap));
        if (countImages % 2 == 0) {
            if (lastLeftId == -1) {
                cardParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                cardParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                cardParams.setMargins(externalMargins, firstTopMargin, 0, 0);
            } else {
                cardParams.addRule(RelativeLayout.BELOW, lastLeftId);
                cardParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                cardParams.setMargins(externalMargins, topMargin, 0, 0);
            }
            lastLeftId = tempId;
        } else {
            if (lastRightId == -1) {
                cardParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                cardParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                cardParams.setMargins(0, firstTopMargin, externalMargins, 0);
            } else {
                cardParams.addRule(RelativeLayout.BELOW, lastRightId);
                cardParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                cardParams.setMargins(0, topMargin, externalMargins, 0);
            }
            lastRightId = tempId;
        }
        return cardParams;
    }

    private void setInMyCollection(ImageView imageView){
        ImageView accept = new ImageView(context);
        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(maxImageSize /5, maxImageSize /5);
        imageParams.addRule(RelativeLayout.ALIGN_LEFT, imageView.getId());
        imageParams.addRule(RelativeLayout.ALIGN_BOTTOM, imageView.getId());
        imageParams.setMargins(checkImageSize, 0, 0, 0);
        accept.setLayoutParams(imageParams);
        if(fragmentType.equals("comCollections"))
            accept.setImageResource(R.drawable.i_have_it_button);
        else
            accept.setImageResource(R.drawable.ic_delete_from_mycolls);
        layout.addView(accept);
    }
}
