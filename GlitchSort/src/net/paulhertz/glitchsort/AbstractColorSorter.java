/**
 * 
 */
package net.paulhertz.glitchsort;

import processing.core.*;
import net.paulhertz.glitchsort.constants.*;

/**
 * @author paulhz
 *
 */
public abstract class AbstractColorSorter implements Sorter {
	boolean isRandomBreak = false;
	boolean isSwapChannels = false;
	boolean isAscendingSort = false;
	public float breakPoint = 500.0f;
	SorterType sorterType;
	public CompOrder compOrder = CompOrder.RGB;
	public SwapChannel swap;
	public long count = 0;
	int testV = 0;
	int testW = 0;
	int[] compV;
	int[] compW;
	private PApplet app;
	
	public AbstractColorSorter(PApplet app) {
		this.app = app;
	}
	
	// permits many different evaluations of the color values of two pixels.
	// could be optimized, but then it would be harder to understand
	public boolean less(int v, int w) { 
	    compV = rgbComponents(v);
	    compW = rgbComponents(w);
	    testV = v;
	    testW = w;
		switch(compOrder) {
		case RGB: {
			break;
		}
		case BRG: {
			testV = composeColor(compV[2], compV[0], compV[1], 255);
			testW = composeColor(compW[2], compW[0], compW[1], 255);
			break;
		}
		case GBR: {
			testV = composeColor(compV[1], compV[2], compV[0], 255);
			testW = composeColor(compW[1], compW[2], compW[0], 255);
			break;
		}
		case GRB: {
			testV = composeColor(compV[1], compV[0], compV[2], 255);
			testW = composeColor(compW[1], compW[0], compW[2], 255);
			break;
		}
		case BGR: {
			testV = composeColor(compV[2], compV[1], compV[0], 255);
			testW = composeColor(compW[2], compW[1], compW[0], 255);
			break;
		}
		case RBG: {
			testV = composeColor(compV[0], compV[2], compV[1], 255);
			testW = composeColor(compW[0], compW[2], compW[1], 255);
			break;
		}
		case HSB: {
			app.colorMode(PApplet.HSB, 255);
			int hueV = Math.round(app.hue(v));
			int brightV = Math.round(app.brightness(v));
			int satV = Math.round(app.saturation(v));
			int hueW = Math.round(app.hue(w));
			int brightW = Math.round(app.brightness(w));
			int satW = Math.round(app.saturation(w));
			testV = composeColor(hueV, satV, brightV, 255);
			testW = composeColor(hueW, satW, brightW, 255);
			app.colorMode(PApplet.RGB, 255);
			break;
		}
		case HBS: {
			app.colorMode(PApplet.HSB, 255);
			int hueV = Math.round(app.hue(v));
			int brightV = Math.round(app.brightness(v));
			int satV = Math.round(app.saturation(v));
			int hueW = Math.round(app.hue(w));
			int brightW = Math.round(app.brightness(w));
			int satW = Math.round(app.saturation(w));
			testV = composeColor(hueV, brightV, satV, 255);
			testW = composeColor(hueW, brightW, satW, 255);
			app.colorMode(PApplet.RGB, 255);
			break;
		}
		case BHS: {
			app.colorMode(PApplet.HSB, 255);
			int hueV = Math.round(app.hue(v));
			int brightV = Math.round(app.brightness(v));
			int satV = Math.round(app.saturation(v));
			int hueW = Math.round(app.hue(w));
			int brightW = Math.round(app.brightness(w));
			int satW = Math.round(app.saturation(w));
			testV = composeColor(brightV, hueV, satV, 255);
			testW = composeColor(brightW, hueW, satW, 255);
			app.colorMode(PApplet.RGB, 255);
			break;
		}
		case SHB: {
			app.colorMode(PApplet.HSB, 255);
			int hueV = Math.round(app.hue(v));
			int brightV = Math.round(app.brightness(v));
			int satV = Math.round(app.saturation(v));
			int hueW = Math.round(app.hue(w));
			int brightW = Math.round(app.brightness(w));
			int satW = Math.round(app.saturation(w));
			testV = composeColor(satV, hueV, brightV, 255);
			testW = composeColor(satW, hueW, brightW, 255);
			app.colorMode(PApplet.RGB, 255);
			break;
		}
		case BSH: {
			app.colorMode(PApplet.HSB, 255);
			int hueV = Math.round(app.hue(v));
			int brightV = Math.round(app.brightness(v));
			int satV = Math.round(app.saturation(v));
			int hueW = Math.round(app.hue(w));
			int brightW = Math.round(app.brightness(w));
			int satW = Math.round(app.saturation(w));
			testV = composeColor(brightV, satV, hueV, 255);
			testW = composeColor(brightW, satW, hueW, 255);
			app.colorMode(PApplet.RGB, 255);
			break;
		}
		case SBH: {
			app.colorMode(PApplet.HSB, 255);
			int hueV = Math.round(app.hue(v));
			int brightV = Math.round(app.brightness(v));
			int satV = Math.round(app.saturation(v));
			int hueW = Math.round(app.hue(w));
			int brightW = Math.round(app.brightness(w));
			int satW = Math.round(app.saturation(w));
			testV = composeColor(satV, brightV, hueV, 255);
			testW = composeColor(satW, brightW, hueW, 255);
			app.colorMode(PApplet.RGB, 255);
			break;
		}
		}
		count++;			
		if (isAscendingSort) return testV > testW;
		return testV < testW;
		// return v < w; 
	} 
	
