package com.yahoo.palagummi.newsreaderapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    ListView listView;
    ArrayList<String> titles = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        articlesDatabase = this.openOrCreateDatabase("articles", MODE_PRIVATE, null);
        articlesDatabase.execSQL("CREATE TABLE IF NOT EXISTS articles (id INT PRIMARY KEY, articleId INT, title VARCHAR, content VARCHAR)");

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //
    }



    public void updateListView() {
        Cursor c = articlesDatabase.rawQuery("SELECT * FROM articles", null);

        int titleIndex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("content");
    }


    public class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1) {
                    char curr = (char) data;
                    result += curr;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);

                int numberOfItems = 20;
                if(jsonArray.length() < numberOfItems)  numberOfItems = jsonArray.length();
                for (int i=0; i<numberOfItems; i++) {
                    // Log.i("JSONItem", jsonArray.getString(i));
                    String articleId = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+ articleId +".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();

                    String articleInfo = "";
                    while (data != -1) {
                        char curr = (char) data;
                        articleInfo += curr;
                        data = reader.read();
                    }
                    JSONObject jsonObject = new JSONObject(articleInfo);

                    // clear the table before adding the top 20 articles
                    articlesDatabase.execSQL("DELETE FROM articles");

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        in = urlConnection.getInputStream();
                        reader = new InputStreamReader(in);
                        data = reader.read();

                        String articleContent = "";
                        while (data != -1) {
                            char curr = (char) data;
                            articleContent += curr;
                            data = reader.read();
                        }
                        // articleContent has the HTML of each article
                        String sql = "INSERT INTO articles (articleId, title, content) VALUES (?, ?, ?)";
                        SQLiteStatement statement = articlesDatabase.compileStatement(sql);

                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleContent);

                        statement.execute();
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
