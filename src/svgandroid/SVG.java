package svgandroid;

import android.content.res.Resources;
import android.graphics.Picture;
import android.graphics.RectF;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

/**
 * Describes a vector Picture object, and optionally its bounds.
 *
 * @author Larva Labs, LLC
 */
public class SVG {
	private static final ConcurrentHashMap<Integer, SoftReference<SVG>> mCache = new ConcurrentHashMap<Integer, SoftReference<SVG>>(64);

	public static SvgDrawable getDrawable(Resources res, int resId) {
		return loadSVG(res, resId).createDrawable();
	}

	public static SvgDrawable getDrawable(Resources res, int resId, int defaultColor) {
		return loadSVG(res, resId, defaultColor).createDrawable();
	}

	public static SvgDrawable getDrawable(Resources res, int resId, int defaultColor, int intrinsicSize) {
		return loadSVG(res, resId, defaultColor).createDrawable(intrinsicSize);
	}

	private static SVG getFromCache(int resId) {
		synchronized (mCache) {
			final SoftReference<SVG> ref = mCache.get(resId);
			return ref != null ? ref.get() : null;
		}
	}

	private static void putToCache(int resId, SVG svg) {
		synchronized (mCache) {
			mCache.put(resId, new SoftReference<SVG>(svg));
		}
	}

	public static SVG loadSVG(Resources res, int resId) {
		SVG svg = getFromCache(resId);
		if (svg == null) {
			svg = SVGParser.getSVGFromResource(res, resId);
			putToCache(resId, svg);
		}
		return svg;
	}

	public static SVG loadSVG(Resources res, int resId, int defaultColor) {
		SVG svg = getFromCache(resId);
		if (svg == null || svg.defaultColor != defaultColor) {
			svg = SVGParser.getSVGFromResource(res, resId, defaultColor);
			svg.defaultColor = defaultColor;
			putToCache(resId, svg);
		}
		return svg;
	}

	private int defaultColor = 0;

	/**
	 * The parsed Picture object.
	 */
	private Picture picture;

	/**
	 * These are the bounds for the SVG specified as a hidden "bounds" layer in the SVG.
	 */
	private RectF bounds;

	/**
	 * These are the estimated bounds of the SVG computed from the SVG elements while parsing.
	 * Note that this could be null if there was a failure to compute limits (ie. an empty SVG).
	 */
	private RectF limits = null;

	/**
	 * Construct a new SVG.
	 *
	 * @param picture the parsed picture object.
	 * @param bounds  the bounds computed from the "bounds" layer in the SVG.
	 */
	SVG(Picture picture, RectF bounds) {
		this.picture = picture;
		this.bounds = bounds;
	}

	/**
	 * Set the limits of the SVG, which are the estimated bounds computed by the parser.
	 *
	 * @param limits the bounds computed while parsing the SVG, may not be entirely accurate.
	 */
	void setLimits(RectF limits) {
		this.limits = limits;
	}

	/**
	 * Create a picture drawable from the SVG.
	 *
	 * @return the PictureDrawable.
	 */
	public SvgDrawable createDrawable() {
		return new SvgDrawable(picture, -1, -1);
	}

	public SvgDrawable createDrawable(int intrinsicWidth, int intrinsicHeight) {
		return new SvgDrawable(picture, intrinsicWidth, intrinsicHeight);
	}

	public SvgDrawable createDrawable(int intrinsicSize) {
		return new SvgDrawable(picture, intrinsicSize, intrinsicSize);
/*		final int width0 = picture.getWidth();
		final int height0 = picture.getHeight();
		if (width == 0)
			width = width0;
		if (height == 0)
			height = height0;

		try {
			final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			final Canvas canvas = new Canvas(bitmap);
			if (width != width0 || height != height0)
				canvas.scale((float) width / width0, (float) height / height0);
			picture.draw(canvas);
			return new BitmapDrawable(res, bitmap);
		} catch (Throwable e) {
		}

		return new PictureDrawable(picture) {
			@Override
			public int getIntrinsicWidth() {
				if (bounds != null) {
					return (int) bounds.width();
				} else if (limits != null) {
					return (int) limits.width();
				} else {
					return -1;
				}
			}

			@Override
			public int getIntrinsicHeight() {
				if (bounds != null) {
					return (int) bounds.height();
				} else if (limits != null) {
					return (int) limits.height();
				} else {
					return -1;
				}
			}
		};
*/
	}

	/**
	 * Get the parsed SVG picture data.
	 *
	 * @return the picture.
	 */
	public Picture getPicture() {
		return picture;
	}

	/**
	 * Gets the bounding rectangle for the SVG, if one was specified.
	 *
	 * @return rectangle representing the bounds.
	 */
	public RectF getBounds() {
		return bounds;
	}

	/**
	 * Gets the bounding rectangle for the SVG that was computed upon parsing. It may not be entirely accurate for certain curves or transformations, but is often better than nothing.
	 *
	 * @return rectangle representing the computed bounds.
	 */
	public RectF getLimits() {
		return limits;
	}
}
