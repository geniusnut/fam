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

package svgandroid;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Paint.Align;
import android.graphics.Region.Op;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * Entry point for parsing SVG files for Android.
 * Use one of the various static methods for parsing SVGs by resource, asset or input stream.
 * Optionally, a single color can be searched and replaced in the SVG while parsing.
 * You can also parse an svg path directly.
 *
 * @author Larva Labs, LLC
 * @see #getSVGFromResource(android.content.res.Resources, int)
 * @see #getSVGFromAsset(android.content.res.AssetManager, String)
 * @see #getSVGFromString(String)
 * @see #getSVGFromInputStream(java.io.InputStream)
 * @see #parsePath(String)
 */
public class SVGParser {
	static final String TAG = "SVG";
	static float DPI = 72.0f;   // Should be settable

	/**
	 * Parse SVG data from an input stream.
	 *
	 * @param svgData the input stream, with SVG XML data in UTF-8 character encoding.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromInputStream(InputStream svgData) throws SVGParseException {
		return SVGParser.parse(svgData, null, DPI);
	}

	/**
	 * Parse SVG data from a string.
	 *
	 * @param svgData the string containing SVG XML data.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromString(String svgData) throws SVGParseException {
		return SVGParser.parse(new ByteArrayInputStream(svgData.getBytes()), null, DPI);
	}

	/**
	 * Parse SVG data from an Android application resource.
	 *
	 * @param resources the Android context resources.
	 * @param resId     the ID of the raw resource SVG.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromResource(Resources resources, int resId) throws SVGParseException {
		return SVGParser.parse(resources.openRawResource(resId), null, resources.getDisplayMetrics().densityDpi);
	}

	/**
	 * Parse SVG data from an Android application asset.
	 *
	 * @param assetMngr the Android asset manager.
	 * @param svgPath   the path to the SVG file in the application's assets.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 * @throws java.io.IOException       if there was a problem reading the file.
	 */
	public static SVG getSVGFromAsset(AssetManager assetMngr, String svgPath) throws SVGParseException, IOException {
		InputStream inputStream = assetMngr.open(svgPath);
		SVG svg = getSVGFromInputStream(inputStream);
		inputStream.close();
		return svg;
	}

	/**
	 * Parse SVG data from an input stream, replacing a single color with another color.
	 *
	 * @param svgData      the input stream, with SVG XML data in UTF-8 character encoding.
	 * @param defaultColor the default color if not specified.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromInputStream(InputStream svgData, int defaultColor) throws SVGParseException {
		return SVGParser.parse(svgData, defaultColor, DPI);
	}

	/**
	 * Parse SVG data from a string.
	 *
	 * @param svgData      the string containing SVG XML data.
	 * @param defaultColor the default color if not specified.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromString(String svgData, int defaultColor) throws SVGParseException {
		return SVGParser.parse(new ByteArrayInputStream(svgData.getBytes()), defaultColor, DPI);
	}

	/**
	 * Parse SVG data from an Android application resource.
	 *
	 * @param resources    the Android context
	 * @param resId        the ID of the raw resource SVG.
	 * @param defaultColor the default color if not specified.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromResource(Resources resources, int resId, int defaultColor) throws SVGParseException {
		return SVGParser.parse(resources.openRawResource(resId), defaultColor, resources.getDisplayMetrics().densityDpi);
	}

	/**
	 * Parse SVG data from an Android application asset.
	 *
	 * @param assetMngr    the Android asset manager.
	 * @param svgPath      the path to the SVG file in the application's assets.
	 * @param defaultColor the default color if not specified.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 * @throws java.io.IOException       if there was a problem reading the file.
	 */
	public static SVG getSVGFromAsset(AssetManager assetMngr, String svgPath, int defaultColor) throws SVGParseException, IOException {
		InputStream inputStream = assetMngr.open(svgPath);
		SVG svg = getSVGFromInputStream(inputStream, defaultColor);
		inputStream.close();
		return svg;
	}

	/**
	 * Parses a single SVG path and returns it as a <code>android.graphics.Path</code> object.
	 * An example path is <code>M250,150L150,350L350,350Z</code>, which draws a triangle.
	 *
	 * @param pathString the SVG path, see the specification <a href="http://www.w3.org/TR/SVG/paths.html">here</a>.
	 */
	public static Path parsePath(String pathString) {
		return doPath(pathString);
	}

	private static final SAXParserFactory mFactory = SAXParserFactory.newInstance();

	private static SVG parse(InputStream in, Integer defaultColor, float dpi) throws SVGParseException {
		// Log.i(TAG, "parsing svg");
		SVGHandler svgHandler = null;
		//long start = System.currentTimeMillis();
		try {
			SAXParser sp = mFactory.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			final Picture picture = new Picture();
			svgHandler = new SVGHandler(picture);
			svgHandler.setDefaultColor(defaultColor);
			//svgHandler.setWhiteMode(whiteMode);
			svgHandler.setDpi(dpi);
			//if (ignoreDefs) {
			xr.setContentHandler(svgHandler);
			xr.parse(new InputSource(new BufferedInputStream(in)));
			/*} else {
				CopyInputStream cin = new CopyInputStream(in);

				IDHandler idHandler = new IDHandler();
				xr.setContentHandler(idHandler);

				ByteArrayInputStream svgData = cin.getCopy();
				xr.parse(new InputSource(cin.getCopy()));
				svgHandler.idXml = idHandler.idXml;

				xr.setContentHandler(svgHandler);
				xr.parse(new InputSource(cin.getCopy()));
			}*/
			//Log.i(TAG, "Parsing complete in " + (System.currentTimeMillis() - start) + " millis.");
			SVG result = new SVG(picture, svgHandler.bounds);
			// Skip bounds if it was an empty pic
			if (!Float.isInfinite(svgHandler.limits.top)) {
				result.setLimits(svgHandler.limits);
			}
			return result;
		} catch (Exception e) {
			Log.w(TAG, "Parse error: " + e);
			//for (String s : handler.parsed.toString().replace(">", ">\n").split("\n"))
			//      Log.d(TAG, "Parsed: " + s);
			throw new SVGParseException(e);
		}
	}

	public static String escape(String s) {
		return s.replaceAll("\"", "&quot;")
				.replaceAll("'", "&apos")
				.replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;")
				.replaceAll("&", "&amp;");
	}

