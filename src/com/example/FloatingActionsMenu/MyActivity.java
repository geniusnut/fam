package com.example.FloatingActionsMenu;

import android.app.Activity;
import android.os.Bundle;

public class MyActivity extends Activity {
	/**
	 * Called when the activity is first created.
	 */
	private FloatingActionsMenu mFam;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mFam = (FloatingActionsMenu) findViewById(R.id.fam);

		for (int i = 0; i < 4; i++) {
			FloatingActionButton fab = new FloatingActionButton.Builder(this)
					.withDrawable(FloatingActionsMenu.createLikeDrawable(this, false))
					.withSize(64) // dp
					.withMargins(16, 0, 0, 16)// dp
					.create();
			fab.setTitle("This is the " + Integer.toString(i) + " fab. Long - long - long Long - long - long Long - long - long Long - long - long Long - long - long Long - long - long Long - long - long ");
			mFam.addButton(fab);
		}
	}
}
