package com.lobotino.collector.fragments;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Resources;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.lobotino.collector.R;
import com.lobotino.collector.activities.MainActivity;
import com.lobotino.collector.utils.DbHandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static com.lobotino.collector.activities.MainActivity.dbHandler;


public class ProfileFragment extends Fragment {

    private int avatarSize, screenWight, screenHeight;
    private View rootView;

    protected TextView userName, lastActivity, userEmail;
    protected ActionBar actionBar;

    private Button btnAllCollections, btnWishes, btnTrade;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_user_profile, container, false);
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.setCurrentFragment(this);

        final int user_id = getArguments().getInt("user_id");


        SQLiteDatabase mDb;
        try {
            mDb = dbHandler.getWritableDatabase();
        } catch (SQLException mSQLException) {
            throw mSQLException;
        }

        Context context = getActivity().getBaseContext();
        screenWight = context.getResources().getDisplayMetrics().widthPixels;
        screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        avatarSize = Math.round((float) (screenWight / 3));

        btnAllCollections = rootView.findViewById(R.id.btn_list_i_have);
        btnWishes         = rootView.findViewById(R.id.btn_list_i_wish);
        btnTrade         = rootView.findViewById(R.id.btn_trade);

        float dip = 17f;
        Resources r = getResources();
        int margins = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics()));
        int secondMargins = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, r.getDisplayMetrics()));

        btnWishes.setWidth(screenWight/2 - margins - secondMargins);
        btnTrade.setWidth(screenWight/2 - margins - secondMargins);


        btnAllCollections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager
                        .beginTransaction();
                CollectionsFragment currentFragment = new CollectionsFragment();
                Bundle bundle = new Bundle();
                bundle.putString(DbHandler.COL_TYPE, DbHandler.USER_ALL_COLLECTIONS);
                bundle.putString("status", "all");
                bundle.putInt("currentUserId", user_id);
                currentFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.content_frame, currentFragment);
                fragmentTransaction.commit();
            }
        });

        btnWishes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager
                        .beginTransaction();
                CollectionsFragment currentFragment = new CollectionsFragment();
                Bundle bundle = new Bundle();
                bundle.putString(DbHandler.COL_TYPE, DbHandler.USER_WISH_COLLECTIONS);
                bundle.putString("status", "all");
                bundle.putInt("currentUserId", user_id);
                currentFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.content_frame, currentFragment);
                fragmentTransaction.commit();
            }
        });

        btnTrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager
                        .beginTransaction();
                CollectionsFragment currentFragment = new CollectionsFragment();
                Bundle bundle = new Bundle();
                bundle.putString(DbHandler.COL_TYPE, DbHandler.USER_TRADE_COLLECTIONS);
                bundle.putString("status", "all");
                bundle.putInt("currentUserId", user_id);
                currentFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.content_frame, currentFragment);
                fragmentTransaction.commit();
            }
        });



        ImageView avatar = rootView.findViewById(R.id.ivAvatar);

        userEmail = rootView.findViewById(R.id.tvUserEmail);
        userName = rootView.findViewById(R.id.tvUserName);
        lastActivity = rootView.findViewById(R.id.tvUserLastActivity);

        actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();

        if(user_id == DbHandler.USER_ID) {
            userName.setText(DbHandler.USER_LOGIN);
            userEmail.setText(DbHandler.USER_EMAIL);
            actionBar.setTitle("Мой профиль");
            lastActivity.setText("");
        }else{
            new AsyncProfileTask(user_id).execute();
        }

        return rootView;
    }


    private class AsyncProfileTask extends AsyncTask<Void, Void, List<String>>{

        int userId;

        public AsyncProfileTask(int userId) {
            this.userId = userId;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        protected List<String> doInBackground(Void... voids) {

            Connection connection = dbHandler.getConnection(getContext());
            Statement st = null;
            ResultSet rs = null;

            if (connection != null) {
                String SQL = "SELECT * FROM " + DbHandler.TABLE_USERS + " WHERE " + DbHandler.KEY_ID + " = " + userId;
                try {
                    st = connection.createStatement();
                    rs = st.executeQuery(SQL);

                    if (rs != null && !isCancelled()) {
                        rs.next();
                        String userName = rs.getString(3);
                        String userEmail = rs.getString(7);
                        String lastActivity = rs.getString(8);
                        List<String> list = new ArrayList<>();
                        list.add(userName);
                        list.add(userEmail);
                        list.add(lastActivity);

                        st.close();
                        rs.close();
                        return list;
                    }
                } catch (java.sql.SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (rs != null) rs.close();
                        if (st != null) st.close();
                    } catch (java.sql.SQLException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<String> list) {
            String name = list.get(0);
            String email = list.get(1);
            String lastAct = list.get(2);

            if(name.equals(DbHandler.USER_LOGIN)) {
                btnAllCollections.setEnabled(false);
            }

            actionBar.setTitle("Профиль " + name);
            userName.setText(name);
            userEmail.setText(email);
            lastActivity.setText(lastAct);

        }
    }
}