	private static NumberParse parseNumbers(String s) {
		//Util.debug("Parsing numbers from: '" + s + "'");
		int n = s.length();
		int p = 0;
		ArrayList<Float> numbers = new ArrayList<Float>();
		boolean skipChar = false;
		for (int i = 1; i < n; i++) {
			if (skipChar) {
				skipChar = false;
				continue;
			}
			char c = s.charAt(i);
			switch (c) {
				// This ends the parsing, as we are on the next element
				case 'M':
				case 'm':
				case 'Z':
				case 'z':
				case 'L':
				case 'l':
				case 'H':
				case 'h':
				case 'V':
				case 'v':
				case 'C':
				case 'c':
				case 'S':
				case 's':
				case 'Q':
				case 'q':
				case 'T':
				case 't':
				case 'a':
				case 'A':
				case ')': {
					String str = s.substring(p, i);
					if (str.trim().length() > 0) {
						//Util.debug("  Last: " + str);
						float f = Float.parseFloat(str);
						numbers.add(f);
					}
					p = i;
					return new NumberParse(numbers, p);
				}
				case '\n':
				case '\t':
				case ' ':
				case ',':
				case '-': {
					String str = s.substring(p, i);
					// Just keep moving if multiple whitespace
					if (str.trim().length() > 0) {
						//Util.debug("  Next: " + str);
						float f = Float.parseFloat(str);
						numbers.add(f);
						if (c == '-') {
							p = i;
						} else {
							p = i + 1;
							skipChar = true;
						}
					} else {
						p++;
					}
					break;
				}
			}
		}
		String last = s.substring(p);
		if (last.length() > 0) {
			//Util.debug("  Last: " + last);
			try {
				numbers.add(Float.parseFloat(last));
			} catch (NumberFormatException nfe) {
				// Just white-space, forget it
			}
			p = s.length();
		}
		return new NumberParse(numbers, p);
	}

	// Process a list of transforms
	// foo(n,n,n...) bar(n,n,n..._ ...)
	// delims are whitespace or ,'s

	private static Matrix parseTransform(String s) {
		//Log.d(TAG, s);
		Matrix matrix = new Matrix();
		while (true) {
			parseTransformItem(s, matrix);
			// Log.i(TAG, "Transformed: (" + s + ") " + matrix);
			int rparen = s.indexOf(")");
			if (rparen > 0 && s.length() > rparen + 1) {
				s = s.substring(rparen + 1).replaceFirst("[\\s,]*", "");
			} else {
				break;
			}
		}
		//Log.d(TAG, matrix.toShortString());
		return matrix;
	}

