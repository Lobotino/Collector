package com.lobotino.collector;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button btnAdd, btnRead, btnClear;
    EditText etCollection, etSet, etElement;
    TextView tvResult;

    DbHandler dbHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(this);

        btnRead = (Button) findViewById(R.id.btnRead);
        btnRead.setOnClickListener(this);

        btnClear = (Button) findViewById(R.id.btnClear);
        btnClear.setOnClickListener(this);

        etCollection = (EditText) findViewById(R.id.etCollection);
        etSet = (EditText) findViewById(R.id.etSet);
        etElement = (EditText) findViewById(R.id.etElement);

        tvResult = (TextView) findViewById(R.id.result);

        dbHandler = new DbHandler(this);
    }



    @Override
    public void onClick(View v) {

        String collection = etCollection.getText().toString();
        String set = etSet.getText().toString();
        String element = etElement.getText().toString();

        SQLiteDatabase database = dbHandler.getWritableDatabase();

        ContentValues contentValues = new ContentValues();


        switch (v.getId()) {

            case R.id.btnAdd: {
                contentValues.put(DbHandler.KEY_COLLECTION, collection);
                contentValues.put(DbHandler.KEY_SET, set);
                contentValues.put(DbHandler.KEY_ELEMENT, element);

                database.insert(DbHandler.TABLE_NAME, null, contentValues);

                etCollection.setText("");
                etSet.setText("");
                etElement.setText("");
                break;
            }

            case R.id.btnRead: {
                Cursor cursor = database.query(DbHandler.TABLE_NAME, null, null, null, null, null, null);
                String result = "";
                if (cursor.moveToFirst()) {
                    int colIndex = cursor.getColumnIndex(DbHandler.KEY_COLLECTION);
                    int setIndex = cursor.getColumnIndex(DbHandler.KEY_SET);
                    int elemIndex = cursor.getColumnIndex(DbHandler.KEY_ELEMENT);
                    do {
                        result = result + cursor.getString(colIndex) +
                                ", " + cursor.getString(setIndex) +
                                ", " + cursor.getString(elemIndex) + "\n";
                    } while (cursor.moveToNext());
                } else
                    result = "0 rows";

                tvResult.setText(result);
                cursor.close();
                break;
            }

            case R.id.btnClear: {
                database.delete(DbHandler.TABLE_NAME, null, null);
                break;
            }
        }
        dbHandler.close();
    }
}
