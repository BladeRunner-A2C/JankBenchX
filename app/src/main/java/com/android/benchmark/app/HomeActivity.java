/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 *
 */

package com.android.benchmark.app;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.benchmark.R;
import com.android.benchmark.api.JankBenchAPI;
import com.android.benchmark.config.Constants;
import com.android.benchmark.registry.BenchmarkRegistry;
import com.android.benchmark.results.GlobalResultsStore;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.topjohnwu.superuser.Shell;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class HomeActivity extends AppCompatActivity implements Button.OnClickListener {

    static {
        /* Shell.Config methods shall be called before any shell is created
         * This is the why in this example we call it in a static block
         * The followings are some examples, check Javadoc for more details */
        Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR);
        Shell.Config.setTimeout(10);
    }

    private FloatingActionButton mStartButton;
    private BenchmarkRegistry mRegistry;
    private Queue<Intent> mRunnableBenchmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mStartButton = (FloatingActionButton) findViewById(R.id.start_button);
        mStartButton.setActivated(true);
        mStartButton.setOnClickListener(this);

        mRegistry = new BenchmarkRegistry(this);

        mRunnableBenchmarks = new LinkedList<>();

        ExpandableListView listView = (ExpandableListView) findViewById(R.id.test_list);
        BenchmarkListAdapter adapter =
                new BenchmarkListAdapter(LayoutInflater.from(this), mRegistry);
        listView.setAdapter(adapter);

        adapter.notifyDataSetChanged();
        ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
        layoutParams.height = 2048;
        listView.setLayoutParams(layoutParams);
        listView.requestLayout();
        System.out.println(System.getProperties().stringPropertyNames());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        HomeActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(HomeActivity.this, "Exporting...", Toast.LENGTH_LONG).show();
                            }
                        });
                        GlobalResultsStore.getInstance(HomeActivity.this).exportToCsv();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    HomeActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeActivity.this, "Done", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }.execute();

            return true;
        } else if (id == R.id.action_upload) {

            new AsyncTask<Void, Void, Void>() {
                boolean success;

                @Override
                protected Void doInBackground(Void... voids) {
                    HomeActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeActivity.this, "Uploading results...", Toast.LENGTH_LONG).show();
                        }
                    });

                    success = JankBenchAPI.uploadResults(HomeActivity.this, Constants.BASE_URL); // TODO: Change baseURL

                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    HomeActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HomeActivity.this, success ? "Upload succeeded" : "Upload failed", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }.execute();

            return true;
        } else if (id == R.id.action_view_results) {
            Uri webpage = Uri.parse("https://jankbenchx.vercel.app");
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        final int groupCount = mRegistry.getGroupCount();
        for (int i = 0; i < groupCount; i++) {

            Intent intent = mRegistry.getBenchmarkGroup(i).getIntent();
            if (intent != null) {
                mRunnableBenchmarks.add(intent);
            }
        }

        handleNextBenchmark();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleNextBenchmark() {
        Intent nextIntent = mRunnableBenchmarks.peek();
        startActivityForResult(nextIntent, 0);
    }
}