	private static Matrix parseTransformItem(String s, Matrix matrix) {
		if (s.startsWith("matrix(")) {
			NumberParse np = parseNumbers(s.substring("matrix(".length()));
			if (np.numbers.size() == 6) {
				Matrix mat = new Matrix();
				mat.setValues(new float[]{
						// Row 1
						np.numbers.get(0),
						np.numbers.get(2),
						np.numbers.get(4),
						// Row 2
						np.numbers.get(1),
						np.numbers.get(3),
						np.numbers.get(5),
						// Row 3
						0,
						0,
						1,
				});
				matrix.preConcat(mat);
			}
		} else if (s.startsWith("translate(")) {
			NumberParse np = parseNumbers(s.substring("translate(".length()));
			if (np.numbers.size() > 0) {
				float tx = np.numbers.get(0);
				float ty = 0;
				if (np.numbers.size() > 1) {
					ty = np.numbers.get(1);
				}
				matrix.preTranslate(tx, ty);
			}
		} else if (s.startsWith("scale(")) {
			NumberParse np = parseNumbers(s.substring("scale(".length()));
			if (np.numbers.size() > 0) {
				float sx = np.numbers.get(0);
				float sy = sx;
				if (np.numbers.size() > 1) {
					sy = np.numbers.get(1);
				}
				matrix.preScale(sx, sy);
			}
		} else if (s.startsWith("skewX(")) {
			NumberParse np = parseNumbers(s.substring("skewX(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				matrix.preSkew((float) Math.tan(angle), 0);
			}
		} else if (s.startsWith("skewY(")) {
			NumberParse np = parseNumbers(s.substring("skewY(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				matrix.preSkew(0, (float) Math.tan(angle));
			}
		} else if (s.startsWith("rotate(")) {
			NumberParse np = parseNumbers(s.substring("rotate(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				float cx = 0;
				float cy = 0;
				if (np.numbers.size() > 2) {
					cx = np.numbers.get(1);
					cy = np.numbers.get(2);
				}
				matrix.preTranslate(cx, cy);
				matrix.preRotate(angle);
				matrix.preTranslate(-cx, -cy);
			}
		} else {
			Log.w(TAG, "Invalid transform (" + s + ")");
		}
		return matrix;
	}

	private static RectF parseViewBox(String s) {
		final NumberParse np = parseNumbers(s);
		if (np.numbers.size() >= 4)
			return new RectF(np.numbers.get(0), np.numbers.get(1), np.numbers.get(2), np.numbers.get(3));
		return null;
	}

	/**
	 * This is where the hard-to-parse paths are handled.
	 * Uppercase rules are absolute positions, lowercase are relative.
	 * Types of path rules:
	 * <p/>
	 * <ol>
	 * <li>M/m - (x y)+ - Move to (without drawing)
	 * <li>Z/z - (no params) - Close path (back to starting point)
	 * <li>L/l - (x y)+ - Line to
	 * <li>H/h - x+ - Horizontal ine to
	 * <li>V/v - y+ - Vertical line to
	 * <li>C/c - (x1 y1 x2 y2 x y)+ - Cubic bezier to
	 * <li>S/s - (x2 y2 x y)+ - Smooth cubic bezier to (shorthand that assumes the x2, y2 from previous C/S is the x1, y1 of this bezier)
	 * <li>Q/q - (x1 y1 x y)+ - Quadratic bezier to
	 * <li>T/t - (x y)+ - Smooth quadratic bezier to (assumes previous control point is "reflection" of last one w.r.t. to current point)
	 * </ol>
	 * <p/>
	 * Numbers are separate by whitespace, comma or nothing at all (!) if they are self-delimiting, (ie. begin with a - sign)
	 *
	 * @param s the path string from the XML
	 */
	private static Path doPath(String s) {
		int n = s.length();
		ParserHelper ph = new ParserHelper(s, 0);
		ph.skipWhitespace();
		Path p = new Path();
		float lastX = 0;
		float lastY = 0;
		float lastX1 = 0;
		float lastY1 = 0;
		float contourInitialX = 0;
		float contourInitialY = 0;
		RectF r = new RectF();
		char cmd = 'x';
		while (ph.pos < n) {
			char next = s.charAt(ph.pos);
			if (!Character.isDigit(next) && !(next == '.') && !(next == '-')) {
				cmd = next;
				ph.advance();
			} else if (cmd == 'M') { // implied command
				cmd = 'L';
			} else if (cmd == 'm') { // implied command
				cmd = 'l';
			} else { // implied command
				// Log.d(TAG, "Implied command: " + cmd);
			}
			p.computeBounds(r, true);
			// Log.d(TAG, "  " + cmd + " " + r);
			// Util.debug("* Commands remaining: '" + path + "'.");
			boolean wasCurve = false;
			switch (cmd) {
				case 'M':
				case 'm': {
					float x = ph.nextFloat();
					float y = ph.nextFloat();
					if (cmd == 'm') {
						p.rMoveTo(x, y);
						lastX += x;
						lastY += y;
					} else {
						p.moveTo(x, y);
						lastX = x;
						lastY = y;
					}
					contourInitialX = lastX;
					contourInitialY = lastY;
					break;
				}
				case 'Z':
				case 'z': {
					/// p.lineTo(contourInitialX, contourInitialY);
					p.close();
					lastX = contourInitialX;
					lastY = contourInitialY;
					break;
				}
				case 'L':
				case 'l': {
					float x = ph.nextFloat();
					float y = ph.nextFloat();
					if (cmd == 'l') {
						p.rLineTo(x, y);
						lastX += x;
						lastY += y;
					} else {
						p.lineTo(x, y);
						lastX = x;
						lastY = y;
					}
					break;
				}
				case 'H':
				case 'h': {
					float x = ph.nextFloat();
					if (cmd == 'h') {
						p.rLineTo(x, 0);
						lastX += x;
					} else {
						p.lineTo(x, lastY);
						lastX = x;
					}
					break;
				}
				case 'V':
				case 'v': {
					float y = ph.nextFloat();
					if (cmd == 'v') {
						p.rLineTo(0, y);
						lastY += y;
					} else {
						p.lineTo(lastX, y);
						lastY = y;
					}
					break;
				}
				case 'C':
				case 'c': {
					wasCurve = true;
					float x1 = ph.nextFloat();
					float y1 = ph.nextFloat();
					float x2 = ph.nextFloat();
					float y2 = ph.nextFloat();
					float x = ph.nextFloat();
					float y = ph.nextFloat();
					if (cmd == 'c') {
						x1 += lastX;
						x2 += lastX;
						x += lastX;
						y1 += lastY;
						y2 += lastY;
						y += lastY;
					}
					p.cubicTo(x1, y1, x2, y2, x, y);
					lastX1 = x2;
					lastY1 = y2;
					lastX = x;
					lastY = y;
					break;
				}
				case 'S':
				case 's': {
					wasCurve = true;
					float x2 = ph.nextFloat();
					float y2 = ph.nextFloat();
					float x = ph.nextFloat();
					float y = ph.nextFloat();
					if (cmd == 's') {
						x2 += lastX;
						x += lastX;
						y2 += lastY;
						y += lastY;
					}
					float x1 = 2 * lastX - lastX1;
					float y1 = 2 * lastY - lastY1;
					p.cubicTo(x1, y1, x2, y2, x, y);
					lastX1 = x2;
					lastY1 = y2;
					lastX = x;
					lastY = y;
					break;
				}
				case 'A':
				case 'a': {
					float rx = ph.nextFloat();
					float ry = ph.nextFloat();
					float theta = ph.nextFloat();
					int largeArc = (int) ph.nextFloat();
					int sweepArc = (int) ph.nextFloat();
					float x = ph.nextFloat();
					float y = ph.nextFloat();
					if (cmd == 'a') {
						x += lastX;
						y += lastY;
					}
					drawArc(p, lastX, lastY, x, y, rx, ry, theta, largeArc == 1, sweepArc == 1);
					lastX = x;
					lastY = y;
					break;
				}
				case 'T':
				case 't': {
					wasCurve = true;
					float x = ph.nextFloat();
					float y = ph.nextFloat();
					if (cmd == 't') {
						x += lastX;
						y += lastY;
					}
					float x1 = 2 * lastX - lastX1;
					float y1 = 2 * lastY - lastY1;
					p.cubicTo(lastX, lastY, x1, y1, x, y);
					lastX = x;
					lastY = y;
					lastX1 = x1;
					lastY1 = y1;
					break;
				}
				case 'Q':
				case 'q': {
					wasCurve = true;
					float x1 = ph.nextFloat();
					float y1 = ph.nextFloat();
					float x = ph.nextFloat();
					float y = ph.nextFloat();
					if (cmd == 'q') {
						x += lastX;
						y += lastY;
						x1 += lastX;
						y1 += lastY;
					}
					p.cubicTo(lastX, lastY, x1, y1, x, y);
					lastX1 = x1;
					lastY1 = y1;
					lastX = x;
					lastY = y;
					break;
				}
				default:
					Log.w(TAG, "Invalid path command: " + cmd);
					ph.advance();
			}
			if (!wasCurve) {
				lastX1 = lastX;
				lastY1 = lastY;
			}
			ph.skipWhitespace();
		}
		return p;
	}

	/**
	 * Elliptical arc implementation based on the SVG specification notes
	 * Adapted from the Batik library (Apache-2 license) by SAU
	 */

	private static void drawArc(Path path, double x0, double y0, double x, double y, double rx,
								double ry, double angle, boolean largeArcFlag, boolean sweepFlag) {
		double dx2 = (x0 - x) / 2.0;
		double dy2 = (y0 - y) / 2.0;
		angle = Math.toRadians(angle % 360.0);
		double cosAngle = Math.cos(angle);
		double sinAngle = Math.sin(angle);

		double x1 = (cosAngle * dx2 + sinAngle * dy2);
		double y1 = (-sinAngle * dx2 + cosAngle * dy2);
		rx = Math.abs(rx);
		ry = Math.abs(ry);

		double Prx = rx * rx;
		double Pry = ry * ry;
		double Px1 = x1 * x1;
		double Py1 = y1 * y1;

		// check that radii are large enough
		double radiiCheck = Px1 / Prx + Py1 / Pry;
		if (radiiCheck > 1) {
			rx = Math.sqrt(radiiCheck) * rx;
			ry = Math.sqrt(radiiCheck) * ry;
			Prx = rx * rx;
			Pry = ry * ry;
		}

		// Step 2 : Compute (cx1, cy1)
		double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
		double sq = ((Prx * Pry) - (Prx * Py1) - (Pry * Px1))
				/ ((Prx * Py1) + (Pry * Px1));
		sq = (sq < 0) ? 0 : sq;
		double coef = (sign * Math.sqrt(sq));
		double cx1 = coef * ((rx * y1) / ry);
		double cy1 = coef * -((ry * x1) / rx);

		double sx2 = (x0 + x) / 2.0;
		double sy2 = (y0 + y) / 2.0;
		double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
		double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

		// Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
		double ux = (x1 - cx1) / rx;
		double uy = (y1 - cy1) / ry;
		double vx = (-x1 - cx1) / rx;
		double vy = (-y1 - cy1) / ry;
		double p, n;

		// Compute the angle start
		n = Math.sqrt((ux * ux) + (uy * uy));
		p = ux; // (1 * ux) + (0 * uy)
		sign = (uy < 0) ? -1.0 : 1.0;
		double angleStart = Math.toDegrees(sign * Math.acos(p / n));

		// Compute the angle extent
		n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
		p = ux * vx + uy * vy;
		sign = (ux * vy - uy * vx < 0) ? -1.0 : 1.0;
		double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
		if (!sweepFlag && angleExtent > 0) {
			angleExtent -= 360f;
		} else if (sweepFlag && angleExtent < 0) {
			angleExtent += 360f;
		}
		angleExtent %= 360f;
		angleStart %= 360f;

		RectF oval = new RectF((float) (cx - rx), (float) (cy - ry), (float) (cx + rx), (float) (cy + ry));
		path.addArc(oval, (float) angleStart, (float) angleExtent);
	}

	private static NumberParse getNumberParseAttr(String name, Attributes attributes) {
		int n = attributes.getLength();
		for (int i = 0; i < n; i++) {
			if (attributes.getLocalName(i).equals(name)) {
				return parseNumbers(attributes.getValue(i));
			}
		}
		return null;
	}

	private static String getStringAttr(String name, Attributes attributes) {
		int n = attributes.getLength();
		for (int i = 0; i < n; i++) {
			if (attributes.getLocalName(i).equals(name)) {
				return attributes.getValue(i);
			}
		}
		return null;
	}

	private static class NumberParse {
		private ArrayList<Float> numbers;
		private int nextCmd;

		public NumberParse(ArrayList<Float> numbers, int nextCmd) {
			this.numbers = numbers;
			this.nextCmd = nextCmd;
		}

		@SuppressWarnings("unused")
		public int getNextCmd() {
			return nextCmd;
		}

		@SuppressWarnings("unused")
		public float getNumber(int index) {
			return numbers.get(index);
		}

	}

	private static class Gradient {
		String id;
		String xlink;
		boolean isLinear;
		float x1, y1, x2, y2;
		float x, y, radius;
		ArrayList<Float> positions = new ArrayList<Float>();
		ArrayList<Integer> colors = new ArrayList<Integer>();
		Matrix matrix = null;
		Shader shader = null;

		public Gradient createChild(Gradient g) {
			Gradient child = new Gradient();
			child.id = g.id;
			child.xlink = id;
			child.isLinear = g.isLinear;
			child.x1 = g.x1;
			child.x2 = g.x2;
			child.y1 = g.y1;
			child.y2 = g.y2;
			child.x = g.x;
			child.y = g.y;
			child.radius = g.radius;
			child.positions = positions;
			child.colors = colors;
			child.matrix = matrix;
			if (g.matrix != null) {
				if (matrix == null) {
					child.matrix = g.matrix;
				} else {
					Matrix m = new Matrix(matrix);
					m.preConcat(g.matrix);
					child.matrix = m;
				}
			}
			return child;
		}

		public Shader createShader() {
			if (shader != null)
				return shader;

			int[] clr = new int[colors.size()];
			for (int i = 0; i < clr.length; i++) {
				clr[i] = colors.get(i);
			}

			float[] pos = new float[positions.size()];
			for (int i = 0; i < pos.length; i++) {
				pos[i] = positions.get(i);
			}

			if (isLinear)
				shader = new LinearGradient(x1, y1, x2, y2, clr, pos, Shader.TileMode.CLAMP);
			else
				shader = new RadialGradient(x, y, radius, clr, pos, Shader.TileMode.CLAMP);
			if (matrix != null) {
				shader.setLocalMatrix(matrix);
			}
			return shader;
		}
	}

	private static class StyleSet {
		HashMap<String, String> styleMap = new HashMap<String, String>();

		private StyleSet(String string) {
			String[] styles = string.split(";");
			for (String s : styles) {
				String[] style = s.split(":");
				if (style.length == 2) {
					styleMap.put(style[0], style[1]);
				}
			}
		}

		public String getStyle(String name) {
			return styleMap.get(name);
		}
	}

	private static class Properties {
		StyleSet styles = null;
		Attributes atts;

		private Properties(Attributes atts) {
			this.atts = atts;
			String styleAttr = getStringAttr("style", atts);
			if (styleAttr != null) {
				styles = new StyleSet(styleAttr);
			}
		}

		public String getAttr(String name) {
			String v = null;
			if (styles != null) {
				v = styles.getStyle(name);
			}
			if (v == null) {
				v = getStringAttr(name, atts);
			}
			return v;
		}

		public String getString(String name) {
			return getAttr(name);
		}

		public Integer getColorValue(String name) {
			String v = getAttr(name);
			if (v == null) {
				return null;
			} else if (v.startsWith("#") && (v.length() == 4 || v.length() == 7)) {
				try {
					int result = Integer.parseInt(v.substring(1), 16);
					return v.length() == 4 ? hex3Tohex6(result) : result;
				} catch (NumberFormatException nfe) {
					return null;
				}
			} else {
				try {
					return Color.parseColor(v);
				} catch (IllegalArgumentException e) {
					Log.w(TAG, "unknown color: " + v);
					return null;
				}
			}
		}

		// convert 0xRGB into 0xRRGGBB
		private int hex3Tohex6(int x) {
			return (x & 0xF00) << 8 | (x & 0xF00) << 12 |
					(x & 0xF0) << 4 | (x & 0xF0) << 8 |
					(x & 0xF) << 4 | (x & 0xF);
		}

		public Float getFloat(String name) {
			String v = getAttr(name);
			if (v == null) {
				return null;
			} else {
				try {
					return Float.parseFloat(v);
				} catch (NumberFormatException nfe) {
					return null;
				}
			}
		}
	}

	private static class SVGHandler extends DefaultHandler {
		//public StringBuilder parsed = new StringBuilder();

		HashMap<String, String> idXml = new HashMap<String, String>();

		Picture picture;
		Canvas canvas;

		Paint strokePaint;
		boolean strokeSet = false;
		Stack<Paint> strokePaintStack = new Stack<Paint>();
		Stack<Boolean> strokeSetStack = new Stack<Boolean>();

		Paint fillPaint;
		boolean fillSet = false;
		Stack<Paint> fillPaintStack = new Stack<Paint>();
		Stack<Boolean> fillSetStack = new Stack<Boolean>();

		float groupOpacity = 1f;
		Stack<Float> groupOpacityStack = new Stack<Float>();

		// Scratch rect (so we aren't constantly making new ones)
		RectF rect = new RectF();
		RectF bounds = null;
		RectF limits = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

		Integer defaultColor = null;

		float dpi = DPI;

		int pushed = 0;

		private boolean hidden = false;
		private int hiddenLevel = 0;
		private boolean boundsMode = false;

		HashMap<String, Gradient> gradientMap = new HashMap<String, Gradient>();
		Gradient gradient = null;
		SvgText text = null;

		private boolean inDefsElement = false;

		private SVGHandler(Picture picture) {
			this.picture = picture;
			strokePaint = new Paint();
			strokePaint.setAntiAlias(true);
			strokePaint.setStyle(Paint.Style.STROKE);
			fillPaint = new Paint();
			fillPaint.setAntiAlias(true);
			fillPaint.setStyle(Paint.Style.FILL);
		}

		public void setDpi(float dpi) {
			this.dpi = dpi;
		}

		public void setDefaultColor(Integer defaultColor) {
			this.defaultColor = defaultColor;
		}

		@Override
		public void startDocument() {
			// Set up prior to parsing a doc
		}

		@Override
		public void endDocument() {
			// Clean up after parsing a doc
                        /*
                        String s = parsed.toString();
                        if (s.endsWith("</svg>")) {
                                s = s + "";
                                Log.d(TAG, s);
                        }
                        */
		}

		private boolean doFill(Properties atts, HashMap<String, Gradient> gradients) {
			if ("none".equals(atts.getString("display"))) {
				return false;
			}
			String fillString = atts.getString("fill");
			if (fillString != null) {
				if (fillString.startsWith("url(#")) {
					// It's a gradient fill, look it up in our map
					String id = fillString.substring("url(#".length(), fillString.length() - 1);
					Gradient gradient = gradients.get(id);
					if (gradient != null) {
						fillPaint.setShader(gradient.createShader());
						return true;
					} else {
						Log.w(TAG, "Didn't find shader, using black: " + id);
						fillPaint.setShader(null);
						doColor(atts, Color.BLACK, true, fillPaint);
						return true;
					}
				} else if (fillString.equalsIgnoreCase("none")) {
					fillPaint.setShader(null);
					fillPaint.setColor(Color.TRANSPARENT);
					return true;
				} else {
					fillPaint.setShader(null);
					Integer color = atts.getColorValue("fill");
					if (color != null) {
						doColor(atts, color, true, fillPaint);
						return true;
					} else {
						Log.w(TAG, "Unrecognized fill color, using black: " + fillString);
						doColor(atts, Color.BLACK, true, fillPaint);
						return true;
					}
				}
			} else {
				if (fillSet) {
					// If fill is set, inherit from parent
					return fillPaint.getColor() != Color.TRANSPARENT;   // optimization
				} else {
					// Default is black fill
					fillPaint.setShader(null);
					fillPaint.setColor(defaultColor != null ? defaultColor : Color.BLACK);
					return true;
				}
			}
		}

		// XXX not done yet
		private boolean doText(Attributes atts, Paint paint) {
			if ("none".equals(atts.getValue("display"))) {
				return false;
			}
			if (atts.getValue("font-size") != null) {
				paint.setTextSize(getFloatAttr("font-size", atts, 10f));
			}
			Typeface typeface = setTypeFace(atts);
			if (typeface != null) {
				paint.setTypeface(typeface);
			}
			Align align = getTextAlign(atts);
			if (align != null) {
				paint.setTextAlign(getTextAlign(atts));
			}
			return true;
		}

		private boolean doStroke(Properties atts) {
			if ("none".equals(atts.getString("display"))) {
				return false;
			}

			// Check for other stroke attributes
			Float width = atts.getFloat("stroke-width");
			if (width != null) {
				strokePaint.setStrokeWidth(width);
			}

			// don't stroke zero width lines
			if (strokePaint.getStrokeWidth() <= 0.0f) {
				return false;
			}

			String linecap = atts.getString("stroke-linecap");
			if ("round".equals(linecap)) {
				strokePaint.setStrokeCap(Paint.Cap.ROUND);
			} else if ("square".equals(linecap)) {
				strokePaint.setStrokeCap(Paint.Cap.SQUARE);
			} else if ("butt".equals(linecap)) {
				strokePaint.setStrokeCap(Paint.Cap.BUTT);
			}

			String linejoin = atts.getString("stroke-linejoin");
			if ("miter".equals(linejoin)) {
				strokePaint.setStrokeJoin(Paint.Join.MITER);
			} else if ("round".equals(linejoin)) {
				strokePaint.setStrokeJoin(Paint.Join.ROUND);
			} else if ("bevel".equals(linejoin)) {
				strokePaint.setStrokeJoin(Paint.Join.BEVEL);
			}

			pathStyleHelper(atts.getString("stroke-dasharray"), atts.getString("stroke-dashoffset"));

			String strokeString = atts.getAttr("stroke");
			if (strokeString != null) {
				if (strokeString.equalsIgnoreCase("none")) {
					strokePaint.setColor(Color.TRANSPARENT);
					return false;
				} else {
					Integer color = atts.getColorValue("stroke");
					if (color != null) {
						doColor(atts, color, false, strokePaint);
						return true;
					} else {
						Log.d(TAG, "Unrecognized stroke color, using none: " + strokeString);
						strokePaint.setColor(Color.TRANSPARENT);
						return false;
					}
				}
			} else {
				if (strokeSet) {
					// Inherit from parent
					return strokePaint.getColor() != Color.TRANSPARENT;   // optimization
				} else if (width != null && defaultColor != null) {
					strokePaint.setColor(defaultColor);
					return true;
				} else {
					// Default is none
					strokePaint.setColor(Color.TRANSPARENT);
					return false;
				}
			}
		}

		private Gradient doGradient(boolean isLinear, Attributes atts) {
			Gradient gradient = new Gradient();
			gradient.id = getStringAttr("id", atts);
			gradient.isLinear = isLinear;
			if (isLinear) {
				gradient.x1 = getFloatAttr("x1", atts, 0f);
				gradient.x2 = getFloatAttr("x2", atts, 0f);
				gradient.y1 = getFloatAttr("y1", atts, 0f);
				gradient.y2 = getFloatAttr("y2", atts, 0f);
			} else {
				gradient.x = getFloatAttr("cx", atts, 0f);
				gradient.y = getFloatAttr("cy", atts, 0f);
				gradient.radius = getFloatAttr("r", atts, 0f);
			}
			String transform = getStringAttr("gradientTransform", atts);
			if (transform != null) {
				gradient.matrix = parseTransform(transform);
			}
			String xlink = getStringAttr("href", atts);
			if (xlink != null) {
				if (xlink.startsWith("#")) {
					xlink = xlink.substring(1);
				}
				gradient.xlink = xlink;
			}
			return gradient;
		}

		private void doColor(Properties atts, Integer color, boolean fillMode, Paint paint) {
			Float opacity = atts.getFloat("opacity");
			if (opacity == null)
				opacity = atts.getFloat(fillMode ? "fill-opacity" : "stroke-opacity");
			if (opacity == null)
				opacity = 1f;
			int alphaInt = Math.round(255 * opacity * groupOpacity);
			paint.setColor(color | (alphaInt << 24));
		}

		/**
		 * set the path style (if any)
		 * stroke-dasharray="n1,n2,..."
		 * stroke-dashoffset=n
		 */

		private void pathStyleHelper(String style, String offset) {
			if (style == null) {
				return;
			}

			if (style.equals("none")) {
				strokePaint.setPathEffect(null);
				return;
			}

			StringTokenizer st = new StringTokenizer(style, " ,");
			int count = st.countTokens();
			float[] intervals = new float[(count & 1) == 1 ? count * 2 : count];
			float max = 0;
			float current = 1f;
			int i = 0;
			while (st.hasMoreTokens()) {
				intervals[i++] = current = toFloat(st.nextToken(), current);
				max += current;
			}

			// in svg speak, we double the intervals on an odd count
			for (int start = 0; i < intervals.length; i++, start++) {
				max += intervals[i] = intervals[start];
			}

			float off = 0f;
			if (offset != null) {
				try {
					off = Float.parseFloat(offset) % max;
				} catch (NumberFormatException e) {
					// ignore
				}
			}

			strokePaint.setPathEffect(new DashPathEffect(intervals, off));
		}

		private static float toFloat(String s, float dflt) {
			float result = dflt;
			try {
				result = Float.parseFloat(s);
			} catch (NumberFormatException e) {
				// ignore
			}
			return result;
		}

		private void doLimits(float x, float y) {
			if (x < limits.left) {
				limits.left = x;
			}
			if (x > limits.right) {
				limits.right = x;
			}
			if (y < limits.top) {
				limits.top = y;
			}
			if (y > limits.bottom) {
				limits.bottom = y;
			}
		}

		private void doLimits(float x, float y, float width, float height) {
			doLimits(x, y);
			doLimits(x + width, y + height);
		}

		private void doLimits(Path path) {
			path.computeBounds(rect, false);
			doLimits(rect.left, rect.top);
			doLimits(rect.right, rect.bottom);
		}

		private final static Matrix IDENTITY_MATRIX = new Matrix();

		// XXX could be more selective using save(flags)
		private void pushTransform(Attributes atts) {
			final String transform = getStringAttr("transform", atts);
			final Matrix matrix = transform == null ? IDENTITY_MATRIX : parseTransform(transform);
			pushed++;
			canvas.save(); //Canvas.MATRIX_SAVE_FLAG);

                        /*final Matrix m = canvas.getMatrix();
                        m.postConcat(matrix);
                        canvas.setMatrix(m);*/

			canvas.concat(matrix);
			//Log.d(TAG, "matrix push: " + canvas.getMatrix());
		}

		private void popTransform() {
			canvas.restore();
			//Log.d(TAG, "matrix pop: " + canvas.getMatrix());
			pushed--;
		}

		private void doBitmap(Canvas canvas, float x, float y, float width, float height, byte[] bytes) {
			Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			if (bm != null) {
				// Log.d(TAG, String.format("Image %f x %f %s", width, height, bm));
				bm.prepareToDraw();
				Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
				RectF rect = new RectF(x, y, x + width, y + height);
				canvas.clipRect(rect, Op.REPLACE);
				canvas.drawBitmap(bm, null, rect, paint);
				bm.recycle();
			}
		}

		@Override
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
			//appendElementString(parsed, namespaceURI, localName, qName, atts);

			// Log.d(TAG, localName + showAttributes(atts));
			// Reset paint opacity
			if (!strokeSet) {
				strokePaint.setAlpha(255);
			}
			if (!fillSet) {
				fillPaint.setAlpha(255);
			}
			// Ignore everything but rectangles in bounds mode
			if (boundsMode) {
				if (localName.equals("rect")) {
					float x = getFloatAttr("x", atts, 0f);
					float y = getFloatAttr("y", atts, 0f);
					Float width = getFloatAttr("width", atts);
					Float height = getFloatAttr("height", atts);
					bounds = new RectF(x, y, x + width, y + height);
				}
				return;
			}

			if (inDefsElement) {
				return;
			}

			if (localName.equals("svg")) {
				int width = (int) Math.ceil(getFloatAttr("width", atts));
				int height = (int) Math.ceil(getFloatAttr("height", atts));
				if (width == 0 || height == 0) {
					final RectF rect = parseViewBox(getStringAttr("viewBox", atts));
					if (rect != null && !rect.isEmpty()) {
						width = Math.round(rect.width());
						height = Math.round(rect.height());
					}
				}
				canvas = picture.beginRecording(width, height);
			} else if (localName.equals("defs")) {
				inDefsElement = true;
			} else if (localName.equals("linearGradient")) {
				gradient = doGradient(true, atts);
			} else if (localName.equals("radialGradient")) {
				gradient = doGradient(false, atts);
			} else if (localName.equals("stop")) {
				if (gradient != null) {
					float offset = getFloatAttr("offset", atts);
					String styles = getStringAttr("style", atts);
					StyleSet styleSet = new StyleSet(styles);
					String colorStyle = styleSet.getStyle("stop-color");
					int color = Color.BLACK;
					if (colorStyle != null) {
						if (colorStyle.startsWith("#")) {
							color = Integer.parseInt(colorStyle.substring(1), 16);
						} else {
							color = Integer.parseInt(colorStyle, 16);
						}
					}
					String opacityStyle = styleSet.getStyle("stop-opacity");
					if (opacityStyle != null) {
						float alpha = Float.parseFloat(opacityStyle);
						int alphaInt = Math.round(255 * alpha);
						color |= (alphaInt << 24);
					} else {
						color |= 0xFF000000;
					}
					gradient.positions.add(offset);
					gradient.colors.add(color);
				}
			} else if (localName.equals("use")) {
				String href = atts.getValue("xlink:href");
				String attTransform = atts.getValue("transform");
				String attX = atts.getValue("x");
				String attY = atts.getValue("y");

				StringBuilder sb = new StringBuilder(4096);
				sb.append("<g");
				sb.append(" xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' version='1.1'");
				if (attTransform != null || attX != null || attY != null) {
					sb.append(" transform='");
					if (attTransform != null) {
						sb.append(escape(attTransform));
					}
					if (attX != null || attY != null) {
						sb.append("translate(");
						sb.append(attX != null ? escape(attX) : "0");
						sb.append(",");
						sb.append(attY != null ? escape(attY) : "0");
						sb.append(")");
					}
					sb.append("'");
				}

				for (int i = 0; i < atts.getLength(); i++) {
					String attrQName = atts.getQName(i);
					if (!"x".equals(attrQName) && !"y".equals(attrQName) &&
							!"width".equals(attrQName) && !"height".equals(attrQName) &&
							!"xlink:href".equals(attrQName) && !"transform".equals(attrQName)) {

						sb.append(" ");
						sb.append(attrQName);
						sb.append("='");
						sb.append(escape(atts.getValue(i)));
						sb.append("'");
					}
				}

				sb.append(">");

				sb.append(idXml.get(href.substring(1)));

				sb.append("</g>");

				// Log.d(TAG, sb.toString());

				InputSource is = new InputSource(new StringReader(sb.toString()));
				try {
					SAXParserFactory spf = SAXParserFactory.newInstance();
					SAXParser sp = spf.newSAXParser();
					XMLReader xr = sp.getXMLReader();
					xr.setContentHandler(this);
					xr.parse(is);
				} catch (Exception e) {
					Log.d(TAG, sb.toString());
					e.printStackTrace();
				}
			} else if (localName.equals("g")) {
				// Check to see if this is the "bounds" layer
				if ("bounds".equalsIgnoreCase(getStringAttr("id", atts))) {
					boundsMode = true;
				}
				if (hidden) {
					hiddenLevel++;
					//Util.debug("Hidden up: " + hiddenLevel);
				}
				// Go in to hidden mode if display is "none"
				if ("none".equals(getStringAttr("display", atts))) {
					if (!hidden) {
						hidden = true;
						hiddenLevel = 1;
						//Util.debug("Hidden up: " + hiddenLevel);
					}
				}
				pushTransform(atts); // sau
				Properties props = new Properties(atts);

				fillPaintStack.push(new Paint(fillPaint));
				strokePaintStack.push(new Paint(strokePaint));
				fillSetStack.push(fillSet);
				strokeSetStack.push(strokeSet);
				groupOpacityStack.push(groupOpacity);

				Float opacity = getFloatAttr("opacity", atts);
				if (opacity != null) {
					groupOpacity = groupOpacity * opacity;
				}

				doText(atts, fillPaint);
				doText(atts, strokePaint);
				doFill(props, gradientMap);
				doStroke(props);

				fillSet |= (props.getString("fill") != null);
				strokeSet |= (props.getString("stroke") != null);
			} else if (!hidden && localName.equals("rect")) {
				float x = getFloatAttr("x", atts, 0f);
				float y = getFloatAttr("y", atts, 0f);
				Float width = getFloatAttr("width", atts);
				Float height = getFloatAttr("height", atts);
				float rx = getFloatAttr("rx", atts, 0f);
				float ry = getFloatAttr("ry", atts, 0f);
				pushTransform(atts);
				Properties props = new Properties(atts);
				if (doFill(props, gradientMap)) {
					doLimits(x, y, width, height);
					if (rx <= 0f && ry <= 0f) {
						canvas.drawRect(x, y, x + width, y + height, fillPaint);
					} else {
						rect.set(x, y, x + width, y + height);
						canvas.drawRoundRect(rect, rx, ry, fillPaint);
					}
				}
				if (doStroke(props)) {
					if (rx <= 0f && ry <= 0f) {
						canvas.drawRect(x, y, x + width, y + height, strokePaint);
					} else {
						rect.set(x, y, x + width, y + height);
						canvas.drawRoundRect(rect, rx, ry, strokePaint);
					}
				}
				popTransform();
			} else if (!hidden && localName.equals("image")) { // only handle inline images
				// <image width="100" height="100" xlink:href="data:image/png;base64,...">
				String url = getStringAttr("href", atts);
				if (url.startsWith("data") && url.indexOf("base64") > 0
						&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
					String base64Data = url.substring(url.indexOf(",") + 1);
					float x = getFloatAttr("x", atts, 0f);
					float y = getFloatAttr("y", atts, 0f);
					float width = getFloatAttr("width", atts, 0f);
					float height = getFloatAttr("height", atts, 0f);
					pushTransform(atts);
					doLimits(x, y, width, height);
					doBitmap(canvas, x, y, width, height, Base64.decode(base64Data, Base64.DEFAULT));
					popTransform();
				}
			} else if (!hidden && localName.equals("line")) {
				Float x1 = getFloatAttr("x1", atts);
				Float x2 = getFloatAttr("x2", atts);
				Float y1 = getFloatAttr("y1", atts);
				Float y2 = getFloatAttr("y2", atts);
				Properties props = new Properties(atts);
				if (doStroke(props)) {
					pushTransform(atts);
					doLimits(x1, y1);
					doLimits(x2, y2);
					canvas.drawLine(x1, y1, x2, y2, strokePaint);
					popTransform();
				}
			} else if (!hidden && localName.equals("circle")) {
				Float centerX = getFloatAttr("cx", atts);
				Float centerY = getFloatAttr("cy", atts);
				Float radius = getFloatAttr("r", atts);
				if (centerX != null && centerY != null && radius != null) {
					pushTransform(atts);
					Properties props = new Properties(atts);
					if (doFill(props, gradientMap)) {
						doLimits(centerX - radius, centerY - radius);
						doLimits(centerX + radius, centerY + radius);
						canvas.drawCircle(centerX, centerY, radius, fillPaint);
					}
					if (doStroke(props)) {
						canvas.drawCircle(centerX, centerY, radius, strokePaint);
					}
					popTransform();
				}
			} else if (!hidden && localName.equals("ellipse")) {
				Float centerX = getFloatAttr("cx", atts);
				Float centerY = getFloatAttr("cy", atts);
				Float radiusX = getFloatAttr("rx", atts);
				Float radiusY = getFloatAttr("ry", atts);
				if (centerX != null && centerY != null && radiusX != null && radiusY != null) {
					pushTransform(atts);
					Properties props = new Properties(atts);
					rect.set(centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY);
					if (doFill(props, gradientMap)) {
						doLimits(centerX - radiusX, centerY - radiusY);
						doLimits(centerX + radiusX, centerY + radiusY);
						canvas.drawOval(rect, fillPaint);
					}
					if (doStroke(props)) {
						canvas.drawOval(rect, strokePaint);
					}
					popTransform();
				}
			} else if (!hidden && (localName.equals("polygon") || localName.equals("polyline"))) {
				NumberParse numbers = getNumberParseAttr("points", atts);
				if (numbers != null) {
					Path p = new Path();
					ArrayList<Float> points = numbers.numbers;
					if (points.size() > 1) {
						pushTransform(atts);
						Properties props = new Properties(atts);
						p.moveTo(points.get(0), points.get(1));
						for (int i = 2; i < points.size(); i += 2) {
							float x = points.get(i);
							float y = points.get(i + 1);
							p.lineTo(x, y);
						}
						// Don't close a polyline
						if (localName.equals("polygon")) {
							p.close();
						}
						if (doFill(props, gradientMap)) {
							doLimits(p);

							// showBounds("fill", p);
							canvas.drawPath(p, fillPaint);
						}
						if (doStroke(props)) {
							// showBounds("stroke", p);
							canvas.drawPath(p, strokePaint);
						}
						popTransform();
					}
				}
			} else if (!hidden && localName.equals("path")) {
				Path p = doPath(getStringAttr("d", atts));
				pushTransform(atts);
				Properties props = new Properties(atts);
				if (doFill(props, gradientMap)) {
					// showBounds("gradient", p);
					doLimits(p);
					// showBounds("gradient", p);
					canvas.drawPath(p, fillPaint);
				}
				if (doStroke(props)) {
					// showBounds("paint", p);
					canvas.drawPath(p, strokePaint);
				}
				popTransform();
			} else if (!hidden && localName.equals("text")) {
				pushTransform(atts);
				text = new SvgText(atts);
			} else if (!hidden) {
				Log.d(TAG, String.format("Unrecognized tag: %s (%s)", localName, showAttributes(atts)));
			}
		}

		@SuppressWarnings("unused")
		private void showBounds(String text, Path p) {
			RectF b = new RectF();
			p.computeBounds(b, true);
			Log.d(TAG, text + " bounds: " + b.left + "," + b.bottom + " to " + b.right + "," + b.top);
		}

		@SuppressWarnings("unused")
		private String showAttributes(Attributes a) {
			String result = "";
			for (int i = 0; i < a.getLength(); i++) {
				result += " " + a.getLocalName(i) + "='" + a.getValue(i) + "'";
			}
			return result;
		}

		@Override
		public void characters(char ch[], int start, int length) {
			// Log.i(TAG, new String(ch) + " " + start + "/" + length);
			if (text != null) {
				text.setText(ch, start, length);
			}
		}

		@Override
		public void endElement(String namespaceURI, String localName, String qName) {
			// Log.d("TAG", "tag: " + localName);
                        /*parsed.append("</");
                        parsed.append(localName);
                        parsed.append(">");*/

			if (inDefsElement) {
				if (localName.equals("defs")) {
					inDefsElement = false;
				}
				return;
			}

			if (localName.equals("svg")) {
				picture.endRecording();
			} else if (!hidden && localName.equals("text")) {
				if (text != null) {
					text.render(canvas);
					text.close();
				}
				popTransform();
			} else if (localName.equals("linearGradient")
					|| localName.equals("radialGradient")) {
				if (gradient.id != null) {
					if (gradient.xlink != null) {
						Gradient parent = gradientMap.get(gradient.xlink);
						if (parent != null) {
							gradient = parent.createChild(gradient);
						}
					}
					gradientMap.put(gradient.id, gradient);
				}
			} else if (localName.equals("g")) {
				if (boundsMode) {
					boundsMode = false;
				}
				// Break out of hidden mode
				if (hidden) {
					hiddenLevel--;
					//Util.debug("Hidden down: " + hiddenLevel);
					if (hiddenLevel == 0) {
						hidden = false;
					}
				}
				// Clear gradient map: gradientMap.clear();
				popTransform(); // SAU
				fillPaint = fillPaintStack.pop();
				fillSet = fillSetStack.pop();
				strokePaint = strokePaintStack.pop();
				strokeSet = strokeSetStack.pop();
				groupOpacity = groupOpacityStack.pop();
			}
		}

		// class to hold text properties

		private class SvgText {
			private final static int MIDDLE = 1;
			private final static int TOP = 2;
			private Paint stroke = null, fill = null;
			private float x, y;
			private String svgText;
			private boolean inText;
			private int vAlign = 0;

			public SvgText(Attributes atts) {
				// Log.d(TAG, "text");
				x = getFloatAttr("x", atts, 0f);
				y = getFloatAttr("y", atts, 0f);
				svgText = null;
				inText = true;

				Properties props = new Properties(atts);
				if (doFill(props, gradientMap)) {
					fill = new Paint(fillPaint);
					doText(atts, fill);
				}
				if (doStroke(props)) {
					stroke = new Paint(strokePaint);
					doText(atts, stroke);
				}
				// quick hack
				String valign = getStringAttr("alignment-baseline", atts);
				if ("middle".equals(valign)) {
					vAlign = MIDDLE;
				} else if ("top".equals(valign)) {
					vAlign = TOP;
				}
			}

			// ignore tspan elements for now
			public void setText(char[] ch, int start, int len) {
				if (isInText()) {
					if (svgText == null) {
						svgText = new String(ch, start, len);
					} else {
						svgText += new String(ch, start, len);
					}

					// This is an experiment for vertical alignment
					if (vAlign > 0) {
						Paint paint = stroke == null ? fill : stroke;
						Rect bnds = new Rect();
						paint.getTextBounds(svgText, 0, svgText.length(), bnds);
						// Log.i(TAG, "Adjusting " + y + " by " + bnds);
						y += (vAlign == MIDDLE) ? -bnds.centerY() : bnds.height();
					}
				}
			}

			public boolean isInText() {
				return inText;
			}

			public void close() {
				inText = false;
			}

			public void render(Canvas canvas) {
				if (fill != null) {
					canvas.drawText(svgText, x, y, fill);
				}
				if (stroke != null) {
					canvas.drawText(svgText, x, y, stroke);
				}
				// Log.i(TAG, "Drawing: " + svgText + " " + x + "," + y);
			}
		}

		private Align getTextAlign(Attributes atts) {
			String align = getStringAttr("text-anchor", atts);
			if (align == null) {
				return null;
			}
			if ("middle".equals(align)) {
				return Align.CENTER;
			} else if ("end".equals(align)) {
				return Align.RIGHT;
			} else {
				return Align.LEFT;
			}
		}

		private Typeface setTypeFace(Attributes atts) {
			String face = getStringAttr("font-family", atts);
			String style = getStringAttr("font-style", atts);
			String weight = getStringAttr("font-weight", atts);

			if (face == null && style == null && weight == null) {
				return null;
			}
			int styleParam = Typeface.NORMAL;
			if ("italic".equals(style)) {
				styleParam |= Typeface.ITALIC;
			}
			if ("bold".equals(weight)) {
				styleParam |= Typeface.BOLD;
			}
			Typeface result = Typeface.create(face, styleParam);
			// Log.d(TAG, "typeface=" + result + " " + styleParam);
			return result;
		}

		private Float getFloatAttr(String name, Attributes attributes) {
			return getFloatAttr(name, attributes, null);
		}

		private Float getFloatAttr(String name, Attributes attributes, Float defaultValue) {
			Float result = convertUnits(name, attributes, dpi);
			return result == null ? defaultValue : result;
		}

		/**
		 * Some SVG unit conversions.  This is approximate
		 *
		 * @param atts
		 * @param dpi
		 */
		private Float convertUnits(String name, Attributes atts, float dpi) {
			String value = getStringAttr(name, atts);
			if (value == null) {
				return null;
			} else if (value.endsWith("px")) {
				return Float.parseFloat(value.substring(0, value.length() - 2));
			} else if (value.endsWith("pt")) {
				return Float.valueOf(value.substring(0, value.length() - 2)) * dpi / 72;
			} else if (value.endsWith("pc")) {
				return Float.valueOf(value.substring(0, value.length() - 2)) * dpi / 6;
			} else if (value.endsWith("cm")) {
				return Float.valueOf(value.substring(0, value.length() - 2)) * dpi / 2.54f;
			} else if (value.endsWith("mm")) {
				return Float.valueOf(value.substring(0, value.length() - 2)) * dpi / 254;
			} else if (value.endsWith("in")) {
				return Float.valueOf(value.substring(0, value.length() - 2)) * dpi;
			} else if (value.endsWith("em")) {
				float size = fillPaint.getTextSize();
				return Float.valueOf(value.substring(0, value.length() - 2)) * size;
			} else if (value.endsWith("ex")) {
				float size = fillPaint.getTextSize();
				return Float.valueOf(value.substring(0, value.length() - 2)) * size / 2f; // close?
			} else if (value.endsWith("%")) {
				Float result = Float.valueOf(value.substring(0, value.length() - 1));
				float mult;
				if (name.indexOf("x") >= 0 || name.equals("width")) {
					mult = canvas.getWidth() / 100f;
				} else if (name.indexOf("y") >= 0 || name.equals("height")) {
					mult = canvas.getHeight() / 100f;
				} else {
					mult = (canvas.getHeight() + canvas.getWidth()) / 2f;
				}
				return result * mult;
			} else {
				return Float.valueOf(value);
			}
		}
	}
}