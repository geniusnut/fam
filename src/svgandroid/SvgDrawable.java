package svgandroid;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Build;

public class SvgDrawable extends Drawable {
	public static final boolean HAS_HW_ACCELERATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

	private final int mIntrinsicWidth;
	private final int mIntrinsicHeight;
	private final Picture mPicture;
	private Bitmap mBitmap;
	private Matrix mMatrix;
	private Paint mPaint;

	public SvgDrawable(Picture picture, int intrinsicWidth, int intrinsicHeight) {
		mPicture = picture;
		mIntrinsicWidth = intrinsicWidth;
		mIntrinsicHeight = intrinsicHeight;
	}

	public Picture getPicture() {
		return mPicture;
	}

	@Override
	public void draw(Canvas canvas) {
		final Rect bounds = getBounds();
		if (mBitmap != null) {
			final float x = (bounds.left + bounds.right - mBitmap.getWidth()) / 2f;
			final float y = (bounds.top + bounds.bottom - mBitmap.getHeight()) / 2f;
			canvas.drawBitmap(mBitmap, x, y, mPaint);
		} else {
			canvas.save(Canvas.CLIP_SAVE_FLAG | Canvas.MATRIX_SAVE_FLAG);
			canvas.clipRect(bounds);
			if (mMatrix != null)
				canvas.concat(mMatrix);
			mPicture.draw(canvas);
			canvas.restore();
		}
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		final int cxBounds = bounds.right - bounds.left;
		final int cyBounds = bounds.bottom - bounds.top;
		final int cxPicture = mPicture.getWidth();
		final int cyPicture = mPicture.getHeight();
		final float scaleX = (float) cxBounds / cxPicture;
		final float scaleY = (float) cyBounds / cyPicture;
		final float scale = Math.min(scaleX, scaleY);

		if (HAS_HW_ACCELERATION || mPaint != null) {
			final int width = Math.round(cxPicture * scale);
			final int height = Math.round(cyPicture * scale);

			if (mBitmap != null) {
				if (mBitmap.getWidth() == width && mBitmap.getHeight() == height)
					return;
				mBitmap.recycle();
				mBitmap = null;
			}

			//  drawing a picture to a hardware acceleration can not work, so we create a bitmap as a buffer
			try {
				mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				if (mBitmap != null) {
					final Canvas canvas = new Canvas(mBitmap);
					canvas.scale(scale, scale);
					canvas.translate((width - cxPicture * scale) / 2, (height - cyPicture * scale) / 2);
					mPicture.draw(canvas);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		} else {
			if (mMatrix == null)
				mMatrix = new Matrix();
			else
				mMatrix.reset();
			mMatrix.postScale(scale, scale);
			mMatrix.postTranslate((cxBounds - cxPicture * scale) / 2 + bounds.left,
					(cyBounds - cyPicture * scale) / 2 + bounds.top);
		}
	}

	@Override
	public int getIntrinsicWidth() {
		// Returns -1 if it has no intrinsic width, since SVG can scaled smoothly.
		return mIntrinsicWidth;
	}

	@Override
	public int getIntrinsicHeight() {
		// Returns -1 if it has no intrinsic height, since SVG can scaled smoothly.
		return mIntrinsicHeight;
	}

	@Override
	public int getOpacity() {
		// Not sure, so be safe
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int alpha) {
	}

	@Override
	public void setColorFilter(ColorFilter filter) {
		if (filter != null) {
			if (mPaint == null)
				mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
			mPaint.setColorFilter(filter);
		} else {
			mPaint = null;
		}
		if (!HAS_HW_ACCELERATION)
			onBoundsChange(getBounds());
		invalidateSelf();
	}

	@Override
	public void setDither(boolean dither) {
	}

	@Override
	public void setFilterBitmap(boolean filter) {
	}
}
