package com.example.FloatingActionsMenu;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

public class FloatingActionButton extends View {

	private Context context;
	Paint mButtonPaint;
	Paint mDrawablePaint;
	Bitmap mBitmap;
	FrameLayout.LayoutParams mParams;
	protected final Paint mPaintThin = new Paint(Paint.ANTI_ALIAS_FLAG);

	boolean mHidden = false;
	public boolean mLiked = false;
	public long mLikes = 0;
	private String mTitle;

	public FloatingActionButton(Context context, FrameLayout.LayoutParams params) {
		super(context);
		this.context = context;
		mParams = params;
		init(Color.WHITE);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void init(int color) {
		setWillNotDraw(false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mButtonPaint.setColor(color);
		mButtonPaint.setStyle(Paint.Style.FILL);
		mButtonPaint.setShadowLayer(10.0f, 0.0f, 3.5f, Color.argb(100, 0, 0, 0));
		mDrawablePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(mParams.width, mParams.height);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		setClickable(true);
		//
		canvas.drawCircle(getWidth() / 2, getHeight() / 2, (float) (getWidth() / 2.6), mButtonPaint);
		// draw Bitmap
		canvas.drawBitmap(mBitmap, (getWidth() - mBitmap.getWidth()) / 2,
				(getHeight() - mBitmap.getHeight()) / 1.8f, mDrawablePaint);
		// drawText
		String likes = convertToStr(mLikes);
		mPaintThin.setTextSize(mBitmap.getWidth() / 3.0f);
		RectF bounds = new RectF(new Rect(0, 0, getWidth(), getHeight()));
		bounds.right = mPaintThin.measureText(likes, 0, likes.length());
		bounds.bottom = mPaintThin.descent() - mPaintThin.ascent();

		bounds.left += (getWidth() - bounds.right) / 2.0f;
		bounds.top += (getHeight() - bounds.bottom) / 2.0f;

		mPaintThin.setColor(mLiked ? mButtonPaint.getColor() : Color.WHITE);//getResources().getColor(android.R.color.holo_blue_dark));
		canvas.drawText(likes, bounds.left, bounds.top - mPaintThin.ascent(), mPaintThin);
	}

	private String convertToStr(long mLikes) {
		return String.valueOf(mLikes);
	}

	public void setLikes(long likes) {
		mLikes = likes;
		invalidate();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			setAlpha(1.0f);
		} else if (event.getAction() == MotionEvent.ACTION_DOWN) {
			setAlpha(0.6f);
		}
		return super.onTouchEvent(event);
	}

	public void setColor(int color) {
		init(color);
	}

	public void setDrawable(Drawable drawable) {
		int w = drawable.getIntrinsicWidth();
		int h = drawable.getIntrinsicHeight();
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		mBitmap = Bitmap.createBitmap(w, h, config);
		Canvas canvas = new Canvas(mBitmap);
		drawable.setBounds(0, 0, w, h);
		drawable.draw(canvas);
		// mBitmap = ((BitmapDrawable) drawable).getBitmap();
		invalidate();
	}

	public void setTitle(String title) {
		mTitle = title;
		TextView label = getLabelView();
		if (label != null) {
			label.setText(title);
		}
	}

	TextView getLabelView() {
		return (TextView) getTag(R.id.fab_label);
	}

	public String getTitle() {
		return mTitle;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void hide() {
		if (!mHidden) {
			ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1, 0);
			ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1, 0);
			AnimatorSet animSetXY = new AnimatorSet();
			animSetXY.playTogether(scaleX, scaleY);
			animSetXY.setInterpolator(new AccelerateInterpolator());
			animSetXY.setDuration(100);
			animSetXY.start();
			mHidden = true;
		}
	}

	public void setMargins(int left, int top, int right, int bottom) {
		mParams.setMargins(left, top, right, bottom);
		this.setLayoutParams(mParams);
	}

	public void setLiked(boolean liked) {
		mLiked = liked;
		invalidate();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void show() {
		if (mHidden) {
			ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 0, 1);
			ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 0, 1);
			AnimatorSet animSetXY = new AnimatorSet();
			animSetXY.playTogether(scaleX, scaleY);
			animSetXY.setInterpolator(new OvershootInterpolator());
			animSetXY.setDuration(200);
			animSetXY.start();
			mHidden = false;
		}
	}
	@Override
	public void setVisibility(int visibility) {
		TextView label = getLabelView();
		if (label != null) {
			label.setVisibility(visibility);
		}

		super.setVisibility(visibility);
	}


	public boolean isHidden() {
		return mHidden;
	}

	public static class Builder {
		private FrameLayout.LayoutParams params;
		// private final Activity activity;
		private Context context;
		private ViewGroup root;
		int gravity = Gravity.BOTTOM | Gravity.RIGHT; // default bottom right
		Drawable drawable;
		int color = Color.WHITE;
		int size = 0;
		float scale = 0;

		/**
		 * Constructor using a context for this builder and the
		 * {@link FloatingActionButton} it creates
		 *
		 * @param context
		 */
		public Builder(Context context) {
			scale = context.getResources().getDisplayMetrics().density;
			// The calculation (value * scale + 0.5f) is a widely used to convert to dps to pixel
			// units based on density scale
			// see <a href="http://developer.android.com/guide/practices/screens_support.html">
			// developer.android.com (Supporting Multiple Screen Sizes)</a>
			size = (int) (72 * scale + 0.5f); // default size is 72dp by 72dp
			params = new FrameLayout.LayoutParams(size, size);
			params.gravity = gravity;
			this.root = root;
			this.context = context;
		}

		/**
		 * Sets the FAB gravity.
		 */
		public Builder withGravity(int gravity) {
			this.gravity = gravity;
			return this;
		}

		/**
		 * Sets the FAB margins in dp.
		 */
		public Builder withMargins(int left, int top, int right, int bottom) {
			params.setMargins((int) (left * scale + 0.5f), (int) (top * scale + 0.5f),
					(int) (right * scale + 0.5f), (int) (bottom * scale + 0.5f));
			return this;
		}

		/**
		 * Sets the FAB drawable.
		 *
		 * @param drawable
		 */
		public Builder withDrawable(final Drawable drawable) {
			this.drawable = drawable;
			return this;
		}

		/**
		 * Sets the FAB color.
		 *
		 * @param color
		 */
		public Builder withColor(final int color) {
			this.color = color;
			return this;
		}

		/**
		 * Sets the FAB size.
		 *
		 * @param size
		 * @return
		 */
		public Builder withSize(int size) {
			size = (int) (size * scale + 0.5f);
			params = new FrameLayout.LayoutParams(size, size);
			return this;
		}

		/**
		 * Creates a {@link FloatingActionButton} with the
		 * arguments supplied to this builder.
		 */
		public FloatingActionButton create() {
			params.gravity = this.gravity;
			final FloatingActionButton button = new FloatingActionButton(context, params);
			button.setColor(this.color);
			button.setDrawable(this.drawable);
			return button;
		}
	}

}