	/**
	 * Breaks a Processing color into R, G and B values in an array.
	 * @param argb   a Processing color as a 32-bit integer 
	 * @return       an array of integers in the intRange 0..255 for 3 primary color components: {R, G, B}
	 */
	public static int[] rgbComponents(int argb) {
		int[] comp = new int[3];
		comp[0] = (argb >> 16) & 0xFF;  // Faster way of getting red(argb)
		comp[1] = (argb >> 8) & 0xFF;   // Faster way of getting green(argb)
		comp[2] = argb & 0xFF;          // Faster way of getting blue(argb)
		return comp;
	}

	/**
	 * Creates a Processing ARGB color from r, g, b, and alpha channel values. Note the order
	 * of arguments, the same as the Processing color(value1, value2, value3, alpha) method. 
	 * @param r   red component 0..255
	 * @param g   green component 0..255
	 * @param b   blue component 0..255
	 * @param a   alpha component 0..255
	 * @return    a 32-bit integer with bytes in Processing format ARGB.
	 */
	public static int composeColor(int r, int g, int b, int a) {
		return a << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Creates a Processing ARGB color from r, g, b, values in an array. 
	 * @param comp   array of 3 integers in range 0..255, for red, green and blue components of color
	 *               alpha value is assumed to be 255
	 * @return       a 32-bit integer with bytes in Processing format ARGB.
	 */
	public static int composeColor(int[] comp) {
		return 255 << 24 | comp[0] << 16 | comp[1] << 8 | comp[2];
	}

	
	public void exch(int[] a, int i, int j) { 
		if (isSwapChannels) {
			switch (swap) {
			case RR: {
				a[i] = composeColor(compW[0], compV[1], compV[2], 255);
				a[j] = composeColor(compV[0], compW[1], compW[2], 255);
				break;
			}
			case RG: {
				a[i] = composeColor(compW[1], compV[1], compV[2], 255);
				a[j] = composeColor(compW[0], compV[0], compW[2], 255);
				break;
			}
			case RB: {
				a[i] = composeColor(compW[2], compV[1], compV[2], 255);
				a[j] = composeColor(compW[0], compW[1], compV[0], 255);
				break;
			}
			case GR: {
				a[i] = composeColor(compV[0], compW[0], compV[2], 255);
				a[j] = composeColor(compV[1], compW[1], compW[2], 255);
				break;
			}
			case GG: {
				a[i] = composeColor(compV[0], compW[1], compV[2], 255);
				a[j] = composeColor(compW[0], compV[1], compW[2], 255);
				break;
			}
			case GB: {
				a[i] = composeColor(compV[0], compW[2], compV[2], 255);
				a[j] = composeColor(compW[0], compW[1], compV[1], 255);
				break;
			}
			case BR: {
				a[i] = composeColor(compV[0], compV[1], compW[0], 255);
				a[j] = composeColor(compV[2], compW[1], compW[2], 255);
				break;
			}
			case BG: {
				a[i] = composeColor(compV[0], compV[1], compV[2], 255);
				a[j] = composeColor(compW[0], compW[1], compW[2], 255);
				break;
			}
			case BB: {
				a[i] = composeColor(compV[0], compV[1], compW[1], 255);
				a[j] = composeColor(compW[0], compW[2], compV[2], 255);
				break;
			}
			}
		}
		else {
//			the following two lines should also be equivalent to a swap
/*				a[i] = composeColor(compV[0], compV[1], compV[2], 255);
			a[j] = composeColor(compW[0], compW[1], compW[2], 255)
*/				 // swap
			int t = a[i]; 
			a[i] = a[j]; 
			a[j] = t; 
		}
	} 

	public void compExch(int[] a, int i, int j) { 
		if (less(a[j], a[i])) exch (a, i, j); 
	} 
	
	// this method is different for each algorithm
	public abstract void sort(int[] a, int l, int r);

	// this convenience method permits sorting of any arbitrary array of ints
	public void sort(int[] a) {
		sort(a, 0, a.length - 1);
	}
	
	/***** getters and setters and other methods specific to instance variables *****/

	/**
	 * @return the isRandomBreak
	 */
	public boolean isRandomBreak() {
		return isRandomBreak;
	}

	/**
	 * @param isRandomBreak the isRandomBreak to set
	 */
	public void setRandomBreak(boolean isRandomBreak) {
		this.isRandomBreak = isRandomBreak;
	}

	public float getBreakPoint() {
		return breakPoint;
	}

	public void setBreakPoint(float breakPoint) {
		this.breakPoint = breakPoint;
	}
	
	public boolean breakTest() {
		return (breakPoint < app.random(0, 1000));
	}

	/**
	 * @return the isSwapChannels
	 */
	public boolean isSwapChanels() {
		return isSwapChannels;
	}

	/**
	 * @param isSwapChannels the isSwapChannels to set
	 */
	public void setSwapChannels(boolean isSwapChannels) {
		this.isSwapChannels = isSwapChannels;
	}

	/**
	 * @return the isAscendingSort
	 */
	public boolean isAscendingSort() {
		return isAscendingSort;
	}

	/**
	 * @param isAscendingSort the isAscendingSort to set
	 */
	public void setAscendingSort(boolean isAscendingSort) {
		this.isAscendingSort = isAscendingSort;
	}

	public SorterType getSorterType() {
		return sorterType;
	}
	
	/**
	 * @return the compOrder
	 */
	public CompOrder getCompOrder() {
		return compOrder;
	}

	/**
	 * @param compOrder the compOrder to set
	 */
	public void setCompOrder(CompOrder compOrder) {
		this.compOrder = compOrder;
	}

	/**
	 * @return the swap
	 */
	public SwapChannel getSwap() {
		return swap;
	}

	/**
	 * @param swap the swap to set
	 */
	public void setSwap(SwapChannel swap) {
		this.swap = swap;
	}

}

