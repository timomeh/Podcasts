package de.timomeh.podcasts.ui.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import de.timomeh.podcasts.R;

public class SearchResultsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Toast.makeText(this, "Searched: " + query, Toast.LENGTH_LONG).show();
        }
    }
}
