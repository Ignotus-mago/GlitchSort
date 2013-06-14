package net.paulhertz.glitchsort;
/*
 * Copyright (c) 2011, Paul Hertz This code is free software; you can
 * redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version
 * 3.0 of the License, or (at your option) any later version.
 * http://www.gnu.org/licenses/lgpl.html This software is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details. You should have
 * received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301, USA
 * 
 * @author		Paul Hertz
 * @created		July 13, 2012 
 * @modified	June 6, 2013
 * @version  	1.0b10 for Processing 2.0
 *				- various minor changes. The biggest change is that this version runs in Processing 2.0
 *				- Same old manual
 *				- save (s) and save a copy (S) commands, revert to saved (r) and revert to original (R)
 *				- open file (o) and load file to snapshot buffer (O)
 *				- rotate right (t) and rotate left (T)
 *				- added zigzag percent number box, sets percent of blocks that get zigzag sorted on each pass
 *				- ***** requires Processing 2.0, processing.org  *****
 *				- ***** requires ControlP5 2.0.4, www.sojamo.de/code ***** 
 * version		1.0b9 for Processing 1.5.1
 *				- added '_' (underscore) key command to turn 4 times repeating lastCommand
 *				- fixed dragging to work both when control panel is visible and when it is not, 
 *				  without shift key--covers most cases, but doesn't track collapsed panels
 *				- added scaledLowPass method, low pass filter each RGB channel with different 
 *				  FFT block size (64, 32, 16), component order depends on current Component Sorting Order setting
 *                Currently only triggered with ')' key command, works best when pixel dimension are multiples of 64.
 *				- added ZigzagStyle enum and zigzagStyle variable to set zigzag sorting to random angles, aligned angles, 
 *				  or angles permuted in blocks of four
 *				- added global variables for control panel location and width
 *				- added flipX and flipY methods to Zigzagger to handle changing zigzag angle
 *				- changed default settings of statistical FFT
 *				- various small fixes 
 * version 	1.0b8a
 * 				- changes from last version
 *				- fixed denoise command to include edge and corner pixels
 *				- added lastCommand variable, tracks last key command in "gl<>9kjdGLKJD" 
 * This version has a new reference manual for version 1.0b8. 
 * If it wasn't included, see http://paulhertz.net/factory/2012/08/glitchsort2/.
 * 				
 * 
 */ 

// uses pixel sorting to imitate wild glitches
// by Paul Hertz, 2012
// http://paulhertz.net/
// updates: http://paulhertz.net/factory/2012/08/glitchsort2/
// requires: 
//   Processing: http://processing.org/
//   ControlP5 library for Processing: http://www.sojamo.de/libraries/controlP5/


// ISSUES
// 0. Processing 2.0 resolved the image memory leak problem: this version of GlitchSort runs in Processing 2.0 (not 1.5.1!).
// 1. Type 'r' (reload) or 'f' fit to screen after loading the first picture to get correct window size. (Processing 1.5.1).
// 2. Using return key to load a file from the file dialog sometimes causes application to hang. Double-click works.
// 3. The display window may still hide a row or two of pixels, though I think I have 
//    fixed this. You can drag it a little bigger.
// 4. Audify ('/' and '\' is new and still kludgy, but the bugs that would cause a crash in 
//   1.0b7 pre-release "c" seem to have been fixed.
// 5. The Minim library routines I use for FFT are now deprecated, but functional. 
// 6. There must be other issues. 


import java.awt.Container;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.imageio.*;
import javax.imageio.stream.*;

import processing.core.*;
import ddf.minim.analysis.*;
import ddf.minim.*;

import controlP5.*;

import net.paulhertz.glitchsort.constants.*;


// uses pixel sorting, quantization, FFT, etc. to imitate wild glitches, audifies pixels
// by Paul Hertz, 2012
// http://paulhertz.net/

// press spacebar to show or hide the control panel
// option-drag on control panel bar to move control panel
// shift-drag on image to pan large image (no shift key needed if control panel is hidden)
// press 'f' to toggle fit image to screen
// press 'o' to open a file
// press 'O' to load a file to the snapshot buffer
// press 's' to save display to a timestamped .png file
// press 'S' to save display to a timestamped .png file as a copy
// press 'r' to revert to the most recently saved version of the file
// press 'R' to revert to the oldest version of the file
// press 't' to turn the image 90 clockwise
// press 'g' to sort the pixels (glitch)
// press 'l' to sort in zigzag-scanned blocks
// press 'z' to undo the last action
// press '1' to select quick sort
// press '2' to select shell sort
// press '3' to select bubble sort
// press '4' to select insert sort
// press 'a' to change sort order to ascending or descending
// press 'b' to toggle random breaks in sorters
// press 'x' to toggle color channel swapping (glitchy!)
// press 'c' to step through the color channels swaps
// press '+' or '-' to step through color component orderings used for sorting
// press 'y' to turn glitch cycling on and off (for glitch steps > 1)
// press '[' or ']' to decrease or increase glitch steps
// press '{' or '}' to cycle through Shell sort settings
// press 'd' to degrade the image with low quality JPEG compression
// press UP or DOWN arrow keys to change degrade quality
// press 'p' to reduce (quantize) the color palette of the image
// press LEFT or RIGHT arrow keys to change color quantization
// press '<' or ',' to shift selected color channel one pixel left
// press '>' or '.' to shift selected color channel one pixel right
// press '9' to denoise image with a median filter
// press 'n' to grab a snapshot of the current image
// press 'u' to load the most recent snapshot
// press 'm' to munge the current image with the most recent snapshot and the undo buffer
// press 'j' to apply equalizer FFT
// press 'k' to apply statistical FFT
// press '/' to turn audify on and execute commands on a single block of pixels
// press '\' to turn audify off
// press '_' to turn 90 degrees and execute last command, four times
// press ')' to run scaled low pass filter
// press 'v' to turn verbose output on and off
// press 'h' to show help message


/* TODO  teh list
 * 
 * bug fix for sorting - blocks or lines get skipped (done)
 * "real-time" compositing preview
 * break out commands into command pattern class that can be effectively journaled
 * create a genetic algorithm driven version
 * sorting breaks at user-selected pixel value
 * interactive channel-shifting
 * more buffers
 * save and load FFT settings
 * termites
 * performance interface
 *     load image list and step through it (with programmable Markov chaining)
 *     multiple buffers
 *     pass audio to another app with osc
 *     perhaps: use new version of minim library
 *     "Stick" audio sources to locations, select multiple blocks
 *     real time glitch from camera
 * 
 * 
 */

@SuppressWarnings("serial")
public class GlitchSort extends PApplet {
	/** the current component order to use for sorting */
	CompOrder compOrder;
	/** handy variable for stepping through the CompOrder enum */
	int compOrderIndex = 0;
	/** the current channel swapping scheme for glitchy color fx */
	SwapChannel swap = SwapChannel.BB;
	/** ordering of pixels in subarray to be sorted, relative to source array. Not yet in use. */
	public static enum SortFormat {ROW, SQUARE, DIAGONAL;}
	/** current format of pixels to use in sorting a subarray of pixels from an image */
	SortFormat sortFormat = SortFormat.ROW;
	/** default is random orientations for zigzag sorting */
	ZigzagStyle zigzagStyle = ZigzagStyle.RANDOM;
	/** a SortSelector offers a strategy to manage different sorting algorithms */
	SortSelector sortTool;
	/** the most recently saved version of the selected file */
	File displayFile;
	/** the original file, selected with the 'open' command */
	File originalFile;
	/** the primary image to display and glitch */
	PImage img;
	/** an image buffer used to undo the most recent operation, usually contains an earlier version of the primary image */
	PImage bakImg;
	/** a version of the image scaled to fit the screen dimensions, for display only */
	PImage fitImg;
	/** a snapshot of the primary image, used as an extended undo buffer and for the "munge" operation */
	PImage snapImg;
	/** true if image should fit screen, otherwise false */
	boolean isFitToScreen = false;
	/** maximum width for the display window */
	int maxWindowWidth;
	/** maximum height for the display window */
	int maxWindowHeight;
	/** width of the image when scaled to fit the display window */
	int scaledWidth;
	/** height of the image when scaled to fit the display window */
	int scaledHeight;
	/** current width of the frame (display window) */
	int frameWidth;
	/** current height of the frame (display window) */
	int frameHeight;
	/** flags when a large image can be dragged (translated) in the display window */
	boolean isDragImage = false;
	/** translation on the x-axis for an image larger than the display window */
	int transX = 0;
	/** translation on the y-axis for an image larger than the display window */
	int transY = 0;
	/** reference to the frame (display window) */
	Frame myFrame;
	/** true if sorting is interrupted, causing glitches. if false, horizontal lines of pixels are completely sorted */
	boolean randomBreak = true;
	/** true if lots of output to the monitor is desired (useful for debugging) */
	boolean verbose = false;
	/** true if pixels values are sorted in ascending numeric order */
	boolean isAscendingSort = false;
	/** true if pixels that are exchanged in sorting swap a pair of channels, creating color artifacts */
	boolean isSwapChannels = false;
	/** an array of row numbers for the horizontal lines of pixels, used when sorting */
	int[] rowNums;
	/** the current row of pixels being sorted */
	int row = 0;	
	/** 
	 * a value from 1..999 that determines how often a sorting method is interrupted.  
	 * In general, higher values decrease the probability of interruption, and so do more sorting. 
	 * However, each sorting method behaves differently. Quick sort is sensitive from 1..999. 
	 * Shell sort seems to do best from 900..999; lower values result in sorting only at the the image edge.
	 * Bubble sort does well from 990..999: at 999 it will diffuse the pixels. Insert sort seems 
	 * to be effective from 990..999. 
	 * TODO: create a more intuitive setting for breakpoint, with greater precision where needed.
	 */
	float breakPoint = 500;
	/** number of partitions of pixel rows, (1/glitchSteps * number of rows) rows of pixels 
	    are sorted (glitched) each time the sort command is executed */
	float glitchSteps = 1;
	/** if true, sorting steps are cyclical: rows will be sorted by successive sort commands
	    and the order in which rows are sorted will not be shuffled until all rows are sorted. 
	    if false, the 1/glitchSteps rows are sorted and then the row order is shuffled */
	boolean isCycleGlitch = false;
	RangeManager ranger; 
	RangeManager zigzagRanger; 
	/** the JPEG quality setting for degrading the image by saving it as a JPEG and then reloading it */
	float degradeQuality = 0.125f;
	/** the value below which the maximum absolute difference between color channel values induces munging */
	int mungeThreshold = 16;
	/** "munging" compares the current image with the undo buffer. When isMungeInverted is false, 
	 * pixels outside the difference threshold are replaced with corresponding pixels from the snapshot buffer.
	 * When isMungeInverted is true, pixels within the threshold are replaced. */
	boolean isMungeInverted = false;
	/** Color quantizer, for color reduction, using an octree.  */
	ImageColorQuantizer quant;
	/** The number of colors to reduce to, should not exceed 255 limit imposed by octree */
	int colorQuantize = 32;
	/** maximum dimension of zigzag sorting block */
    int zigzagCeiling = 64;
    /** minimum dimension of zigzag sorting block */
    int zigzagFloor = 8;
    /** percentage of blocks that will be zigzag sorted */
    float zigzagPercent = 100.0f;
    // ratio and divisor values for Shell sort, feel free to add your own pairs
    int[] shellParams = {2,3, 2,5, 3,5, 3,7, 3,9, 4,7, 4,9, 5,7, 5,9, 5,11, 8,13};
    // shell params index
    int shellIndex = 8;
    boolean isShiftR = true;
    boolean isShiftG = false;
    boolean isShiftB = false;
    // file count for filenames in sequence
    int fileCount = 0;
    // timestamp for filenames
    String timestamp; 
   
   // FFT
   Minim minim;
   FFT fft;
   int zigzagBlockWidth = 128;
   int fftBlockWidth = 64;
   int bufferSize;
   float sampleRate = 44100.0f;
   public float eqMax = 1;
   public float eqMin = -1;
   public float eqScale = 1;
   public float eqGain = 1;
   float[] eq;
   //* array to store average amplitude for each range of bands in averaged FFT */
   double[] binTotals;
   //* array to store band indices in averaged FFT band, has some problems */
   ArrayList<IntRange> bandList;
   //* array to store band low and high frequencies for each averaged FFT band */
   ArrayList<FloatRange> freqList;
   int minBandWidth = 11;
   int bandsPerOctave = 3;
   /** when we call calculateEqBands, the actual number of bands may differ from the internal value  */
   int calculatedBands;
   /** default maximum number of eq bands */
   int eqBands = 33;
   boolean isEqGlitchBrightness = true;
   boolean isEqGlitchHue = false;
   boolean isEqGlitchSaturation = false;
   boolean isEqGlitchRed = false;
   boolean isEqGlitchGreen = false;
   boolean isEqGlitchBlue = false;
   int eqPos = 0;
   boolean isStatGlitchBrightness = true;
   boolean isStatGlitchHue = false;
   boolean isStatGlitchSaturation = false;
   boolean isStatGlitchRed = false;
   boolean isStatGlitchGreen = false;
   boolean isStatGlitchBlue = false;
   /** for statistical FFT, number of standard deviations to the left of mean */
   public float leftBound = -0.25f, defaultLeftBound = leftBound;
   /** for statistical FFT, number of standard deviations to the right of mean */
   public float rightBound = 5.0f, defaultRightBound = rightBound;
   /** factor to multiiply values within left and right bounds of mean */
   float boost = 2.0f, defaultBoost = boost;
   /** factor to multiply values outside left and right bounds of mean */
   float cut = 0.5f, defaultCut = cut;
   public boolean isLowFrequencyCut = true;
   float[] audioBuf;

   // Control Panel
   ControlP5 controlP5;
   Group settings;
   Group fftSettings;
   Tab settingsTab;
   Tab fftSettingsTab;
   public int eqH = 100;
   public int controlPanelHeight = 392;
   public int controlPanelWidth = 284;
   public int controlPanelX = 4;
   public int controlPanelY = 36;
   DecimalFormat twoPlaces;
   DecimalFormat noPlaces;
   List<ControllerInterface<?>> mouseControls;
   String sliderIdentifier = "_eq";
   
   // Audio
   char lastCommand;
   public GlitchSignal glitchSignal;
   public AudioOutput out;
   
	/**
	 * @param args
	 * Entry point used in Eclipse IDE
	 */
	public static void main(String[] args) {
		PApplet.main(new String[] { "--present", "GlitchSort" });
	}

	public void setup() {
		println("Screen: "+ displayWidth +", "+ displayHeight);
		// println("Display: "+ displayWidth +", "+ displayHeight);
		size(640, 480);
		smooth();
		// max window width is the screen width
		maxWindowWidth = displayWidth;
		// leave window height some room for title bar, etc.
		maxWindowHeight = displayHeight - 56;
		// image to display
		img = createImage(width, height, ARGB);
		// image for undo 
		bakImg = createImage(width, height, ARGB);
		// the primary tool for sorting
		sortTool = new SortSelector(this);
		sortTool.setRandomBreak(randomBreak);
		// initial order of color channels for sorting
		compOrder = CompOrder.values()[compOrderIndex];
		// initialize number formatters
		initDecimalFormat();
		initFFT();
	    // initialize ControlP5 and build our control panels
		controlP5 = new ControlP5(this);
		// load Glitch panel
		loadGlitchPanel();
		// load FFT panel
		loadFFTPanel(eqH, eqMin, eqMax);
		// initialize panel settings, now that panels are loaded
		initPanelSettings();
		printHelp();
		// TODO include version number here
		println("GlitchSort version 1.0b10, created June 6, 2013, for Processing 2.0");
		// okay now to open an image file
		chooseFile();
		// Processing initializes the frame and hands it to you in the "frame" field.
		// Eclipse does things differently. Use findFrame method to get the frame in Eclipse.
		myFrame = findFrame();
		myFrame.setResizable(true);
		// the first time around, window won't be resized, a reload should resize it
		revert(false);
		// initialize timestamp, used in filename
	    // timestamp = year() + nf(month(),2) + nf(day(),2) + "-"  + nf(hour(),2) + nf(minute(),2) + nf(second(),2);
	    timestamp = nf(day(),2) + nf(hour(),2) + nf(minute(),2) + nf(second(),2);
	}
	
	/* (non-Javadoc)
	 * @see processing.core.PApplet#stop()
	 * we need this to assure that minim stops on exit
	 */
	public void stop() {
		minim.stop();
		super.stop();
	}
	
	/**
	 * Sets up FFT buffer at fftBlockWidth * fftBlockWidth, calls calculateEqBands to calculates the number 
	 * of equalizer bands available for current buffer size, initializes eq array.
	 */
	public void initFFT() {
	    minim = new Minim(this);
	    // we process square blocks of pixels as if they were an audio signal
	    bufferSize = fftBlockWidth * fftBlockWidth;
	    fft = new FFT(bufferSize, sampleRate);
	    // we do our own calculation of logarithmic bands
       	calculateEqBands();
       	// calculateEqBnds sets the variable calculatedBands
	    eq = new float[calculatedBands];
		java.util.Arrays.fill(eq, 0);
	}
	
	/**
	 * @param newSize   new size for edge of a pixel block, must be a power of 2
	 */
	public void resetFFT(int newSize) {
		fftBlockWidth = newSize;
		if (true) println("-- fftBlockWidth = "+ fftBlockWidth);
		// reset the slider, usually redundant call, should have no side effects if broadcast is off
		Slider s4 = (Slider) controlP5.getController("setFFTBlockWidth"); 
		s4.setBroadcast(false);
		s4.setValue((int) Math.sqrt(fftBlockWidth));
		s4.setBroadcast(true);
		// redraw the text label for the block size, on our control panel
		Textlabel l11 = (Textlabel) controlP5.getController("blockSizeLabel");
		l11.setText("FFT Block Size = "+ fftBlockWidth);
	    // we process square blocks of pixels as if they were an audio signal
		bufferSize = fftBlockWidth * fftBlockWidth;
		fft = new FFT(bufferSize, sampleRate);
		println("  fft timesize = "+ fft.timeSize());
	    // we do our own calculation of logarithmic bands
       	calculateEqBands();
       	// calculateEqBnds sets the variable calculatedBands for the new size of pixel block
	    eq = new float[calculatedBands];
		java.util.Arrays.fill(eq, 0);
		// create a new equalizer on the FFT control panel
		// setupEqualizer(eqPos, eqH, eqMax, eqMin);
		showEqualizerBands();
	}
	
	/**
	 * initializes the zero place and two place decimal number formatters
	 */
	public void initDecimalFormat() {
		// DecimalFormat sets formatting conventions from the local system, unless we tell it otherwise.
		// make sure we use "." for decimal separator, as in US, not a comma, as in many other countries 
		Locale loc = Locale.US;
		DecimalFormatSymbols dfSymbols = new DecimalFormatSymbols(loc);
		dfSymbols.setDecimalSeparator('.');
		twoPlaces = new DecimalFormat("0.00", dfSymbols);
		noPlaces = new DecimalFormat("00", dfSymbols);
	}
	
	/**
	 * @return   Frame where Processing draws, useful method in Eclipse
	 */
	public Frame findFrame() {
		Container f = this.getParent();
		while (!(f instanceof Frame) && f!=null)
			f = f.getParent();
		return (Frame) f;
	}
	
	/**
	 * Prints help message to the console
	 */
	public void printHelp() {
		println("press spacebar to show or hide the control panel");
		println("option-drag on control panel bar to move control panel");
		println("shift-drag on image to pan large image (no shift key needed if control panel is hidden)");
		println("press 'f' to toggle fit image to screen");
		println("press 'o' to open a file");
		println("press 'O' to load a file to the snapshot buffer");
		println("press 's' to save display to a timestamped .png file");
		println("press 'S' to save display to a timestamped .png file as a copy");
		println("press 'r' to revert to the most recently saved version of the file");
		println("press 'R' to revert to the oldest version of the file");
		println("press 't' to turn the image 90 clockwise");
		println("press 'g' to sort the pixels (glitch)");
		println("press 'l' to sort in zigzag-scanned blocks");
		println("press 'z' to undo the last action");
		println("press '1' to select quick sort");
		println("press '2' to select shell sort");
		println("press '3' to select bubble sort");
		println("press '4' to select insert sort");
		println("press 'a' to change sort order to ascending or descending");
		println("press 'b' to toggle random breaks in sorters");
		println("press 'x' to toggle color channel swapping (glitchy!)");
		println("press 'c' to step through the color channels swaps");
		println("press '+' or '-' to step through color component orderings used for sorting");
		println("press 'y' to turn glitch cycling on and off (for glitch steps > 1)");
		println("press '[' or ']' to decrease or increase glitch steps");
		println("press '{' or '}' to cycle through Shell sort settings");
		println("press 'd' to degrade the image with low quality JPEG compression");
		println("press UP or DOWN arrow keys to change degrade quality");
		println("press 'p' to reduce (quantize) the color palette of the image");
		println("press LEFT or RIGHT arrow keys to change color quantization");
		println("press '<' or ',' to shift selected color channel one pixel left");
		println("press '>' or '.' to shift selected color channel one pixel right");
		println("press '9' to denoise image with a median filter");
		println("press 'n' to grab a snapshot of the current image");
		println("press 'u' to load the most recent snapshot");
		println("press 'm' to munge the current image with the most recent snapshot and the undo buffer");
		println("press 'j' to apply equalizer FFT");
		println("press 'k' to apply statistical FFT");
		println("press '/' to turn audify on and execute commands on a single block of pixels");
		println("press '\' to turn audify off");
		println("press '_' to turn 90 degrees and execute last command, four times");
		println("press ')' to run scaled low pass filter");
		println("press 'v' to turn verbose output on and off");
		println("press 'h' to show help message");
	}

	
	public void draw() {
		if (isFitToScreen) {
			image(fitImg, 0, 0);
		}
		else {
			background(255);
			image(img, -transX, -transY);
		}
		trackMouseEq();
	}

	/**
	 * Experimental tool for setting a command sequence in variable cmd
	 */
	public void commandSequence() {
		String cmd;
		// (rotate zigzag save) 4 rotation sequence
		// cmd = "lcstlctttsttlcttstttlcts";
		// (degrade undo munge) 8 times
		// cmd = "dzmdzmdzmdzmdzmdzmdzmdzm";
		// (FFT:4 turn FFT:4 turn:3)
		// cmd = "kkkktkkkkttt";
		// (FFT turn):4
		// cmd = "ktktktkt";
		// (zigzag turn munge cycle)
		// cmd = "lmtltttmttlttmtttltm";
		// (zigzag turn cycle)
		cmd = "ltltltlt";
		// rpzimi[dzm]:
		exec(cmd);
	}

	/**
	 * Experimental tool for executing a command sequence
	 */
	public void commandSequence(char cue) {
		String cmd;
		// (char in turn cycle)
		cmd = "t"+ cue +"t"+ cue +"t"+ cue +"t"+ cue;
		// rpzimi[dzm]:
		exec(cmd);
	}

	/**
	 * Executes a supplied commend sequence
	 * @param cmd   a command sequence
	 */
	public void exec(String cmd) {
		char[] cycle = cmd.toCharArray();
		for (char ch : cycle) {
			decode(ch);
		}
	}
	
	/**
	 * Demo method of a brief animation output as PNG files 
	 */
	public void anim() {
		zigzagFloor = 24;
		zigzagCeiling = 96;
		this.setAscending(true, false);
		this.setCompOrder(CompOrder.HSB.ordinal(), false);
		this.setSwap(SwapChannel.BB, false);
		this.setIsSwapChannels(true, false);
		String cmd = "nskl stlttt sttltt stttltmc";
		for (int i = 0; i < 8; i++) {
			exec(cmd);
		}
	}

	
	/**
	 * Uses statistical FFT to reduce high frequencies in R, G and B channels. Each channel is
	 * processed separately at a different scale (64, 32, and 16 pixel wide blocks). The order
	 * of the channels is determined by the current Component Sorting Order settings. If the setting
	 * uses the RGB channels, that setting determines the channel order; otherwise, a random RGB order
	 * is used. Once processed, the image is ready to be sharpened again with the statistical FFT
	 * (key command 'k'). Amazingly, the information trhown out by the lowpass can be reasonably well
	 * reconstructed, but of course it's glitchy. If the image dimensions are not evenly divisible by 64,
	 * artifacts will result. 
	 */
	public void scaledLowPass() {
		int savedCompOrderIndex = compOrderIndex;
		String ordStr = CompOrder.values()[compOrderIndex].toString();
		if (compOrderIndex > CompOrder.BGR.ordinal()) {
			ordStr = CompOrder.values()[(int) random(6)].toString();
		}
		this.setCompOrder(CompOrder.HSB.ordinal(), false);
		this.setAscending(true, false);
		setIsSwapChannels(true, false);
		this.setSwap(SwapChannel.BB, false);
		// exec("t1gttt");
		setRightBound(-0.5f);
		setLeftBound(-5f);
		if (ordStr.charAt(0) == 'R') {
			setStatChan(false, false, false, true, false, false, false);
		}
		else if (ordStr.charAt(0) == 'G') {
			setStatChan(false, false, false, false, true, false, false);
		}
		else {
			setStatChan(false, false, false, false, false, true, false);
		}
		setFFTBlockWidth(4);
		exec("tktktktk");
		if (ordStr.charAt(1) == 'R') {
			setStatChan(false, false, false, true, false, false, false);
		}
		else if (ordStr.charAt(1) == 'G') {
			setStatChan(false, false, false, false, true, false, false);
		}
		else {
			setStatChan(false, false, false, false, false, true, false);
		}
		setFFTBlockWidth(5);		
		exec("tktktktk");
		if (ordStr.charAt(2) == 'R') {
			setStatChan(false, false, false, true, false, false, false);
		}
		else if (ordStr.charAt(2) == 'G') {
			setStatChan(false, false, false, false, true, false, false);
		}
		else {
			setStatChan(false, false, false, false, false, true, false);
		}
		setFFTBlockWidth(6);		
		exec("tktktktk");
		setStatChan(true, false, false, false, false, false, false);
		setFFTBlockWidth(4);
		this.resetStat();
		//exec("tktktktk");		
		//exec("tktktktk");
		//exec("t9t9t9t9t9t9t9t9");
		// set compOrder back to previous value
		this.setCompOrder(savedCompOrderIndex, false);
		println("Channel order: "+ ordStr);
	}
	
	/* (non-Javadoc)
	 * @see processing.core.PApplet#mousePressed()
	 * if the ControlP5 control panel is hidden, permit image panning
	 * also allow panning if both tabs are not active, as at startup
	 * or any time the mouse is not within an active tab
	 * I have not handled the cases where tabs are collapsed.
	 */
	public void mousePressed() {
		if (!controlP5.isVisible() || !(settingsTab.isActive() || fftSettingsTab.isActive())) isDragImage = true;
		else {
			Rectangle r = new Rectangle(controlPanelX, controlPanelY, controlPanelWidth, controlPanelHeight);
			isDragImage = !r.contains(mouseX, mouseY);
		}
	}
	
	// handle dragging to permit large images to be panned
	// shift-drag will always work, shift key is not needed if control panel is hidden 
	public void mouseDragged() {
		if (isDragImage) {
			translateImage(-mouseX + pmouseX, -mouseY + pmouseY);
		}
	}
	
	/* (non-Javadoc)
	 * handles key presses intended as commands
	 * @see processing.core.PApplet#keyPressed()
	 */
	public void keyPressed() {
		if (key == '_') {
			commandSequence(lastCommand);
		}
		else if (key == ')') {
			scaledLowPass();
		}
		else if (key != CODED) {
			decode(key);
		}
		else {
			if (keyCode == UP) {
				incrementDegradeQuality(true);      // increment degradeQuality
			}
			else if (keyCode == DOWN) {
				incrementDegradeQuality(false);     // decrement degradeQuality
			}
			else if (keyCode == RIGHT) {
				incrementColorQuantize(true);       // incremeent colorQuantize
			}
			else if (keyCode == LEFT) {
				incrementColorQuantize(false);      // decrement colorQuantize
			}
		}
		if ("gl<>9kjdGLKJD".indexOf(key) > -1) lastCommand = key;
	}
		
	/**
	 * associates characters input from keyboard with commands
	 * @param ch   a char value representing a command
	 */
	public void decode(char ch) {
		if (ch == ' ') {
			toggleControlPanelVisibility();          // hide and show control panels
		}
		else if (ch == '1') {
			setSorter(SorterType.QUICK, false);      // use quick sort 
		}
		else if (ch == '2') {
			setSorter(SorterType.SHELL, false);      // use shell sort
		}
		else if (ch == '3') {
			setSorter(SorterType.BUBBLE, false);     // use bubble sort
		}
		else if (ch == '4') {
			setSorter(SorterType.INSERT, false);     // use insert sort
		}
		else if (ch == 'g' || ch == 'G') {
			sortPixels();                            // 'g' for glitch: sort with current algorithm
		}
		else if (ch == 'o') {
			openFile();                              // open a new file
		}
		else if (ch == 'O') {
			loadFileToSnapshot();                    // load a file to the snapshot buffer
		}
		else if (ch == 'n' || ch == 'N') {
			snap();                                  // save display to snapshot buffer
		}
		else if (ch == 'u' || ch == 'U') {
			unsnap();                                // copy snapshot buffer to display
		}
		else if (ch == 'b' || ch == 'B') {
			setRandomBreak(!randomBreak, false);     // toggle random break on or off
		}
		else if (ch == 'v' || ch == 'V') {
			//verbose = !verbose;                      // toggle verbose on or off
			println("verbose is "+ verbose);
		}
		else if (ch == 's') {
			saveFile(false);                         // save to file
		}
		else if (ch == 'S') {
			saveFile(true);                          // save to file as copy
		}
		else if (ch == '=' || ch == '+') {
			int n = (compOrderIndex + 1) % CompOrder.values().length;
			setCompOrder(n, false);                  // increment compOrderIndex
		}
		else if (ch == '-' || ch == '_') {
			int n = (compOrderIndex + CompOrder.values().length - 1) % CompOrder.values().length;
			setCompOrder(n, false);                  // decrement compOrderIndex
		}
		else if (ch == 'a' || ch == 'A') {
			// ascending or descending sort
			setAscending(!isAscendingSort, false);   // toggle ascending/descending sort
		}
		else if (ch == 'r') {
			revert(false);                           // reload display from disk
		}
		else if (ch == 'R') {
			revert(true);                                // reload display from orignal file on disk
		}
		else if (ch == 't') {
			rotatePixels(true);                          // rotate display 90 degrees right (CW)
		}
		else if (ch == 'T') {
			rotatePixels(false);                         // rotate display 90 degrees left (CCW)
		}
		else if (ch == 'x' || ch == 'X') {
			setIsSwapChannels(!isSwapChannels, false);   // toggle channel swapping (color glitching)
		}
		else if (ch == 'c' || ch == 'C') {
			int n = (swap.ordinal() + 1) % SwapChannel.values().length;
			setSwap(SwapChannel.values()[n], false);     // increment swap channel settings
		}
		else if (ch == 'z' || ch == 'Z') {
			restore();                               // copy undo buffer to display
		}
		else if (ch == 'h' || ch == 'H') {
			printHelp();                             // print help message
		}
		else if (ch == 'f' || ch == 'F') {
			fitPixels(!isFitToScreen, false);        // toggle display window size to fit to screen or not
		}
		else if (ch == 'd' || ch == 'D') {
			degrade();                               // save and reload JPEG with current compression quality
		}
		else if (ch == 'm' || ch == 'M') {
			munge();                                 // composite display and snapshot with undo buffer difference mask 
		}
		else if (ch == 'i' || ch == 'I') {
			invertMunge(!isMungeInverted, false);    // invert functioning of the difference mask for munge command
		}
		else if (ch == 'y' || ch == 'Y') {
			setCycle(!isCycleGlitch, false);         // in multi-step sort, cycle through all lines in image
		}
		else if (ch == 'p' || ch == 'P') {
			reduceColors();                          // quantize colors
		}
		else if (ch == 'l' || ch == 'L') {
			zigzag();                                // perform a zigzag sort
		}
		else if (ch == 'k' || ch == 'K') {
			statZigzagFFT();                         // perform an FFT using statistical interface settings
		}
		else if (ch == 'j' || ch == 'J') {
			eqZigzagFFT();                           // perform an FFT using equalizer interface settings
		}
		else if (ch == ';') {
			analyzeEq(true);                         // perform analysis of frequencies in image 
		}
		else if (ch == '{') {
			decShellIndex();                         // step to previous shell sort settings
		}
		else if (ch == '}') {
			incShellIndex();                         // step to next shell sort settings
		}
		else if (ch == '*') {
			anim();                                  // save an animation
		}
		else if (ch == ':') {
			testEq();                                // run a test of the FFT
		}
		else if (ch == '[') {
			incrementGlitchSteps(false);             // increase the glitchSteps value
		}
		else if (ch == ']') {
			incrementGlitchSteps(true);              // decrease the glitchSteps value
		}
		else if (ch == '/') {
			audify();                                // turn on audify
		}
		else if (ch == '\\') {
			audifyOff();                             // turn off audify
		}
		else if (ch == '9') {
			denoise();                              // denoise
		}
		else if (ch == ',' || ch == '<') {
			shiftLeft();                            // shift selected color channel left
		}
		else if (ch == '.' || ch == '>') {
			shiftRight();                           // shift selected color channel right
		}
	}
	
	/**
	 * tracks mouse movement over the equalizer in the FFT control panel
	 */
	public void trackMouseEq() {
		if (controlP5.isVisible()) {
			if (fftSettings.isVisible()) {
				mouseControls = controlP5.getMouseOverList();
				for (ControllerInterface<?> con : mouseControls) {
					if (con.getName().length() > 3 && con.getName().substring(0, 3).equals(sliderIdentifier)) {
						if (mousePressed) {
							PVector vec = con.getAbsolutePosition();
							float v = map(mouseY, vec.y, vec.y + eqH, eqMax, eqMin);
							if (v != con.getValue()) {
								// println(con.getName() +": "+ vec.y +"; mouseY: "+ mouseY +"; v = "+ v);
								con.setValue(v);
							}
						}
						else {
							if (con.getId() >= 0) {
								int bin = con.getId();
								// write out the current amplitude setting from the eq tool
								if (bin < eq.length) {
									String legend = "band "+ bin +" = "+ twoPlaces.format(eq[bin]);
									if (null != binTotals && bin < binTotals.length) {
										legend += ", bin avg = "+ twoPlaces.format(binTotals[bin]);
										// legend += ", bins "+ bandList.get(eq.length - bin - 1).toString();
										// get indices of the range of bands covered by each slider and calculate their center frequency
										IntRange ir = bandList.get(bin);
										legend += ", cf = "+ twoPlaces.format((fft.indexToFreq(ir.upper) + fft.indexToFreq(ir.lower)) * 0.5f);
										// get the scaling value set by the user
									}
									((Textlabel)controlP5.getController("eqLabel")).setValue(legend);
								}
							}
						}
					}
				}
			}
			else {
				// println("conditions not met");
			}
		}
	}

	
    /********************************************/
    /*                                          */
    /*           >>> CONTROL PANEL <<<          */
    /*                                          */
    /********************************************/
	
	/**
	 * Shows or hides the control panel
	 */
	public void toggleControlPanelVisibility() {
		boolean p5Vis = controlP5.isVisible();
		if (p5Vis) {
			controlP5.hide();
		}
		else {
			controlP5.show();
		}
	}
	
	/**
	 * Initializes and arranges the "glitch" control panel widgets
	 * TODO control panel
	 */
	public void loadGlitchPanel() {
		int panelBack = color(123, 123, 144, 255);
		int yPos = 4;
		int step = 18;
		int spacer = 4;
		int widgetH = 14;
		int labelW = 144;
		int panelHeight = controlPanelHeight;
		settings = controlP5.addGroup("Glitch", controlPanelX, controlPanelY, controlPanelWidth);
		settings.setBackgroundColor(panelBack);
		settings.setBackgroundHeight(panelHeight);
		settings.setBarHeight(widgetH + 4);
		settings.setMoveable(false);     // option-drag on bar to move menu not permitted
		// add widgets
		// row of buttons: open, save, revert
		Button b1 = controlP5.addButton("openFile", 0).setPosition(8, yPos).setSize(76, widgetH);
		b1.setGroup(settings);
		b1.getCaptionLabel().set("Open (o)");
		Button b2 = controlP5.addButton("saveFile", 0).setPosition(controlPanelWidth/3 + 4, yPos).setSize(76, widgetH);
		b2.setGroup(settings);
		b2.getCaptionLabel().set("Save (s)");
		Button b3 = controlP5.addButton("revert", 0).setPosition(2 * controlPanelWidth/3 + 4, yPos).setSize(76, widgetH);
		b3.setGroup(settings);
		b3.getCaptionLabel().set("Revert (r)");
		// fit to screen/show all pixels toggle, rotate and undo buttons
		yPos += step;
		CheckBox ch0 = controlP5.addCheckBox("fitPixels", 8, yPos + 2);
		ch0.setGroup(settings);
		ch0.setColorForeground(color(120));
		ch0.setColorActive(color(255));
		ch0.setColorLabel(color(255));
		ch0.setItemsPerRow(3);
		ch0.setSpacingColumn((controlPanelWidth - 8)/4);
		// add items to the checkbox
		ch0.addItem("Fit To Screen (f)", 1);
		ch0.setColorForeground(color(233, 233, 0));
		Button b5 = controlP5.addButton("rotatePixels", 0).setPosition(2 * controlPanelWidth/3 + 4, yPos).setSize(76, widgetH);
		b5.setGroup(settings);
		b5.getCaptionLabel().set("Turn 90 (t)");
		Button b6 = controlP5.addButton("restore", 0).setPosition(controlPanelWidth/3 + 4, yPos).setSize(76, widgetH);
		b6.setGroup(settings);
		b6.getCaptionLabel().set("Undo (z)");
		// sorting section
		yPos += step + spacer;
		Textlabel l1 = controlP5.addTextlabel("sorterLabel", "Sorting", 8, yPos);
		l1.setGroup(settings);
		Textlabel l1u = controlP5.addTextlabel("sortingLabelUnder", "________________________________", 8, yPos + 3);
		l1u.setGroup(settings);
		// sort  button
		yPos += step;
		Button b4 = controlP5.addButton("sortPixels", 0).setPosition(8, yPos).setSize(76, widgetH);
		b4.setGroup(settings);
		b4.getCaptionLabel().set("Sort (g)");
		// sorter selection radio buttons
		yPos += step + 2;
		RadioButton r1 = controlP5.addRadioButton("setSorter", 8, yPos);
		r1.setGroup(settings);
		r1.setColorForeground(color(120));
		r1.setColorActive(color(255));
		r1.setColorLabel(color(255));
		r1.setItemsPerRow(5);
		r1.setSpacingColumn(40);
		r1.setNoneSelectedAllowed(false);
		// enum SorterType {QUICK, SHELL, BUBBLE, INSERT;} 
		int n = 0;
		labelW = 32;
		r1.addItem("QUICK", n++);
		r1.addItem("SHELL", n++);
		r1.addItem("BUBBLE", n++);
		r1.addItem("INSERT", n++);
		setRadioButtonStyle(r1, labelW);
		/* r1.activate("QUICK"); */ // will throw a (non-fatal but annoying) error, see startup method
		// sorting checkboxes
		yPos += step - 4;
		CheckBox ch2 = controlP5.addCheckBox("Sorting", 8, yPos);
		ch2.setGroup(settings);
		ch2.setColorForeground(color(120));
		ch2.setColorActive(color(255));
		ch2.setColorLabel(color(255));
		ch2.setItemsPerRow(3);
		ch2.setSpacingColumn((controlPanelWidth - 8)/4);
		// add items to the checkbox
		ch2.addItem("Ascending", 1);
		ch2.addItem("Break", 2);
		ch2.addItem("Swap", 3);
		ch2.setColorForeground(color(233, 233, 0));
		ch2.activate(1);
		// breakPoint number box
		yPos += step;
		Numberbox n1 = controlP5.addNumberbox("setBreakpoint", breakPoint, 8, yPos, 100, widgetH);
		n1.setGroup(settings);
		n1.setMultiplier(1f);
		n1.setDecimalPrecision(1);
		n1.setMin(1.0f);
		n1.setMax(999.0f);
		n1.getCaptionLabel().set("");
		// label for breakPoint number box
		Textlabel l2 = controlP5.addTextlabel("breakpointLabel", "Breakpoint: " + sortTool.sorter.getSorterType().toString(), 112, yPos + 4);
		l2.setGroup(settings);
		// glitchSteps slider
		yPos += step;
		Slider s1 = controlP5.addSlider("setGlitchSteps", 1, 100.1f, 1, 8, yPos, 101, widgetH);
		s1.setGroup(settings);
		s1.setDecimalPrecision(0);
		s1.getCaptionLabel().set("");
		s1.setSliderMode(Slider.FLEXIBLE);
		// label for glitchSteps slider
		Textlabel l3 = controlP5.addTextlabel("glitchStepsLabel", "Steps = "+ (int)glitchSteps, 112, yPos + 4);
		l3.setGroup(settings);
		// cycle checkbox
		CheckBox ch3 = controlP5.addCheckBox("Glitchmode", 2 * controlPanelWidth/3 + 4, yPos + 2);
		ch3.setGroup(settings);
		ch3.setColorForeground(color(120));
		ch3.setColorActive(color(255));
		ch3.setColorLabel(color(255));
		ch3.setItemsPerRow(3);
		ch3.setSpacingColumn((controlPanelWidth - 8)/4);
		// add items to the checkbox
		ch3.addItem("Cycle", 1);
		ch3.setColorForeground(color(233, 233, 0));
		ch3.deactivate(0);		
		// sort order
		// label the radio button group
		yPos += step;
		Textlabel l5 = controlP5.addTextlabel("compOrderLabel", "Component Sorting Order:", 8, yPos + 4);
		l5.setGroup(settings);
		// move to next row
		yPos += step;
		RadioButton r2 = controlP5.addRadioButton("setCompOrder", 8, yPos);
		r2.setGroup(settings);
		r2.setColorForeground(color(120));
		r2.setColorActive(color(255));
		r2.setColorLabel(color(255));
		r2.setItemsPerRow(6);
		r2.setSpacingColumn(32);
		r2.setSpacingRow(4);
		// enum CompOrder {RGB, RBG, GBR, GRB, BRG, BGR, HSB, HBS, SBH, SHB, BHS, BSH;}
		n = 0;
		labelW = 24;
		r2.addItem("RGB", n++);
		r2.addItem("RBG", n++);
		r2.addItem("GBR", n++);
		r2.addItem("GRB", n++);
		r2.addItem("BRG", n++);
		r2.addItem("BGR", n++);
		r2.addItem("HSB", n++);
		r2.addItem("HBS", n++);
		r2.addItem("SBH", n++);
		r2.addItem("SHB", n++);
		r2.addItem("BHS", n++);
		r2.addItem("BSH", n++);
		setRadioButtonStyle(r2, labelW);
		r2.setNoneSelectedAllowed(false);
		/* r2.activate("RGB"); */ // will throw a (non-fatal but annoying) error, see startup method
		// channel swap
//		yPos += step + step/2;
//		Textlabel l6 = controlP5.addTextlabel("swapLabel", "Swap Channels:", 8, yPos + 4);
//		l6.setGroup(settings);
		yPos += step + step - 4;
		int inset = 80;
		Textlabel l7 = controlP5.addTextlabel("sourceLabel", "Swap Source:", 8, yPos);
		l7.setGroup(settings);
		RadioButton r3 = controlP5.addRadioButton("setSourceChannel", inset, yPos);
		r3.setGroup(settings);
		r3.setColorForeground(color(120));
		r3.setColorActive(color(255));
		r3.setColorLabel(color(255));
		r3.setItemsPerRow(3);
		r3.setSpacingColumn(32);
		n = 0;
		r3.addItem("R1", n++);
		r3.addItem("G1", n++);
		r3.addItem("B1", n++);
		setRadioButtonStyle(r3, labelW);
		r3.setNoneSelectedAllowed(false);
		yPos += step;
		Textlabel l8 = controlP5.addTextlabel("targetLabel", "Swap Target:", 8, yPos);
		l8.setGroup(settings);
		RadioButton r4 = controlP5.addRadioButton("setTargetChannel", inset, yPos);
		r4.setGroup(settings);
		r4.setColorForeground(color(120));
		r4.setColorActive(color(255));
		r4.setColorLabel(color(255));
		r4.setItemsPerRow(3);
		r4.setSpacingColumn(32);
		n = 0;
		r4.addItem("R2", n++);
		r4.addItem("G2", n++);
		r4.addItem("B2", n++);
		setRadioButtonStyle(r4, labelW);
		r4.setNoneSelectedAllowed(false);
		// zigzag intRange
		yPos += step;
		// use values that permit full stepwise range
		// addRange(name, min, max, defaultMin, defaultMax, x, y, w, h) 
		Range r01 = controlP5.addRange("setZigzagRange", 4, 144, 8, 64, 8, yPos, 160, widgetH);
		r01.setGroup(settings);
		r01.setDecimalPrecision(0);
		r01.setLowValue(8);
		r01.setHighValue(64);
		r01.getCaptionLabel().set("");
		// label for zigzag range slider
		Textlabel l10 = controlP5.addTextlabel("zigzagRangeLabel", "Z Range", 170, yPos + 4);
		l10.setGroup(settings);
		// zigzag button
		Button b12 = controlP5.addButton("zigzag", 0).setPosition(2 * controlPanelWidth/3 + 28, yPos).setSize(60, widgetH);
		b12.setGroup(settings);
		b12.getCaptionLabel().set("Zigzag (l)");
		yPos += step;
		// zigzag sorting style
		RadioButton r6 = controlP5.addRadioButton("setZigzagStyle", 8, yPos);
		r6.setGroup(settings);
		r6.setColorForeground(color(120));
		r6.setColorActive(color(255));
		r6.setColorLabel(color(255));
		r6.setItemsPerRow(3);
		r6.setSpacingColumn(48);
		n = 0;
		labelW = 40;
		r6.addItem("Random", n++);
		r6.addItem("Align", n++);
		r6.addItem("Permute", n++);
		setRadioButtonStyle(r6, labelW);
		r6.setNoneSelectedAllowed(false);
		// zigzagPercent number box
		Numberbox n2 = controlP5.addNumberbox("setZigzagPercent", zigzagPercent, 218, yPos, 48, widgetH);
		n2.setGroup(settings);
		n2.setMultiplier(1f);
		n2.setDecimalPrecision(1);
		n2.setMin(1.0f);
		n2.setMax(100.0f);
		n2.getCaptionLabel().set("");
		// label for zigzagPercent number box
		Textlabel l10a = controlP5.addTextlabel("zigzagPercentLabel", "%:", 192, yPos + 2);
		l10a.setGroup(settings);
		// degrading, compositing section
		yPos += step + spacer;
		Textlabel l20 = controlP5.addTextlabel("degradeLabel", "Degrade + Quantize + Munge", 8, yPos);
		l20.setGroup(settings);
		Textlabel l20u = controlP5.addTextlabel("degradeLabelUnder", "________________________________", 8, yPos + 3);
		l20u.setGroup(settings);		
		// degrade controls
		yPos += step;
		Slider s2 = controlP5.addSlider("setQuality", 100, 0, 13.0f, 8, yPos, 128, widgetH);
		s2.setGroup(settings);
		s2.setDecimalPrecision(1);
		s2.getCaptionLabel().set("");
		s2.setSliderMode(Slider.FLEXIBLE);
		// label for degrade quality slider
		Textlabel l4 = controlP5.addTextlabel("QualityLabel", "Quality", 137, yPos + 4);
		l4.setGroup(settings);
		// degrade button
		Button b10 = controlP5.addButton("degrade", 0).setPosition(2 * controlPanelWidth/3 + 28, yPos).setSize(60, widgetH);
		b10.setGroup(settings);
		b10.getCaptionLabel().set("Degrade (d)");
		// reduce colors slider
		yPos += step;
		Slider s3 = controlP5.addSlider("setColorQuantize", 2, 128, colorQuantize, 8, yPos, 127, widgetH);
		s3.setGroup(settings);
		s3.setDecimalPrecision(0);
		s3.getCaptionLabel().set("");
		s3.setSliderMode(Slider.FLEXIBLE);
		// label for color quantize slider
		Textlabel l9 = controlP5.addTextlabel("colorQuantizeLabel", "Colors = "+ colorQuantize, 137, yPos + 4);
		l9.setGroup(settings);
		// reduce colors button
		Button b11 = controlP5.addButton("reduceColors", 0).setPosition(2 * controlPanelWidth/3 + 28, yPos).setSize(60, widgetH);
		b11.setGroup(settings);
		b11.getCaptionLabel().set("Reduce (p)");
		yPos += step;
		Button b14 = controlP5.addButton("shiftLeft", 0).setPosition(8, yPos).setSize(32, widgetH);
		b14.setGroup(settings);
		b14.getCaptionLabel().set(" << ");		
		RadioButton r5 = controlP5.addRadioButton("Shift", 48, yPos + 2);
		r5.setGroup(settings);
		r5.setColorForeground(color(120));
		r5.setColorActive(color(255));
		r5.setColorLabel(color(255));
		r5.setItemsPerRow(3);
		r5.setSpacingColumn((controlPanelWidth - 8)/12);
		// add items to the checkbox
		r5.addItem("R", 1);
		r5.addItem("G", 2);
		r5.addItem("B", 3);
		r5.setColorForeground(color(233, 233, 0));
		r5.activate(0);		
		Button b15 = controlP5.addButton("shiftRight", 0).setPosition(48 + 4 * ((controlPanelWidth - 8)/12), yPos).setSize(32, widgetH);
		b15.setGroup(settings);
		b15.getCaptionLabel().set(" >> ");		
		Button b13 = controlP5.addButton("denoise", 0).setPosition(2 * controlPanelWidth/3 + 28, yPos).setSize(60, widgetH);
		b13.setGroup(settings);
		b13.getCaptionLabel().set("Denoise (9)");
		// snap, unsnap
		yPos += step + spacer;
		Button b7 = controlP5.addButton("snap", 0).setPosition(8, yPos).setSize(76, widgetH);
		b7.setGroup(settings);
		b7.getCaptionLabel().set("Snap (n)");
		Button b8 = controlP5.addButton("unsnap", 0).setPosition(controlPanelWidth/3 + 4, yPos).setSize(76, widgetH);
		b8.setGroup(settings);
		b8.getCaptionLabel().set("Unsnap (u)");
		// invert munge checkbox
		CheckBox ch6 = controlP5.addCheckBox("invertMunge", 2 * controlPanelWidth/3 + 4, yPos + 2);
		ch6.setGroup(settings);
		ch6.setColorForeground(color(120));
		ch6.setColorActive(color(255));
		ch6.setColorLabel(color(255));
		ch6.setItemsPerRow(3);
		ch6.setSpacingColumn((controlPanelWidth - 8)/4);
		// add items to the checkbox
		ch6.addItem("Invert Munge (i)", 0);
		ch6.setColorForeground(color(233, 233, 0));		
		// mungeThreshold setting
		yPos += step;
		Slider s5 = controlP5.addSlider("setMungeThreshold", 100, 1, mungeThreshold, 8, yPos, 101, widgetH);
		s5.setGroup(settings);
		s5.setDecimalPrecision(0);
		s5.getCaptionLabel().set("");
		s5.setSliderMode(Slider.FLEXIBLE);
		// label for degrade quality slider
		Textlabel l19 = controlP5.addTextlabel("mungeThresholdLabel", "Munge Threshold", 112, yPos + 4);
		l19.setGroup(settings);
		// munge button
		Button b9 = controlP5.addButton("munge", 0).setPosition(2 * controlPanelWidth/3 + 28, yPos).setSize(60, widgetH);
		b9.setGroup(settings);
		b9.getCaptionLabel().set("Munge (m)");			
		// nexxxxxt....
		yPos += step;
		// create glitch settings tab
		Tab global = controlP5.getTab("default");
		global.setLabel("");
		global.hide();
		settings.moveTo("glitch");
		settingsTab = controlP5.getTab("glitch");
		settingsTab.activateEvent(true);
		settingsTab.setLabel("  Glitch  ");
		settingsTab.setId(1);
	}

	/**
	 * Initializes and arranges the FFT control panel widgets
	 * TODO FFT panel
	 */
	public void loadFFTPanel(int h, float min, float max) {
		int panelBack = color(123, 123, 144, 255);
		int yPos = 6;
		int step = 18;
		int widgetH = 14;
		int labelW = 144;
		int panelHeight = controlPanelHeight;
		fftSettings = controlP5.addGroup("FFT", controlPanelX, controlPanelY, controlPanelWidth);
		fftSettings.setBackgroundColor(panelBack);
		fftSettings.setBackgroundHeight(panelHeight);
		fftSettings.setBarHeight(widgetH + 4);
		fftSettings.setMoveable(false);     // dragging throws absolute position off...
		// add widgets
		// legend
		Textlabel l12 = controlP5.addTextlabel("equalizerLabel", "Equalizer FFT", 8, yPos);
		l12.setGroup(fftSettings);		
		Textlabel l12u = controlP5.addTextlabel("equalizerLabelUnder", "________________________________", 8, yPos + 3);
		l12u.setGroup(fftSettings);		
		// row of buttons: 
		yPos += step + step/3;
		Button b13 = controlP5.addButton("eqZigzagFFT", 0).setPosition(8, yPos).setSize(64, widgetH);
		b13.setGroup(fftSettings);
		b13.getCaptionLabel().set("Run (j)");
		Button b14 = controlP5.addButton("resetEq", 0).setPosition(controlPanelWidth/3, yPos).setSize(64, widgetH);
		b14.setGroup(fftSettings);
		b14.getCaptionLabel().set("Reset");
		//// incorporate analysis into FFT ?
		Button b15 = controlP5.addButton("analyzeEqBands", 0).setPosition(2 * controlPanelWidth/3, yPos).setSize(64, widgetH);
		b15.setGroup(fftSettings);
		b15.getCaptionLabel().set("Analyze (;)");
		yPos += step;
		// label at bottom of eQ bands
		Textlabel l13 = controlP5.addTextlabel("eqLabel", "----", 8, yPos + h + step/2);
		l13.setGroup(fftSettings);
		// equalizer
		setupEqualizer(yPos, h, max, min);
		showEqualizerBands();
		yPos += h + step + step/2;
		// HSB/RGB checkboxes for equalizer-controlled FFT
		CheckBox ch4 = controlP5.addCheckBox("ChanEq", 8, yPos + 2);
		ch4.setGroup(fftSettings);
		ch4.setColorForeground(color(120));
		ch4.setColorActive(color(255));
		ch4.setColorLabel(color(255));
		ch4.setItemsPerRow(3);
		ch4.setSpacingColumn((controlPanelWidth - 8)/4);
		// add items to the checkbox, note that we can't use names that start with sliderIdentifier "_eq" 
		ch4.addItem("eqBrightness", 1);
		ch4.getItem(0).setCaptionLabel("Brightness");
		ch4.setColorForeground(color(233, 233, 0));
		ch4.addItem("eqHue", 2);
		ch4.getItem(1).setCaptionLabel("Hue");
		ch4.setColorForeground(color(233, 233, 0));
		ch4.addItem("eqSaturation", 3);
		ch4.getItem(2).setCaptionLabel("Saturation");
		ch4.setColorForeground(color(233, 233, 0));
		// add items to the checkbox
		ch4.addItem("eqRed", 4);
		ch4.getItem(3).setCaptionLabel("Red");
		ch4.setColorForeground(color(233, 233, 0));
		ch4.addItem("eqGreen", 5);
		ch4.getItem(4).setCaptionLabel("Green");
		ch4.setColorForeground(color(233, 233, 0));
		ch4.addItem("eqBlue", 6);
		ch4.getItem(5).setCaptionLabel("Blue");
		ch4.setColorForeground(color(233, 233, 0));
		// statistical FFT settings section
		// section label
		yPos += 2 * step;
		Textlabel l14 = controlP5.addTextlabel("statFFTLabel", "Statistical FFT", 8, yPos);
		l14.setGroup(fftSettings);
		Textlabel l14u = controlP5.addTextlabel("statFFTLabelUnder", "________________________________", 8, yPos + 3);
		l14u.setGroup(fftSettings);		
		// buttons
		yPos += step;
		Button b16 = controlP5.addButton("statZigzagFFT", 0).setPosition(8, yPos).setSize(64, widgetH);
		b16.setGroup(fftSettings);
		b16.getCaptionLabel().set("Run (k)");
		Button b17 = controlP5.addButton("resetStat", 0).setPosition(controlPanelWidth/3, yPos).setSize(64, widgetH);
		b17.setGroup(fftSettings);
		b17.getCaptionLabel().set("Reset");
		//------- begin slider
		// use a range slider for bounds
		yPos += step;
		// addRange(name, min, max, defaultMin, defaultMax, x, y, w, h) 
		Range r02 = controlP5.addRange("setStatEqRange", -5.0f, 5.0f, leftBound, rightBound, 8, yPos, 180, widgetH);
		r02.setGroup(fftSettings);
		r02.setDecimalPrecision(2);
		r02.setLowValue(leftBound);
		r02.setHighValue(rightBound);
		r02.getCaptionLabel().set("");
		// label for statistical eQ range slider
		Textlabel l16 = controlP5.addTextlabel("statEqRangeLabel", "Deviation", 190, yPos + 4);
		l16.setGroup(fftSettings);
		//------- end slider
		// number box for boost
		yPos += step;
		Numberbox n4 = controlP5.addNumberbox("setBoost", boost, 8, yPos, 40, widgetH);
		n4.setGroup(fftSettings);
		n4.setMultiplier(0.01f);
		n4.setDecimalPrecision(2);
		n4.setMin(0.0f);
		n4.setMax(8.0f);
		n4.getCaptionLabel().set("");
		// label for boost number box
		Textlabel l17 = controlP5.addTextlabel("boostLabel", "IN Scale", 48, yPos + 4);
		l17.setGroup(fftSettings);
		// number box for cut
		Numberbox n5 = controlP5.addNumberbox("setCut", cut, (controlPanelWidth - 8)/3, yPos, 40, widgetH);
		n5.setGroup(fftSettings);
		n5.setMultiplier(0.01f);
		n5.setDecimalPrecision(2);
		n5.setMin(0.0f);
		n5.setMax(8.0f);
		n5.getCaptionLabel().set("");
		// label for cut number box
		Textlabel l18 = controlP5.addTextlabel("cutLabel", "OUT Scale", (controlPanelWidth - 8)/3 + 40, yPos + 4);
		l18.setGroup(fftSettings);
		// low cut checkbox
		CheckBox ch6 = controlP5.addCheckBox("LowFreqCut", 2 * (controlPanelWidth - 8)/3 + 8, yPos + 4);		
		ch6.setGroup(fftSettings);
		ch6.setColorForeground(color(120));
		ch6.setColorActive(color(255));
		ch6.setColorLabel(color(255));
		ch6.setItemsPerRow(3);
		ch6.setSpacingColumn((controlPanelWidth - 8)/4);
		// add items to the checkbox
		ch6.addItem("lowCut", 0);
		ch6.getItem(0).setCaptionLabel("Low Cut");
		ch6.setColorForeground(color(233, 233, 0));
		// HSB/RGB checkboxes for statistically-controlled FFT
		yPos += step;
		CheckBox ch5 = controlP5.addCheckBox("ChanStat", 8, yPos + 2);		
		ch5.setGroup(fftSettings);
		ch5.setColorForeground(color(120));
		ch5.setColorActive(color(255));
		ch5.setColorLabel(color(255));
		ch5.setItemsPerRow(3);
		ch5.setSpacingColumn((controlPanelWidth - 8)/4);
		// add items to the checkbox
		ch5.addItem("statBrightness", 1);
		ch5.getItem(0).setCaptionLabel("Brightness");
		ch5.setColorForeground(color(233, 233, 0));
		ch5.addItem("statHue", 2);
		ch5.getItem(1).setCaptionLabel("Hue");
		ch5.setColorForeground(color(233, 233, 0));
		ch5.addItem("statSaturation", 3);
		ch5.getItem(2).setCaptionLabel("Saturation");
		ch5.setColorForeground(color(233, 233, 0));
		// add items to the checkbox
		ch5.addItem("statRed", 4);
		ch5.getItem(3).setCaptionLabel("Red");
		ch5.setColorForeground(color(233, 233, 0));
		ch5.addItem("statGreen", 5);
		ch5.setColorForeground(color(233, 233, 0));
		ch5.getItem(4).setCaptionLabel("Green");
		ch5.addItem("statBlue", 6);
		ch5.getItem(5).setCaptionLabel("Blue");
		ch5.setColorForeground(color(233, 233, 0));
		// section label
		yPos += 2 * step;
		Textlabel l19 = controlP5.addTextlabel("blockSizeSectionLabel", "FFT Block Size", 8, yPos);
		l19.setGroup(fftSettings);
		Textlabel l19u = controlP5.addTextlabel("blockSizeSectionLabelUnder", "________________________________", 8, yPos + 3);
		l19u.setGroup(fftSettings);		
		// slider for FFT block size can't have it in FFT Panel because of a concurrent modification error when we regenerate the FFT panel
		yPos += step;
		Slider s4 = controlP5.addSlider("setFFTBlockWidth", 3, 9, 6, 8, yPos, 64, widgetH); 
		s4.setGroup(fftSettings);
		s4.setDecimalPrecision(0);
		s4.getCaptionLabel().set("");
		Textlabel l11 = controlP5.addTextlabel("blockSizeLabel", "FFT Block Size = "+ fftBlockWidth, 76, yPos + 2);
		l11.setGroup(fftSettings);
		// move fftSettings into a tab
		fftSettings.moveTo("FFT");
		fftSettingsTab = controlP5.getTab("FFT");
		fftSettingsTab.activateEvent(true);
		fftSettingsTab.setLabel("  FFT  ");
		fftSettingsTab.setId(2);
	}
	
    /**
     * Sets left and right eQ bounds in response to control panel.
     * @param val   a value forwarded by ControlP5 that we will ignore (just in this case)
     */
    public void setStatEqRange(float val) {
    	// here's one way to retrieve the values of the range controller
		Range r1 = (Range) controlP5.getController("setStatEqRange");
		if (!r1.isInside()) {
			return;
		}
		leftBound = r1.getArrayValue()[0];
		rightBound = r1.getArrayValue()[1];
    }

    /**
	 * Sets up and draws the multi-band equalizer control.
	 * @param yPos   y-offset of control position
	 * @param h      height of a slider
	 * @param max    maximum value represented by slider
	 * @param min    minimum value represented by slider
	 */
	public void setupEqualizer(int yPos, int h, float max, float min) {
		int eqW = 8;
		int left = 8;
		// we use a fixed maximum number of bands and show or hide bands as FFT buffer size varies
		int lim = eqBands;
		for (int i = 0; i < lim; i++) {
			String token = sliderIdentifier + noPlaces.format(i);
			Slider slider = controlP5.addSlider(token).setPosition(left, yPos).setSize(eqW, h).setId(i);
			slider.setMax(max);
			slider.setMin(min);
			slider.setValue(0);
			slider.setMoveable(false).setLabelVisible(false);
			int fc = color(199, 47, 21, 255);
			slider.setColorForeground(fc);
			int bc = color(233, 233, 254, 255);
			slider.setColorBackground(bc);
			slider.setGroup(fftSettings);
			left += eqW;
		}
		if (0 == eqPos) eqPos = yPos;
	}
	
	/**
	 * removes the equalizer from the control panel, currently not used
	 */
	public void removeEqualizer() {
		int lim = eq.length;
		for (int i = 0; i < lim; i++) {
			String token = sliderIdentifier + noPlaces.format(i);
			Slider slider = (Slider) controlP5.getController(token);
			slider.remove();
		}
	}

	/**
	 * shows equalizer bands used for current FFT block size, hides others
	 */
	public void showEqualizerBands() {
		// precautionary coding. The number of eq bins (eq.length) should not excede the max number of bands.
		int lim = eq.length > eqBands ? eqBands : eq.length;
		for (int i = 0; i < lim; i++) {
			String token = sliderIdentifier + noPlaces.format(i);
			Slider slider = (Slider) controlP5.getController(token);
			slider.setVisible(true);
			// set value to 0
			slider.setValue(0);
		}
		for (int i = lim; i < eqBands; i++) {
			String token = sliderIdentifier + noPlaces.format(i);
			Slider slider = (Slider) controlP5.getController(token);
			slider.setVisible(false);
			// don't change the value, bins are out of range for ControlP5 propagated event
		}
	}
	

	/**
	 * @param rb      formats radio button style
	 * @param width   width of radio button.
	 */
	void setRadioButtonStyle(RadioButton rb, int width) {
		for (Toggle t: rb.getItems()) {
			t.setColorForeground(color(233, 233, 0));
			Label l = t.getCaptionLabel();
			l.enableColorBackground();
			l.setColorBackground(color(80));
			l.getStyle().movePadding(2,0,-1,2);
			l.getStyle().moveMargin(-2,0,0,-3);
			l.getStyle().backgroundWidth = width;
		}
	}

	/**
	 * Once control panels have been created and drawn, set up initial positions and values
	 */
	public void initPanelSettings() {
		// simplest way to avoid some annoying errors is to set various control panel radio buttons
		// after panel has been constructed
		setSorter(SorterType.BUBBLE, false);
		this.setBreakpoint(999);
		setSorter(SorterType.SHELL, false);
		this.setBreakpoint(996);
		setSorter(SorterType.QUICK, false);
		this.setBreakpoint(1);
		setCompOrder(compOrderIndex, false);
		     setSwap(SwapChannel.RR, false);
		setEqChan(true, false, false, false, false, false, false);
		setStatChan(true, false, false, false, false, false, false);
		setLowFrequencyCut(false, false);
		Slider s4 = (Slider) controlP5.getController("setFFTBlockWidth"); 
		s4.setSliderMode(Slider.FLEXIBLE);
		s4.setNumberOfTickMarks(7);
		setZigzagStyle(zigzagStyle, false);
	} 
	

    /********************************************/
    /*                                          */
    /*          >>> GLITCH COMMANDS <<<         */
    /*                                          */
    /********************************************/
	
	/**
	 * opens a user-specified file (JPEG, GIF or PNG only)
	 */
	public void openFile() {
		chooseFile();
	}
	
	/**
	 * reverts display and display buffer to last opened file
	 */
	public void revert(boolean toOriginalFile) {
		if (null != displayFile) {
			if (toOriginalFile) loadOriginalFile();	
			else loadFile();
			if (isFitToScreen) fitPixels(true, false);
			// reset row numbers and translation
			loadRowNums();
			resetRanger();
			shuffle(rowNums);
			clipTranslation();
		}
	}
	
	/**
	 * Sets the value above which the current sort method will randomly interrupt, when randomBreak 
	 * is true (the default). Each sorting method uses a distinct value from 1 to 999. Quick sort
	 * can use very low values, down to 1.0. The other sorting methods--shell sort, insert sort, 
	 * bubble sort--generally work best with higher values. 
	 * @param newBreakPoint   the breakpoint to set
	 */
	public void setBreakpoint(float newBreakPoint) {
		if (newBreakPoint == breakPoint) return;
		breakPoint = newBreakPoint;
		sortTool.sorter.setBreakPoint(breakPoint);
	}
	
	/**
	 * increments shellIndex, changes shell sort settings
	 */
	public void incShellIndex() {
		shellIndex = shellIndex < shellParams.length - 3 ? shellIndex + 2 : 0;
		int r = shellParams[shellIndex];
		int d = shellParams[shellIndex + 1];
		sortTool.shell.setRatio(r);
		sortTool.shell.setDivisor(d);
		println("ShellIndex = "+ shellIndex +", Shellsort ratio = "+ r +", divisor = "+ d);
	}
	/**
	 * decrements shellIndex, changes shell sort settings
	 */
	public void decShellIndex() {
		shellIndex = shellIndex > 1 ? shellIndex - 2 : shellParams.length - 2;
		int r = shellParams[shellIndex];
		int d = shellParams[shellIndex + 1];
		sortTool.shell.setRatio(r);
		sortTool.shell.setDivisor(d);
		println("ShellIndex = "+ shellIndex +", Shellsort ratio = "+ r +", divisor = "+ d);
	}
	
	/**
	 * Initializes rowNums array, used in stepped or cyclic sorting, with img.height elements.
	 */
	public void loadRowNums() {
		rowNums = new int[img.height];
		for (int i = 0; i < img.height; i++) rowNums[i] = i;
	}
	
	/**
	 * Initializes rowNums array to rowCount elements, shuffles it and sets row value to 0.
	 */
	public void resetRowNums(int rowCount) {
		rowNums = new int[rowCount];
		for (int i = 0; i < rowCount; i++) rowNums[i] = i;
		resetRowNums();
	}
	/**
	 * Shuffles rowNums array and sets row value to 0.
	 */
	public void resetRowNums() {
		shuffle(rowNums);
		row = 0;
		if (verbose) println("Row numbers shuffled");
	}
	
	/**
	 * Sets RangeManager stored in ranger to intial settings, with a range of img.height 
	 * and number of intervals equal to glitchSteps.
	 */
	public void resetRanger() {
		if (null == ranger) {
			ranger = new RangeManager(img.height, (int) glitchSteps);
		}
		else {
			ranger.resetCurrentIndex();
			ranger.setRange(img.height);
			ranger.setNumberOfIntervals((int) glitchSteps);
		}
		if (verbose) println("range index reset to 0");
	}

	int rCount = 0;
	/**
	 * rotates image and backup image 90 degrees clockwise
	 */
	public void rotatePixels(boolean isTurnRight) {
		if (null == img) return;
		if (isTurnRight) img = rotateImageRight(img);
		else img = rotateImageLeft(img);
		fitPixels(isFitToScreen, false);
		// rotate undo buffer image, don't rotate snapshot
		if (null != bakImg) {
			if (isTurnRight) bakImg = rotateImageRight(bakImg);
			else bakImg = rotateImageLeft(bakImg);
		}
		// load the row numbers
		loadRowNums();
		resetRanger();
		// shuffle the row numbers
		shuffle(rowNums);
		// clip translation to image bounds
		clipTranslation();
	}
	
	/**
	 * rotates image pixels 90 degrees clockwise
	 * @param image   the image to rotate
	 * @return        the rotated image
	 */
	public PImage rotateImageRight(PImage image) {
		// rotate image 90 degrees
		int h = image.height;
		int w = image.width;
		int i = 0;
		PImage newImage = createImage(h, w, ARGB);
		newImage.loadPixels();
		for (int ry = 0; ry < w; ry++) {
			for (int rx = 0; rx < h; rx++) {
				newImage.pixels[i++] = image.pixels[(h - 1 - rx) * image.width + ry];
			}
		}
		newImage.updatePixels();
		return newImage;
	}

	/**
	 * rotates image pixels 90 degrees clockwise
	 * @param image   the image to rotate
	 * @return        the rotated image
	 */
	public PImage rotateImageLeft(PImage image) {
		// rotate image 90 degrees
		int h = image.height;
		int w = image.width;
		int i = 0;
		PImage newImage = createImage(h, w, ARGB);
		newImage.loadPixels();
		for (int ry = w-1; ry >= 0; ry--) {
			for (int rx = h-1; rx >= 0; rx--) {
				newImage.pixels[i++] = image.pixels[(h - 1 - rx) * image.width + ry];
			}
		}
		newImage.updatePixels();
		return newImage;
	}
	
	
	/**
	 * tranlates the display image by a specified horizontal and vertical distances
	 * @param tx   distance to translate on x-axis
	 * @param ty   distance to translate on y-axis
	 */
	public void translateImage(int tx, int ty) {
		transX += tx;
		transY += ty;
		clipTranslation();
	}
	
	/**
	 * handles clipping of a translated image to the display window
	 */
	public void clipTranslation() {
		int limW = (frameWidth < img.width) ? img.width - frameWidth : 0;
		int limH = (frameHeight < img.height) ? img.height - frameHeight : 0;
		if (transX > limW) transX = limW;
		if (transX < 0) transX = 0;
		if (transY > limH) transY = limH;
		if (transY < 0) transY = 0;
		// println(transX +", "+ transY  +", limit width = "+ limW  +", limit height = "+ limH +", image width = "+ img.width +", image height = "+ img.height);		
	}

	/**
	 * Sorts the pixels line by line, in random order, using the current
	 * sorting method set in sortTool.
	 * @fix moved loadPixels outside loop
	 * TODO implement a cycle and row manager class
	 */
	public void sortPixels() {
		if (null == img || null == ranger) {
			println("No image is available for sorting or the ranger is not initialized (sortPixels method)");
			return;
		}
		// TODO implement methods to set sorter values for color swapping, sort order, component order
		// for the moment, we do it by providing the sortTool with top level access to the GlitchSort instance
		// and pulling the values from local variables. It would be more efficient to do this only on control panel changes. 
		sortTool.setControlState();
		backup();
		img.loadPixels();
		if (isCycleGlitch) {
			IntRange range;
			if (ranger.hasNext()) {
				range = ranger.getNext();
				println(range.toString());
			} 
			else {
				ranger.resetCurrentIndex();
				range = ranger.getNext();
				resetRowNums();
				println("starting a new cycle");
			}
			for (int i = range.lower; i < range.upper; i++) {
				int n = rowNums[i];
				if (verbose) println("sorting row "+ n +" at index "+ i);
				row++;
				int l = n * img.width;
				int r = l + img.width - 1;
				sortTool.sort(img.pixels, l, r);
			}
		}
		else {
			int rowMax = (int)(Math.round(rowNums.length / glitchSteps));
			for (int i = 0; i < rowMax; i++) {
				int n = rowNums[i];
				if (verbose) println("sorting row "+ n);
				int l = n * img.width;
				int r = l + img.width - 1;
				sortTool.sort(img.pixels, l, r);
			}
			shuffle(rowNums);
		}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	
	/**
	 * Saves a copy of the currently displayed image in img to bakImg.
	 */
	public void backup() {
		bakImg = img.get();
	}

	/**
	 * Undoes the last command. Not applicable to munge command.
	 */
	public void restore() {
		// store a copy of the current image in tempImg
		PImage tempImg = img.get();
		img = bakImg;
		bakImg = tempImg;
		// println("--- restore");
		fitPixels(isFitToScreen, false);
		// if the display image and the backup image are different sizes, we need to reset rows and translation
		loadRowNums();
		resetRanger();
		shuffle(rowNums);
		clipTranslation();
	}
	
	/**
	 * Saves a copy of the currently displayed image in img to snapImg.
	 */
	public void snap() {
//		if (null == snapImg) snapImg = createImage(width, height, ARGB);
//		snapImg.resize(img.width, img.height);
//		snapImg.copy(img, 0, 0, img.width, img.height, 0, 0, img.width, img.height);
		snapImg = img.get();
		println("took a snapshot of current state");
	}
	
	/**
	 * copies snapImg to img, undo buffer bakImg is not changed
	 */
	public void unsnap() {
		if (null == snapImg) return;
		img = snapImg.get();
		fitPixels(isFitToScreen, false);
		// if the display image and the snapshot image are different sizes, we need to reset rows and translation
		loadRowNums();
		resetRanger();
		shuffle(rowNums);
		clipTranslation();
	}
	
	/**
	 * loads a file into snapshot buffer.
	 */
	public void loadFileToSnapshot() {
		selectInput("Image file for snapshot buffer:", "snapshotFileSelected");
	}
	
	public void snapshotFileSelected(File selectedFile) {
		if (null != selectedFile) {
			noLoop();
			File snapFile = selectedFile;
			// snapImg = createImage(width, height, ARGB);
			snapImg = loadImage(snapFile.getAbsolutePath()).get();
			println("loaded "+ snapFile.getName() +" to snapshot buffer: width = "+ snapImg.width +", height = "+ snapImg.height);
			loop();
		}
		else {
			println("No file was selected");
		}
	}
	
	/**
	 * Composites the current image (img) with the snapshot (snapImg) using the undo buffer (bakImg)
	 * as a mask. When the largest absolute difference between a pixel in the image and the same
	 * pixel in the undo buffer is greater than mungeThreshold, a pixel from the snapshot will be written
	 * to the image. The undo buffer and the snapshot will be resized to the image dimensions if
	 * necessary (it's not called "munge" for nothing).
	 */
	public void munge() {
		if (null == bakImg || null == snapImg) {
			println("To munge an image you need an undo buffer and a snapshot");
			return;
		}
		if (img.width != bakImg.width || img.height != bakImg.height) {
			bakImg.resize(img.width, img.height);
		}
		if (img.width != snapImg.width || img.height != snapImg.height) {
			snapImg.resize(img.width, img.height);
		}
		img.loadPixels();
		bakImg.loadPixels();
		snapImg.loadPixels();
		int alpha = 255 << 24;
		for (int i = 0; i < img.pixels.length; i++) {
			int src = Math.abs(img.pixels[i]);
			int targ = Math.abs(bakImg.pixels[i]);
			int diff = maxColorDiff(src, targ);
			if (isMungeInverted) {
				if (diff < mungeThreshold) {
					img.pixels[i] = snapImg.pixels[i] | alpha;
				}
				
			}
			else {
				if (diff > mungeThreshold) {
					img.pixels[i] = snapImg.pixels[i] | alpha;
				}
			}
		}
		println("munged -----");
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	

	/**
	 * degrades the image by saving it as a low quality JPEG and loading the saved image
	 */
	public void degrade() {
		try {
			backup();
			println("degrading");
			degradeImage(img, degradeQuality);
			if (isFitToScreen) fitPixels(true, false);
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Quantizes colors in image to a user-specified value between 2 and 255
	 */
	public void reduceColors() {
		BufferedImage im = (BufferedImage) img.getNative();
		if (null == quant) {
			quant = new ImageColorQuantizer(colorQuantize);
		}
		else {
			quant.setColorCount(colorQuantize);
		}
		quant.filter(im, null);
		int[] px = quant.pixels;
		if (px.length != img.pixels.length) {
			println("---- pixel arrays are not equal (method reduceColors)");
			return;
		}
		backup();
		img.loadPixels();
		int alpha = 255 << 24;
		for (int i = 0; i < px.length; i++) {
			// provide the alpha channel, otherwise the image will vanish
			img.pixels[i] = px[i] | alpha;
		}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	
	/**
	 * implements a basic 3x3 denoise (median) filter
	 * TODO provide generalized filter for any edge dimension, tuned to individual color channels
	 */
	public void denoise() {
		int boxW = 3;
        int medianPos = 4;
        backup();
		PImage imgCopy = img.get();
		int w = img.width;
		int h = img.height;
		int[] pix = new int[boxW * boxW];
		img.loadPixels();
		for (int v = 1; v < h - 1; v++) {
			for (int u = 1; u < w - 1; u++) {
				int k = 0;
                for (int j = -1; j <= 1; j++) {
                    for (int i = -1; i <= 1; i++) {
                        pix[k] = imgCopy.get(u + i, v + j);
                        k++;
                    }
                }
                Arrays.sort(pix);
                img.set(u, v, pix[medianPos]);
			}
		}
		// prepare array for edges
		pix = new int[(boxW - 1) * boxW];
		// left edge
		for (int v = 1; v < h - 1; v++) {
			int u = 0;
			int k = 0;
			for (int j = -1; j <= 1; j++) {
				for (int i = 0; i <= 1; i++) {
					pix[k] = imgCopy.get(u + i, v + j);
					k++;
				}
			}
			Arrays.sort(pix);
			img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
		}
		// right edge
		for (int v = 1; v < h - 1; v++) {
			int u = w - 1;
			int k = 0;
			for (int j = -1; j <= 1; j++) {
				for (int i = 0; i <= 1; i++) {
					pix[k] = imgCopy.get(u - i, v + j);
					k++;
				}
			}
			Arrays.sort(pix);
			img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
		}
		// top edge
		for (int u = 1; u < w - 1; u++) {
			int v = 0;
			int k = 0;
			for (int j = 0; j <= 1; j++) {
				for (int i = -1; i <= 1; i++) {
					pix[k] = imgCopy.get(u + i, v + j);
					k++;
				}
			}
			Arrays.sort(pix);
			img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
		}
		// bottom edge 
		for (int u = 1; u < w - 1; u++) {
			int v = h - 1;
			int k = 0;
			for (int j = 0; j <= 1; j++) {
				for (int i = -1; i <= 1; i++) {
					pix[k] = imgCopy.get(u + i, v - j);
					k++;
				}
			}
			Arrays.sort(pix);
			img.set(u, v, GlitchSort.meanColor(pix[2], pix[3]));
		}
		// prepare array for corners
		pix = new int[(boxW - 1) * (boxW - 1)];
		// do the corners
		pix[0] = imgCopy.get(0, 0);
		pix[1] = imgCopy.get(0, 1);
		pix[2] = imgCopy.get(1, 0);
		pix[3] = imgCopy.get(1, 1);
		Arrays.sort(pix);
		img.set(0, 0, GlitchSort.meanColor(pix[1], pix[2]));
		pix[0] = imgCopy.get(w - 1, 0);
		pix[1] = imgCopy.get(w - 1, 1);
		pix[2] = imgCopy.get(w - 2, 0);
		pix[3] = imgCopy.get(w - 2, 1);
		Arrays.sort(pix);
		img.set(w - 1, 0, GlitchSort.meanColor(pix[1], pix[2]));
		pix[0] = imgCopy.get(0, h - 1);
		pix[1] = imgCopy.get(0, h - 2);
		pix[2] = imgCopy.get(1, h - 1);
		pix[3] = imgCopy.get(1, h - 2);
		Arrays.sort(pix);
		img.set(0, h - 1, GlitchSort.meanColor(pix[1], pix[2]));
		pix[0] = imgCopy.get(w - 1, h - 1);
		pix[1] = imgCopy.get(w - 1, h - 2);
		pix[2] = imgCopy.get(w - 2, h - 1);
		pix[3] = imgCopy.get(w - 2, h - 1);
		Arrays.sort(pix);
		img.set(w - 1, h - 1, GlitchSort.meanColor(pix[1], pix[2]));
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	
	
	/**
	 * Shifts selected RGB color channel one pixel left.
	 */
	public void shiftLeft() {
		backup();
		img.loadPixels();
		int c1, c2;
		// I've unwound the loop so as to check the channel to shift only once
		if (isShiftR) {
			for (int i = 0; i < rowNums.length; i++) {
				int l = i * img.width;
				int r = l + img.width - 1;
				int temp = img.pixels[l];
				for (int u = l + 1; u <= r; u++) {
					c1 = img.pixels[u];
					c2 = img.pixels[u - 1];
					img.pixels[u - 1] = 255 << 24 | ((c1 >> 16) & 0xFF) << 16 | ((c2 >> 8) & 0xFF) << 8 | c2 & 0xFF;
				}
				c2 = img.pixels[r];
				img.pixels[r] = 255 << 24 | ((temp >> 16) & 0xFF) << 16 | ((c2 >> 8) & 0xFF) << 8 | c2 & 0xFF;
			}
		}
		else if (isShiftG) {
			for (int i = 0; i < rowNums.length; i++) {
				int l = i * img.width;
				int r = l + img.width - 1;
				int temp = img.pixels[l];
				for (int u = l + 1; u <= r; u++) {
					c1 = img.pixels[u];
					c2 = img.pixels[u - 1];
					img.pixels[u - 1] = 255 << 24 | ((c2 >> 16) & 0xFF) << 16 | ((c1 >> 8) & 0xFF) << 8 | c2 & 0xFF;
				}
				c2 = img.pixels[r];
				img.pixels[r] = 255 << 24 | ((c2 >> 16) & 0xFF) << 16 | ((temp >> 8) & 0xFF) << 8 | c2 & 0xFF;
			}
		}
		else if (isShiftB) {
			for (int i = 0; i < rowNums.length; i++) {
				int l = i * img.width;
				int r = l + img.width - 1;
				int temp = img.pixels[l];
				for (int u = l + 1; u <= r; u++) {
					c1 = img.pixels[u];
					c2 = img.pixels[u - 1];
					img.pixels[u - 1] = 255 << 24 | ((c2 >> 16) & 0xFF) << 16 | ((c2 >> 8) & 0xFF) << 8 | c1 & 0xFF;
				}
				c2 = img.pixels[r];
				img.pixels[r] = 255 << 24 | ((c2 >> 16) & 0xFF) << 16 | ((c2 >> 8) & 0xFF) << 8 | temp & 0xFF;
			}
		}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	
	/**
	 * Shifts selected RGB channel one pixel right.
	 */
	public void shiftRight() {
		backup();
		img.loadPixels();
		int c1, c2;
		// I've unwound the loop so as to check the channel o shift only once
		if (isShiftR) {
			for (int i = 0; i < rowNums.length; i++) {
				int l = i * img.width;
				int r = l + img.width - 1;
				int temp = img.pixels[r];
				for (int u = r - 1; u >= l; u--) {
					c1 = img.pixels[u];
					c2 = img.pixels[u + 1];
					img.pixels[u + 1] = 255 << 24 | ((c1 >> 16) & 0xFF) << 16 | ((c2 >> 8) & 0xFF) << 8 | c2 & 0xFF;
				}
				c2 = img.pixels[l];
				img.pixels[l] = 255 << 24 | ((temp >> 16) & 0xFF) << 16 | ((c2 >> 8) & 0xFF) << 8 | c2 & 0xFF;
			}
		}
		else if (isShiftG) {
			for (int i = 0; i < rowNums.length; i++) {
				int l = i * img.width;
				int r = l + img.width - 1;
				int temp = img.pixels[r];
				for (int u = r - 1; u >= l; u--) {
					c1 = img.pixels[u];
					c2 = img.pixels[u + 1];
					img.pixels[u + 1] = 255 << 24 | ((c2 >> 16) & 0xFF) << 16 | ((c1 >> 8) & 0xFF) << 8 | c2 & 0xFF;
				}
				c2 = img.pixels[l];
				img.pixels[l] = 255 << 24 | ((c2 >> 16) & 0xFF) << 16 | ((temp >> 8) & 0xFF) << 8 | c2 & 0xFF;
			}
		}
		else if (isShiftB) {
			for (int i = 0; i < rowNums.length; i++) {
				int l = i * img.width;
				int r = l + img.width - 1;
				int temp = img.pixels[r];
				for (int u = r - 1; u >= l; u--) {
					c1 = img.pixels[u];
					c2 = img.pixels[u + 1];
					img.pixels[u + 1] = 255 << 24 | ((c2 >> 16) & 0xFF) << 16 | ((c2 >> 8) & 0xFF) << 8 | c1 & 0xFF;
				}
				c2 = img.pixels[l];
				img.pixels[l] = 255 << 24 | ((c2 >> 16) & 0xFF) << 16 | ((c2 >> 8) & 0xFF) << 8 | temp & 0xFF;
			}
		}
		img.updatePixels();
		fitPixels(isFitToScreen, false);
	}
	
	/**
	 * TODO fit full image into frame, with no hidden pixels. Works when fitToScreen is true, fails in some 
	 * extreme instances when fitToScreen is false. 
	 * This method is a bottleneck for all screen display--keep it so. 
	 * Fits images that are too big for the screen to the screen, or displays as much of a large image 
	 * as fits the screen if every pixel is displayed. There is still some goofiness in getting the whole
	 * image to display--bottom edge gets hidden by the window. It would be good to have a scrolling window.
	 * 
	 * @param fitToScreen   true if image should be fit to screen, false if every pixel should displayed
	 * @param isFromControlPanel   true if the control panel dispatched the call, false otherwise
	 */
	public void fitPixels(boolean fitToScreen, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (fitToScreen) ((CheckBox) controlP5.getGroup("fitPixels")).activate(0);
			else ((CheckBox) controlP5.getGroup("fitPixels")).deactivate(0);
		}
		else {
			if (fitToScreen) {
				fitImg = createImage(img.width, img.height, ARGB);
				scaledWidth = fitImg.width;
				scaledHeight = fitImg.height;
				fitImg = img.get();
				// calculate proportions of window and image, 
				// be sure to convert ints to floats to get the math right
				// ratio of the window height to the window width
				float windowRatio = maxWindowHeight/(float)maxWindowWidth;
				// ratio of the image height to the image width
				float imageRatio = fitImg.height/(float)fitImg.width;
				if (verbose) {
					println("maxWindowWidth "+ maxWindowWidth +", maxWindowHeight "+ maxWindowHeight +", screen ratio "+ windowRatio);
					println("image width "+ fitImg.width +", image height "+ fitImg.height +", image ratio "+ imageRatio);
				}
				if (imageRatio > windowRatio) {
					// image is proportionally taller than the display window, 
					// so scale image height to fit the window height
					scaledHeight = maxWindowHeight;
					// and scale image width by window height divided by image height
					scaledWidth = Math.round(fitImg.width * (maxWindowHeight / (float)fitImg.height));
				}
				else {
					// image is proportionally equal to or wider than the display window, 
					// so scale image width to fit the windwo width
					scaledWidth = maxWindowWidth;
					// and scale image height by window width divided by image width
					scaledHeight = Math.round(fitImg.height * (maxWindowWidth / (float)fitImg.width));
				}
				fitImg.resize(scaledWidth, scaledHeight);
				if (null != myFrame) myFrame.setSize(scaledWidth, scaledHeight + 48);
			}
			else {
				scaledWidth = img.width;
				scaledHeight = img.height;
				if (null != myFrame) {
					frameWidth = scaledWidth <= maxWindowWidth ? scaledWidth : maxWindowWidth;
					frameHeight = scaledHeight <= maxWindowHeight ? scaledHeight : maxWindowHeight;
					myFrame.setSize(frameWidth, frameHeight + 38);
				}
			}
			// println("scaledWidth = "+ scaledWidth +", scaledHeight = "+ scaledHeight +", frameWidth = "+ frameWidth +", frameHeight = "+ frameHeight);
			isFitToScreen = fitToScreen;
		}
	}
	
    /********************************************/
    /*                                          */
    /*      >>> CONTROL PANEL COMMANDS <<<      */
    /*                                          */
    /********************************************/
	

	/**
	 * Sets glitchSteps.
	 * @param val   the new value for glitchSteps
	 */
	public void setGlitchSteps(float val) {
		val = val < 1 ? 1 : (val > 100 ? 100 : val);
		if (val == glitchSteps) return;
		glitchSteps = val;
		glitchSteps = (int)  Math.floor(glitchSteps);
		((Textlabel)controlP5.getController("glitchStepsLabel")).setValue("Steps = "+ (int)glitchSteps);
		if (null != ranger) {
			ranger.setNumberOfIntervals((int)glitchSteps);
			println("range intervals set to "+ (int) glitchSteps);
		}
		if (verbose) println("glitchSteps = "+ glitchSteps);
	}
	
	/**
	 * Sets glitchSteps.
	 * @param val   the new value for glitchSteps
	 */
	public void incrementGlitchSteps(boolean up) {
		// a workaround to permit us to call setGlitchSteps as a bottleneck method
		int steps = (int) glitchSteps;
		if (up) steps++;
		else steps--;
		setGlitchSteps(steps);
		controlP5.getController("setGlitchSteps").setBroadcast(false);
		controlP5.getController("setGlitchSteps").setValue(glitchSteps);
		controlP5.getController("setGlitchSteps").setBroadcast(true);
	}
	
	/**
	 * Set the value of isGlitchCycle.
	 * @param isCycle   the value to set isCycleGlitch
	 * @param isFromControlPanel   true if called from the control panel, false otherwise
	 */
	public void setCycle(boolean isCycle, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (isCycle) ((CheckBox) controlP5.getGroup("Glitchmode")).activate(0);
			else ((CheckBox) controlP5.getGroup("Glitchmode")).deactivate(0);
		}
		else {
			isCycleGlitch = isCycle;
			if (null != rowNums) resetRowNums();
			// if isCycleGlitch was just set to true, reset ranger's index to 0
			if (isCycleGlitch && null != ranger) {
				ranger.resetCurrentIndex();
				println("range index reset to 0");
			}
			if (verbose) println("isCycleGlitch = "+ isCycleGlitch);
		}
	}
	
	/**
	 * Sets mungeThreshold
	 * @param val   the desired JPEG quality setting (* 100).
	 */
	public void setMungeThreshold(float val) {
		if ((int) val == mungeThreshold) return;
		mungeThreshold = (int) val;
		if (verbose) println("degrade quality = "+ degradeQuality);
	}

	
	/**
	 * Toggles value of isMungeInverted, changes how difference mask operates in munge operation.
	 * @param invert
	 * @param isFromControlPanel
	 */
	public void invertMunge(boolean invert, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (invert) ((CheckBox) controlP5.getGroup("invertMunge")).activate(0);
			else ((CheckBox) controlP5.getGroup("invertMunge")).deactivate(0);
		}
		else {
			isMungeInverted = invert;
			println("isMungeInverted = "+ isMungeInverted);		
		}
	}
	
	/**
	 * Sets degradeQuality
	 * @param val   the desired JPEG quality setting (* 100).
	 */
	public void setQuality(float val) {
		if (val == degradeQuality * 100) return;
		degradeQuality = val * 0.01f;
		println("degrade quality = "+ this.twoPlaces.format(degradeQuality * 100));
	}
	
	/**
	 * Increments or decrements and sets degradeQuality.
	 * @param up   true if increment, false if decrement
	 */
	public void incrementDegradeQuality(boolean up) {
		// a workaround to permit us to call setQuality as a bottleneck method
		float q = (degradeQuality * 100);
		if (up) q++; 
		else q--;
		setQuality(constrain(q, 0, 100));
		controlP5.getController("setQuality").setBroadcast(false);
		controlP5.getController("setQuality").setValue(degradeQuality * 100);
		controlP5.getController("setQuality").setBroadcast(true);
	}
	

	
	/**
	 * TODO
	 * Sets the sorting method (QUICK, SHELL, BUBBLE, INSERT) used by sortTool.
	 * @param type   the type of sorting method to use
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setSorter(SorterType type, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			((RadioButton) controlP5.getGroup("setSorter")).activate(type.name());
		}
		else {
			sortTool.setSorter(type);
			breakPoint = sortTool.sorter.getBreakPoint();
			controlP5.getController("setBreakpoint").setBroadcast(false);
			controlP5.getController("setBreakpoint").setValue(breakPoint);
			((Textlabel)controlP5.getController("breakpointLabel")).setValue("Breakpoint: " + sortTool.sorter.getSorterType().toString());
			controlP5.getController("setBreakpoint").setBroadcast(true);
			println(type.name() +" sorter loaded");
		}
		if (type == SorterType.BUBBLE || type == SorterType.INSERT) {
			// bubble and insert sorts are extremely slow: it only make sense to use them if they break (glitch)
			// so we set the break checkbox to true and lock it
			println("bubble or insert sort: break set to true");
			setRandomBreak(true, false);
			((CheckBox) controlP5.getGroup("Sorting")).getItem(1).setLock(true);
		}
		else {
			// unlock the break checkbox
			((CheckBox) controlP5.getGroup("Sorting")).getItem(1).setLock(false);
		}
	}

	/**
	 * Sets the order of components used to sort pixels.
	 * @param index   index number of CompOrder values 
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setCompOrder(int index, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			((RadioButton) controlP5.getGroup("setCompOrder")).activate(index);
		}
		else {
			compOrderIndex = index;
			compOrder = CompOrder.values()[compOrderIndex];
			println("Color component order set to "+ compOrder.name());
		}
	}
	
	/**
	 * @param val   true if sorting should be in ascending order, false otherwise 
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setAscending(boolean val, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (val) ((CheckBox) controlP5.getGroup("Sorting")).activate("Ascending");
			else ((CheckBox) controlP5.getGroup("Sorting")).deactivate("Ascending");
		}
		else {
			if (isAscendingSort == val) return;
			isAscendingSort = val;
			println("Ascending sort order is "+ isAscendingSort);
		}
	}
		
	/**
	 * @param val   true if random breaks in sorting ("glitches") are desired, false otherwise.
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setRandomBreak(boolean val, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (val) ((CheckBox) controlP5.getGroup("Sorting")).activate("Break");
			else ((CheckBox) controlP5.getGroup("Sorting")).deactivate("Break");
		}
		else {
			if (randomBreak == val) return;
			randomBreak = val;
			sortTool.setRandomBreak(randomBreak);
			println("randomBreak is "+ randomBreak);
		}
	}

	/**
	 * @param val   true if color channels should be swapped when sorting (more glitching). 
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setIsSwapChannels(boolean val, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			if (val) ((CheckBox) controlP5.getGroup("Sorting")).activate("Swap");
			else ((CheckBox) controlP5.getGroup("Sorting")).deactivate("Swap");
		}
		else {
			if (isSwapChannels == val) return;
			isSwapChannels = val;
			println("Swap color channels is "+ isSwapChannels);
		}
	}
	
	/**
	 * @param newSwap   the swap value to set, determinse which channels are swapped.
	 * @param isFromControlPanel   true if call is from a control panel interaction, false otherwise
	 */
	public void setSwap(SwapChannel newSwap, boolean isFromControlPanel) {
		if (swap == newSwap) return;
		if (!isFromControlPanel) {
			RadioButton rb1 = (RadioButton)controlP5.getGroup("setSourceChannel");
			RadioButton rb2 = (RadioButton)controlP5.getGroup("setTargetChannel");
			switch (newSwap) {
			case RR: {
				rb1.activate(0);
				rb2.activate(0);
				break;
			}
			case RG: {
				rb1.activate(0);
				rb2.activate(1);
				break;
			}
			case RB: {
				rb1.activate(0);
				rb2.activate(2);
				break;
			}
			case GR: {
				rb1.activate(1);
				rb2.activate(0);
				break;
			}
			case GG: {
				rb1.activate(1);
				rb2.activate(1);
				break;
			}
			case GB: {
				rb1.activate(1);
				rb2.activate(2);
				break;
			}
			case BR: {
				rb1.activate(2);
				rb2.activate(0);
				break;
			}
			case BG: {
				rb1.activate(2);
				rb2.activate(1);
				break;
			}
			case BB: {
				rb1.activate(2);
				rb2.activate(2);
				break;
			}
			}
		}
		else {
			swap = newSwap;
			println("swap is "+ swap.name());
		}
	}
	
	public void setZigzagStyle(ZigzagStyle style, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			((RadioButton) controlP5.getGroup("setZigzagStyle")).activate(style.ordinal());
		}
		else {
			zigzagStyle = style;
			println("-- zizagStyle = "+ style.name());
		}
	}	
	
	/**
	 * adjusts control panel text to reflect updated quantization value
	 * @param val   the current quantization value
	 */
	public void setColorQuantize(float val) {
		if (val == colorQuantize) return;
		colorQuantize = (int) val;
		((Textlabel)controlP5.getController("colorQuantizeLabel")).setValue("Colors = "+ colorQuantize);
		// if (verbose) 
		println("colorQuantize = "+ colorQuantize);
	}
	
	/**
	 * Sets colorQuantize.
	 * @param val   the new value for glitchSteps
	 */
	public void incrementColorQuantize(boolean up) {
		// a workaround to permit us to call setGlitchSteps as a bottleneck method
		int val = (int) colorQuantize;
		if (up && val < 128) val++;
		else if (val > 2) val--;
		setColorQuantize(val);
		controlP5.getController("setColorQuantize").setBroadcast(false);
		controlP5.getController("setColorQuantize").setValue(colorQuantize);
		controlP5.getController("setColorQuantize").setBroadcast(true);
	}
	
	/**
	 * Sets the values of equalizer-controlled FFT settings.
	 * @param isBrightness   true if brightness channel is affect by FFT, false otherwise
	 * @param isHue          true if hue channel is affect by FFT, false otherwise
	 * @param isSaturation   true if saturation channel is affect by FFT, false otherwise
	 * @param isRed          true if red channel is affect by FFT, false otherwise
	 * @param isGreen        true if green channel is affect by FFT, false otherwise
	 * @param isBlue         true if blue channel is affect by FFT, false otherwise
	 * @param isFromControlPanel   true if called from the control panel, false otherwise
	 */
	public void setEqChan(boolean isBrightness, boolean isHue, boolean isSaturation,
			boolean isRed, boolean isGreen, boolean isBlue, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			// println("setting equalizer HSB/RGB");
			if (isBrightness) ((CheckBox) controlP5.getGroup("ChanEq")).activate(0);
			else ((CheckBox) controlP5.getGroup("ChanEq")).deactivate(0);
			if (isHue) ((CheckBox) controlP5.getGroup("ChanEq")).activate(1);
			else ((CheckBox) controlP5.getGroup("ChanEq")).deactivate(1);
			if (isSaturation) ((CheckBox) controlP5.getGroup("ChanEq")).activate(2);
			else ((CheckBox) controlP5.getGroup("ChanEq")).deactivate(2);
			if (isRed) ((CheckBox) controlP5.getGroup("ChanEq")).activate(3);
			else ((CheckBox) controlP5.getGroup("ChanEq")).deactivate(3);
			if (isGreen) ((CheckBox) controlP5.getGroup("ChanEq")).activate(4);
			else ((CheckBox) controlP5.getGroup("ChanEq")).deactivate(4);
			if (isBlue) ((CheckBox) controlP5.getGroup("ChanEq")).activate(5);
			else ((CheckBox) controlP5.getGroup("ChanEq")).deactivate(5);
		}
		else {
			isEqGlitchBrightness = isBrightness;
			isEqGlitchHue = isHue;
			isEqGlitchSaturation = isSaturation;
			isEqGlitchRed = isRed;
			isEqGlitchGreen = isGreen;
			isEqGlitchBlue = isBlue;
			if (verbose) 
			{
				println("Equalizer FFT: ");
				print("  Brightness = "+ isEqGlitchBrightness);
				print(", Hue = "+ isEqGlitchHue);
				print(", Saturation = "+ isEqGlitchSaturation);
				print(", Red = "+ isEqGlitchRed);
				print(", Green = "+ isEqGlitchGreen);
				println(", Blue = "+ isEqGlitchBlue);
			}
		}
	}
	
	/**
	 * Sets the values of statistically controlled FFT settings.
	 * @param isBrightness   true if brightness channel is affect by FFT, false otherwise
	 * @param isHue          true if hue channel is affect by FFT, false otherwise
	 * @param isSaturation   true if saturation channel is affect by FFT, false otherwise
	 * @param isRed          true if red channel is affect by FFT, false otherwise
	 * @param isGreen        true if green channel is affect by FFT, false otherwise
	 * @param isBlue         true if blue channel is affect by FFT, false otherwise
	 * @param isFromControlPanel   true if called from the control panel, false otherwise
	 */
	public void setStatChan(boolean isBrightness, boolean isHue, boolean isSaturation,
			boolean isRed, boolean isGreen, boolean isBlue, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			// println("setting statistical HSB/RGB");
			if (isBrightness) ((CheckBox) controlP5.getGroup("ChanStat")).activate(0);
			else ((CheckBox) controlP5.getGroup("ChanStat")).deactivate(0);
			if (isHue) ((CheckBox) controlP5.getGroup("ChanStat")).activate(1);
			else ((CheckBox) controlP5.getGroup("ChanStat")).deactivate(1);
			if (isSaturation) ((CheckBox) controlP5.getGroup("ChanStat")).activate(2);
			else ((CheckBox) controlP5.getGroup("ChanStat")).deactivate(2);
			if (isRed) ((CheckBox) controlP5.getGroup("ChanStat")).activate(3);
			else ((CheckBox) controlP5.getGroup("ChanStat")).deactivate(3);
			if (isGreen) ((CheckBox) controlP5.getGroup("ChanStat")).activate(4);
			else ((CheckBox) controlP5.getGroup("ChanStat")).deactivate(4);
			if (isBlue) ((CheckBox) controlP5.getGroup("ChanStat")).activate(5);
			else ((CheckBox) controlP5.getGroup("ChanStat")).deactivate(5);
		}
		else {
			isStatGlitchBrightness = isBrightness;
			isStatGlitchHue = isHue;
			isStatGlitchSaturation = isSaturation;
			isStatGlitchRed = isRed;
			isStatGlitchGreen = isGreen;
			isStatGlitchBlue = isBlue;
			if (verbose) 
			{
				println("Statistical FFT: ");
				print("  Brightness = "+ isStatGlitchBrightness);
				print(", Hue = "+ isStatGlitchHue);
				print(", Saturation = "+ isStatGlitchSaturation);
				print(", Red = "+ isStatGlitchRed);
				print(", Green = "+ isStatGlitchGreen);
				println(", Blue = "+ isStatGlitchBlue);
			}
		}
	}


	/**
	 * Toggles low frequency cut setting in statistical FFT control.
	 * @param isCut
	 * @param isFromControlPanel
	 */
	public void setLowFrequencyCut(boolean isCut, boolean isFromControlPanel) {
		if (!isFromControlPanel) {
			// println("setting statistical HSB/RGB");
			if (isCut) ((CheckBox) controlP5.getGroup("LowFreqCut")).activate(0);
			else ((CheckBox) controlP5.getGroup("LowFreqCut")).deactivate(0);
		}
		else {
			isLowFrequencyCut = isCut;
			println("isLowFrequencyCut = "+ isLowFrequencyCut);
		}
	}

	/**
	 * Sets fftBlockWidth. 
	 * @param val   the new value for eqGain
	 */
	public void setFFTBlockWidth(float val) {
		val = val < 3 ? 3 : (val > 9 ? 9 : val);
		int temp = (int) Math.pow(2, (int) val);
		if (temp == fftBlockWidth) return;
		resetFFT(temp);
	}
	
	/**
	 * Sets value of leftBound used in statistical FFT interface
	 * @param newLeftBound
	 */
	public void setLeftBound(float newLeftBound) {
		if (newLeftBound == leftBound) return;
		leftBound = newLeftBound;
	}

	/**
	 * Sets value of rightBound used in statistical FFT interface
	 * @param newRightBound
	 */
	public void setRightBound(float newRightBound) {
		if (newRightBound == rightBound) return;
		rightBound = newRightBound;
	}

	/**
	 * Sets value of boost used in statistical FFT interface
	 * @param newBoost
	 */
	public void setBoost(float newBoost) {
		if (newBoost == boost) return;
		boost = newBoost;
	}
	
	/**
	 * Sets value of cut used in statistical FFT interface
	 * @param newCut
	 */
	public void setCut(float newCut) {
		if (newCut == cut) return;
		cut = newCut;
	}
	

	
	/**
	 * Bottleneck that catches events propagated by control panel, used particularly for radio buttons and checkboxes.
	 * @param evt   the event from the control panel
	 */
	public void controlEvent(ControlEvent evt) {
		if (evt.isGroup()) {
			if ("setCompOrder".equals(evt.getName())) {
				setCompOrder((int) evt.getGroup().getValue(), true);
			}
			else if ("setSorter".equals((evt.getName()))) {
				SorterType type = SorterType.values()[(int) evt.getGroup().getValue()];
				setSorter(type, true);
			}
			else if ("Sorting".equals(evt.getName())) {
				int n = (int)(evt.getGroup().getArrayValue()[0]);
				setAscending(n == 1, true);
				n = (int)(evt.getGroup().getArrayValue()[1]);
				setRandomBreak(n == 1, true);
				n = (int)(evt.getGroup().getArrayValue()[2]);
				setIsSwapChannels(n == 1, true);
			}
			else if ("setSourceChannel".equals(evt.getName())) {
				int n = (int)(evt.getGroup().getValue());
				RadioButton rb = (RadioButton)controlP5.getGroup("setTargetChannel");
				int m = (int) rb.getValue();
				String str = ChannelNames.values()[n].toString() + ChannelNames.values()[m].toString();
				SwapChannel sc = SwapChannel.valueOf(str);
				setSwap(sc, true);
			}
			else if ("setTargetChannel".equals(evt.getName())) {
				RadioButton rb = (RadioButton)controlP5.getGroup("setSourceChannel");
				int n = (int) rb.getValue();
				int m = (int)(evt.getGroup().getValue());
				String str = ChannelNames.values()[n].toString() + ChannelNames.values()[m].toString();
				SwapChannel sc = SwapChannel.valueOf(str);
				setSwap(sc, true);
			}
			else if ("fitPixels".equals(evt.getName())) {
				int n = (int)(evt.getGroup().getArrayValue()[0]);
				fitPixels(n == 1, true);
			}
			else if (("setZigzagStyle").equals(evt.getName())) {
				ZigzagStyle z = ZigzagStyle.values()[(int) evt.getGroup().getValue()];
				setZigzagStyle(z, true);				
			}
			else if ("invertMunge".equals(evt.getName())) {
				int n = (int)(evt.getGroup().getArrayValue()[0]);
				invertMunge(n == 1, true);
			}
			else if ("Glitchmode".equals(evt.getName())) {
				int n = (int)(evt.getGroup().getArrayValue()[0]);
				setCycle(n == 1, true);
			}
			else if ("ChanEq".equals(evt.getName())) {
				if (verbose) println("ChanEq event");
				int b = (int)(evt.getGroup().getArrayValue()[0]);
				int h = (int)(evt.getGroup().getArrayValue()[1]);
				int s = (int)(evt.getGroup().getArrayValue()[2]);
				int r = (int)(evt.getGroup().getArrayValue()[3]);
				int g = (int)(evt.getGroup().getArrayValue()[4]);
				int bl = (int)(evt.getGroup().getArrayValue()[5]);
				setEqChan(b == 1, h == 1, s == 1, r == 1, g == 1, bl == 1, true);
			}
			else if ("ChanStat".equals(evt.getName())) {
				if (verbose) println("ChanStat event");
				int b = (int)(evt.getGroup().getArrayValue()[0]);
				int h = (int)(evt.getGroup().getArrayValue()[1]);
				int s = (int)(evt.getGroup().getArrayValue()[2]);
				int r = (int)(evt.getGroup().getArrayValue()[3]);
				int g = (int)(evt.getGroup().getArrayValue()[4]);
				int bl = (int)(evt.getGroup().getArrayValue()[5]);
				setStatChan(b == 1, h == 1, s == 1, r == 1, g == 1, bl == 1, true);
			}
			else if ("Shift".equals(evt.getName())) {
				isShiftR = ((int)(evt.getGroup().getArrayValue()[0])) == 1;
				isShiftG = ((int)(evt.getGroup().getArrayValue()[1])) == 1;
				isShiftB = ((int)(evt.getGroup().getArrayValue()[2])) == 1;
			}
			else if ("LowFreqCut".equals(evt.getName())) {
				if (verbose) println("LowFreqCut event");
				int cut = (int)(evt.getGroup().getArrayValue()[0]);
				setLowFrequencyCut(cut == 1, true);
			}
			if (verbose) {
				print("got an event from "+ evt.getGroup().getName() +"\t");
				for(int i=0; i < evt.getGroup().getArrayValue().length; i++) {
					print((int)(evt.getGroup().getArrayValue()[i]));
				}
				println("\t "+ evt.getGroup().getValue());
			}
		}
		else if (evt.isController()) {
			String name = evt.getController().getName();
			if (name.substring(0, 3).equals(sliderIdentifier)) {
				Slider con = (Slider) evt.getController();
				int bin = con.getId();
				float val = con.getValue();
				if (bin >= 0 && bin < eq.length) {
					if (val < 0) eq[bin] = val + 1;
					else eq[bin] = lerp(0, eqScale, val) + 1;
					String legend = "band "+ bin +" = "+ twoPlaces.format(eq[bin]);
					if (null != binTotals && bin < binTotals.length) {
						// TODO : duplicated code here, put it in a function
						legend += ", bin avg = "+ twoPlaces.format(binTotals[bin]);
						IntRange ir = bandList.get(bin);
						legend += ", cf = "+ twoPlaces.format((fft.indexToFreq(ir.upper) + fft.indexToFreq(ir.lower)) * 0.5f);
					}
					((Textlabel)controlP5.getController("eqLabel")).setValue(legend);
				}
			}
			
		}
	}
	
		
    /********************************************/
    /*                                          */
    /*              >>> UTILITY <<<             */
    /*                                          */
    /********************************************/
	
	/**
	 * Shuffles an array of integers into random order.
	 * Implements Richard Durstenfeld's version of the Fisher-Yates algorithm, popularized by Donald Knuth.
	 * see http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
	 * @param intArray an array of <code>int</code>s, changed on exit
	 */
	public void shuffle(int[] intArray) {
		for (int lastPlace = intArray.length - 1; lastPlace > 0; lastPlace--) {
			// Choose a random location from 0..lastPlace
			int randLoc = (int) (random(lastPlace + 1));
			// Swap items in locations randLoc and lastPlace
			int temp = intArray[randLoc];
			intArray[randLoc] = intArray[lastPlace];
			intArray[lastPlace] = temp;
		}
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
	/**
	 * Returns the largest difference between the components of two colors. 
	 * If the value returned is 0, colors are identical.
	 * @param color1
	 * @param color2
	 * @return
	 */
	public static int maxColorDiff(int color1, int color2) {
		int rDiff = Math.abs(((color1 >> 16) & 0xFF) - ((color2 >> 16) & 0xFF));
		int gDiff = Math.abs(((color1 >> 8) & 0xFF) - ((color2 >> 8) & 0xFF));
		int bDiff = Math.abs(((color1) & 0xFF) - ((color2) & 0xFF));
		return Math.max(Math.max(rDiff, gDiff), bDiff);
	}
	
	public static int meanColor(int argb1, int argb2) {
		int[] comp1 = GlitchSort.rgbComponents(argb1);
		int[] comp2 = GlitchSort.rgbComponents(argb2);
		for (int i = 0; i < comp1.length; i++) {
			comp1[i] = (int) ((comp1[i] + comp2[i]) * 0.5f);
		}
		return GlitchSort.composeColor(comp1);
	}
	

    /********************************************/
    /*                                          */
    /*             >>> FILE I/O <<<             */
    /*                                          */
    /********************************************/
	
	/**
	 * saves current image to a uniquely-named file
	 */
	public void saveFile(boolean isCopy) {
	    String shortName = originalFile.getName();
	    String[] parts = shortName.split("\\.");
	    // String attributes = compOrder.name();
	    // if (isSwapChannels) attributes += "_"+ swap.name();
	    // String fName = parts[0] +"_"+ timestamp +"_"+ attributes +".png";
	    String fName = parts[0] +"_"+ timestamp +"_"+ fileCount +".png";
	    fileCount++;
	    if (!isCopy) println("saving to "+ fName);
	    else  println("saving copy to "+ fName);
	    img.save(fName);
	    if (!isCopy) {
	    	// Eclipse and Processing have different default paths
	    	// println("sketchPath = "+ sketchPath);
	        displayFile = new File(sketchPath +"/"+ fName);
	    }
	}

	/**
	 * @return   true if a file reference was successfully returned from the file dialogue, false otherwise
	 */
	public void chooseFile() {
		selectInput("Choose an image file.", "displayFileSelected");
	}
	
	public void displayFileSelected(File selectedFile) {
		File oldFile = displayFile;
		if (null != selectedFile && oldFile != selectedFile) {
			noLoop();
			displayFile = selectedFile;
			originalFile = selectedFile;
			loadFile();
			if (isFitToScreen) fitPixels(true, false);
			loop();
		}
		else {
			println("No file was selected");
		}
	}
	
	
	/**
	 * loads a file into variable img.
	 */
	public void loadFile() {
		println("\nselected file "+ displayFile.getAbsolutePath());
		img = loadImage(displayFile.getAbsolutePath());
		transX = transY = 0;
		fitPixels(isFitToScreen, false);
		println("image width "+ img.width +", image height "+ img.height);
		resetRowNums(img.height);
		if (null == ranger) {
			ranger = new RangeManager(img.height, (int) glitchSteps);
		}
		else {
			ranger.setRange(img.height);
		}
		analyzeEq(false);
	}

	/**
	 * loads original file into variable img.
	 */
	public void loadOriginalFile() {
		println("\noriginal file "+ originalFile.getAbsolutePath());
		displayFile = originalFile;
		img = loadImage(displayFile.getAbsolutePath());
		transX = transY = 0;
		fitPixels(isFitToScreen, false);
		println("image width "+ img.width +", image height "+ img.height);
		resetRowNums(img.height);
		if (null == ranger) {
			ranger = new RangeManager(img.height, (int) glitchSteps);
		}
		else {
			ranger.setRange(img.height);
		}
		analyzeEq(false);
	}

	
    /**
     * @param image          the image to degrade
     * @param quality        the desired JPEG quality
     * @throws IOException   error thrown by file i/o
     */
    public void degradeImage(PImage image, float quality) throws IOException {
    	Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
    	ImageWriter writer = (ImageWriter)iter.next();
    	ImageWriteParam iwp = writer.getDefaultWriteParam();
    	iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    	iwp.setCompressionQuality(quality);   	
    	try {
    		BufferedImage bi =  (BufferedImage) image.getNative();
    		String shortName = displayFile.getName();
    		String[] parts = shortName.split("\\.");
    		// String fName = parts[0] +"_q"+ Math.round(quality * 100) +".jpg";
    		// just save one degrade file per image
    		String fName = parts[0] +"_degrade" +".jpg";
    		File temp = new File(savePath(fName));
    		FileImageOutputStream output = new FileImageOutputStream(temp);
    		writer.setOutput(output);
    		IIOImage outImage = new IIOImage(bi, null, null);
    		writer.write(null, outImage, iwp);
    		writer.dispose();
    		PImage newImage = loadImage(temp.getAbsolutePath());
    		img = newImage;
    		println("degraded "+ fName);
    	}
    	catch (FileNotFoundException e) {
    		println("file not found error " + e);
    	}
    	catch (IOException e) {
    		println("IOException "+ e);
    	}
    }

 
    /********************************************/
    /*                                          */
    /*           >>> RANGE MANAGER <<<          */
    /*                                          */
    /********************************************/
    
    public void testRangeManager() {
    	int lower = (int)random(10, 100);
    	lower = 0;
    	int upper = lower + (int)random(50, 1000);
    	int count = (int)random(2, 13);
    	RangeManager rm = new RangeManager(lower, upper, count);
    	println(rm.toString());
    }
    
    /**
     * Mini-class for storing bounds of an integer range.
     *
     */
    class IntRange {
    	int lower;
    	int upper;
    	
    	public IntRange(int lower, int upper) {
    		this.lower = lower;
    		this.upper = upper;
    	}
    	
    	public IntRange() {
    		this(0, 0);
    	}
    			
		public String toString() {
			return "("+ lower +", "+ upper +")";
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof IntRange && (((IntRange) o).lower == this.lower) && (((IntRange) o).upper == this.upper);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			// cf. Effective Java, 2nd edition, ch. 3, item 9.
			int result = 17;
			result = 31 * result + Float.floatToIntBits(lower);
			result = 31 * result + Float.floatToIntBits(upper);
			return result;
		}
    	
    }
    
    /**
     * Mini-class for storing bounds of an integer range.
     *
     */
    class FloatRange {
    	float lower;
    	float upper;
    	
    	public FloatRange(float lower, float upper) {
    		this.lower = lower;
    		this.upper = upper;
    	}
    	
    	public FloatRange() {
    		this(0, 0);
    	}
    			
		public String toString() {
			return "("+ lower +", "+ upper +")";
		}
    	
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof FloatRange && (((FloatRange) o).lower == this.lower) && (((FloatRange) o).upper == this.upper);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			// cf. Effective Java, 2nd edition, ch. 3, item 9.
			int result = 17;
			result = 31 * result + Float.floatToIntBits(lower);
			result = 31 * result + Float.floatToIntBits(upper);
			return result;
		}

    }
    
    
    /**
     * A utility class to assist in stepping through a array divided into a specified number of equal segments.
     *
     */
    class RangeManager {
    	ArrayList<IntRange> intervals;
    	Iterator<IntRange> iter;
    	IntRange intRange;
    	int numberOfIntervals;
    	int currentIndex;
    	
    	/**
    	 * Divides an intRange of integers, from a lower bound up to but not including an upper bound,
    	 * into a given number of equal intervals. 
    	 * 
     	 * @param lower   lower index of intRange
    	 * @param upper   upper index of intRange
    	 * @param count   number of intervals in which to divide the intRange
    	 */
    	public RangeManager(int lower, int upper, int count) {
    		this.intervals = new ArrayList<IntRange>(count);
    		this.intRange = new IntRange(lower, upper);
    		this.setNumberOfIntervals(count);
    	}
    	
    	/**
    	 * Divides a intRange of integers from 0 up to but not including an upper bound
    	 * into a given number of equal intervals. The upper bound would typically be 
    	 * the length of an array.
    	 * 
    	 * @param length
    	 * @param count
    	 */
    	public RangeManager(int upper, int count) {
    		this(0, upper, count);
    	}
    	
    	public Iterator<IntRange> getIter() {
    		if (null == iter) {
    			iter = intervals.iterator();
    		}
    		return iter;
    	}
    	
    	public IntRange get(int i) {
    		return intervals.get(i);
    	}
    	public IntRange getNext() {
    		return intervals.get(currentIndex++);
    	}
    	public boolean hasNext() {
    		return currentIndex < numberOfIntervals;
    	}
 
    	public int getCurrentIndex() {
    		return currentIndex;
    	}
    	public void resetCurrentIndex() {
    		currentIndex = 0;
    	}

    	public int getUpper() {
    		return intRange.upper;
    	}  	
    	public int getLower() {
    		return intRange.lower;
    	}
    	
    	/**
		 * @return the numberOfIntervals
		 */
		public int getNumberOfIntervals() {
			return numberOfIntervals;
		}

		/**
		 * Sets the value of numberOfIntervals and creates a new series of intervals.
		 * @param numberOfIntervals the numberOfIntervals to set
		 */
		public void setNumberOfIntervals(int numberOfIntervals) {
			this.numberOfIntervals = numberOfIntervals;
			adjustIntervals();
			resetCurrentIndex();
		}
	
		public void adjustIntervals() {
			this.intervals.clear();
    		int u = 0;
    		int l = getLower();
    		float pos = l;
    		float delta = (getUpper() - l) / (float) this.numberOfIntervals;
    		for (int i = 1; i <= numberOfIntervals; i++) {
    			pos += delta;
    			u = Math.round(pos) - 1;
    			intervals.add(new IntRange(l, u));
    			l = u + 1;
    		}
		}
		
		public void setRange(int lower, int upper) {
	   		this.intRange = new IntRange(lower, upper);
			adjustIntervals();
			resetCurrentIndex();
		}
		public void setRange(int upper) {
			setRange(0, upper);
		}

		@Override
    	public String toString() {
    		StringBuffer buf = new StringBuffer();
    		Iterator<IntRange> it = this.getIter();
    		buf.append("RangeManager: " + intervals.size() +" intervals from "+ intRange.lower +" to "+ intRange.upper + "\n  ");
    		while (it.hasNext()) {
    			IntRange r = it.next();
    			buf.append(r.toString() + ", ");
    		}
    		buf.delete(buf.length() - 2, buf.length() - 1);
    		return buf.toString();
    	}
    	
    }
 
    
    /********************************************/
    /*                                          */
    /*                >>> FFT <<<               */
    /*                                          */
    /********************************************/

    /**
     * Scales a frequency by a factor.
     * 
     * @param freq
     * @param fac
     */
    public void fftScaleFreq(float freq, float fac) {
    	fft.scaleFreq(freq, fac);
    }
    
    
    
    /**
     * Scales an array of frequencies by an array of factors.
     * @param freqs
     * @param facs
     */
    public void fftScaleFreq(float[] freqs, float[] facs) {
    	for (int i = 0; i < freqs.length; i++) {
    		fft.scaleFreq(freqs[i], facs[i]);
    	}
    }
    
    /**
     * Scales a single frequency bin (index number) by a factor.
     * 
     * @param bin
     * @param fac
     */
    public void fftScaleBin(int bin, float fac) {
     	fft.scaleBand(bin, fac);
    }
    
    
    
    /**
     * Scales an array of frequency bins (index numbers) by an array of factors.
     * @param bins
     * @param facs
     */
    public void fftScaleBin(int[] bins, float[] facs) {
    	for (int i = 0; i < bins.length; i++) {
    		fft.scaleBand(bins[i], facs[i]);
    	}
    }
    
    /**
     * placeholder for future development
     */
    public void fftScaleFreqsTest() {
    	float[] freqs = {};
    	float[] facs = {};
     }
    
    /*
     * FORMANTS
     * i beet 270 2290 3010 
     * I bit 390 1990 2550
     * e bet 530 1840 2480
     * ae bat 660 1720 2410
     * a father 730 1090 2440
     * U book 440 1020 2240
     * u boot 300 870 2240
     * L but 640 1190 2390
     * r bird 490 1350 1690
     * aw bought 570 840 2410
     *   
     */
    
	/**
	 * Calculates statistical variables from frequencies in the current FFT and returns then in an array.
	 * 
	 * @param l         left bound of bin index numbers
	 * @param r         right bound of bin index numbers
	 * @param verbose   true if output to consoles is desired, false otherwise
	 * @param msg       a message to include with output
	 * @return          an array of derived values: minimum, maximum, sum, mean, median, standard deviation, skew.
	 */
	public float[] fftStat(int l, int r, boolean verbose, String msg) {
		double sum = 0;
		double squareSum = 0;
		float[] values = new float[r - l];
		int index = 0;
		for (int i = l; i < r; i++) {
			float val = fft.getBand(i);
			sum += val;
			squareSum += val * val;
			values[index++] = val;
		}
		int mid = values.length/2;
		java.util.Arrays.sort(values);
		float median = (values[mid - 1] + values[mid])/2;
		float min = values[0];
		float max = values[values.length -1];
		float mean = (float) sum/(r - l);
		float variance = (float) squareSum/(r - l) - mean * mean;
		float standardDeviation = (float) Math.sqrt(variance);
		// Pearson's skew measure
		float skew = 3 * (mean - median)/standardDeviation;
		if (verbose) {
			println(msg);
			print("  min = "+ min);
			print("  max = "+ max);
			print("  sum = "+ (float) sum);
			print("  mean = "+ mean);
			print("  median = "+ median);
			println("  sd = "+ standardDeviation);
			println("  skew = "+ skew);
		}
		float[] results = new float[6];
		results[0] = min;
		results[1] = max;
		results[2] = mean;
		results[3] = median;
		results[4] = standardDeviation;
		results[5] = skew;
		return results;
	}
    
	/**
	 * Extracts a selected channel from an array of rgb values.
	 * 
	 * @param samples   rgb values in an array of int
	 * @param chan      the channel to extract 
	 * @return          the extracted channel values as an array of floats
	 */
	public float[] pullChannel(int[] samples, ChannelNames chan) {
		// convert sample channel to float array buf
		float[] buf = new float[samples.length];
		int i = 0;
		switch (chan) {
		case L: {
			for (int argb : samples) buf[i++] = brightness(argb);
			break;
		}
		case H: {
			for (int argb : samples) buf[i++] = hue(argb);
			break;
		}
		case S: {
			for (int argb : samples) buf[i++] = saturation(argb);
			break;
		}
		case R: {
			for (int argb : samples)  buf[i++] = (argb >> 16) & 0xFF;
			break;
		}
		case G: {
			for (int argb : samples) buf[i++] = (argb >> 8) & 0xFF;
			break;
		}
		case B: {
			for (int argb : samples) buf[i++] = argb & 0xFF;
			break;
		}
		}
		return buf;
	}
	
	/**
	 * Replaces a specified channel in an array of pixel values with a value 
	 * derived from an array of floats and clipped to the range 0..255.
	 * 
	 * @param samples   an array of pixel values
	 * @param buf       an array of floats
	 * @param chan      the channel to replace
	 */
	public void pushChannel(int[] samples, float[] buf, ChannelNames chan) {
		// convert sample channel to float array buf
		int i = 0;
		switch (chan) {
		case L: {
			colorMode(HSB, 255);
			for (float component : buf) {
				int comp = Math.round((int) component); 
				comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
				int argb = samples[i];
				samples[i++] = color(Math.round(hue(argb)), Math.round(saturation(argb)), comp, 255);
			}
			break;
		}
		case H: {
			colorMode(HSB, 255);
			for (float component : buf) {
				int comp = Math.round((int) component); 
				comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
				int argb = samples[i];
				samples[i++] = color(comp, Math.round(saturation(argb)), Math.round(brightness(argb)), 255);
			}
			break;
		}
		case S: {
			colorMode(HSB, 255);
			for (float component : buf) {
				int comp = Math.round((int) component); 
				comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
				int argb = samples[i];
				samples[i++] = color(Math.round(hue(argb)), comp, Math.round(brightness(argb)), 255);
			}
			break;
		}
		case R: {
			colorMode(RGB, 255);
			for (float component : buf)  {
				int comp = Math.round((int) component); 
				comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
				int argb = samples[i];
				samples[i++] = 255 << 24 | comp << 16 | ((argb >> 8) & 0xFF) << 8 | argb & 0xFF;
			}
			break;
		}
		case G: {
			colorMode(RGB, 255);
			for (float component : buf) {
				int comp = Math.round((int) component); 
				comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
				int argb = samples[i];
				samples[i++] = 255 << 24 | ((argb >> 16) & 0xFF) << 16 | comp << 8 | argb & 0xFF;
			}
			break;
		}
		case B: {
			colorMode(RGB, 255);
			for (float component : buf) {
				int comp = Math.round((int) component); 
				comp = comp > 255 ? 255 : comp < 0 ? 0 : comp;
				int argb = samples[i];
				samples[i++] = 255 << 24 | ((argb >> 16) & 0xFF) << 16 | ((argb >> 8) & 0xFF) << 8 | comp & 0xFF;
			}
			break;
		}
		}
	}
	
	/**
	 * Performs an FFT on a supplied array of samples, scales frequencies using settings in the 
	 * equalizer interface, modifies the samples and also returns the modified samples. 
	 * 
	 * @param samples   an array of RGB values
	 * @param chan      the channel to pass through the FFT
	 * @return          the modified samples
	 */
	public int[] fftEqGlitch(int[] samples, ChannelNames chan) {
		// convert the selected channel to an array of floats
		float[] buf = pullChannel(samples, chan);
		// do a forward transform on the array of floats
		fft.forward(buf);
		// scale the frequencies in the fft by user-selected values from the equalizer interface
		for (int i = 0; i < calculatedBands; i++) {
			// get indices of the range of bands covered by each slider
			int pos = eq.length - i - 1;
			IntRange ir = bandList.get(pos);
			// get the scaling value set by the user
			float scale = eq[pos];
			// scale all bands between lower and upper index
			for (int j = ir.lower; j <= ir.upper; j++) {
				fft.scaleBand(j, scale);
			}
		}
		// inverse the transform
		fft.inverse(buf);
		pushChannel(samples, buf, chan);
		return samples;
	}
		
	/**
     * Performs a zigzag scan, centered in the image, and passes blocks 
     * to an FFT transform that uses a user-supplied equalization curve.
     * 
     * @param order   the width/height of each pixel block to sort
     */
    public void eqZigzagFFT() {
    	int order = (int) Math.sqrt(bufferSize);
    	this.fftBlockWidth = order;
    	Zigzagger zz = new Zigzagger(order);
    	println("Zigzag order = "+ order);
    	int dw = (img.width / order);
    	int dh = (img.height / order);
     	int w = dw * order;
       	int h = dh * order;
       	int ow = (img.width - w) / 2;
       	int oh = (img.height - h) / 2;
 		backup();
		img.loadPixels();
    	for (int y = 0; y < dh; y++) {
    		for (int x = 0; x < dw; x++) {
    			int mx = x * order + ow;
    			int my = y * order + oh;
//     			if (random(1) > 0.5f) {
//    				zz.flipX();
//    			}
//     			if (random(1) > 0.5f) {
//    				zz.flipY();
//    			}
     			int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
    			// the samples are returned by fftEqGlitch, but they are modified already
    			if (isEqGlitchBrightness) fftEqGlitch(pix, ChannelNames.L);
    			if (isEqGlitchHue) fftEqGlitch(pix, ChannelNames.H);
    			if (isEqGlitchSaturation) fftEqGlitch(pix, ChannelNames.S);
    			if (isEqGlitchRed) fftEqGlitch(pix, ChannelNames.R);
    			if (isEqGlitchGreen) fftEqGlitch(pix, ChannelNames.G);
    			if (isEqGlitchBlue) fftEqGlitch(pix, ChannelNames.B);
    			zz.plant(img.pixels, pix, img.width, img.height, mx, my);
    		}
    	}
		img.updatePixels();
		// necessary to call fitPixels to show updated image
		fitPixels(isFitToScreen, false);
//		analyzeEq(false);
    }
    
	/**
	 * Performs an FFT on a supplied array of samples, scales frequencies using settings in the 
	 * statistical interface, modifies the samples and also returns the modified samples. 
	 * 
	 * @param samples   an array of RGB values
	 * @param chan      the channel to pass through the FFT
	 * @return          the modified samples
	 */
	public float[] fftStatGlitch(int[] samples, ChannelNames chan) {
		// convert the selected channel to an array of floats
		float[] buf = pullChannel(samples, chan);
		// do a forward transform on the array of floats
		fft.forward(buf);
		// ignore first bin, the "DC component" if low frequency is cut
		int low = (isLowFrequencyCut) ? 1 : 0;
		float[] stats = fftStat(low, buf.length, false, "fft "+ chan.name());
		float min = stats[0];
		float max = stats[1];
		float mean = stats[2];
		float median = stats[3];
		float sd = stats[4];
		float skew = stats[5];
		int t = samples.length / 2;
		// typical values: left = 0.5f, right = 2.0f
//		float leftEdge = mean - sd * leftBound;
//		float rightEdge = mean + sd * rightBound;
		float leftEdge = leftBound < 0 ? mean - sd * -leftBound : mean + sd * leftBound;
		float rightEdge = rightBound < 0 ? mean - sd * -rightBound : mean + sd * rightBound;
//		println("min = "+ min +", max = "+ max +", mean = "+ mean +", median = "+ median +", sd = " + sd  +", skew = "+ skew +", leftBound = "+ leftBound +", rightBound = "+ rightBound);		
//		println("-- leftEdge = "+ leftEdge +", rightEdge = "+ rightEdge	);
		// scale the frequencies in the fft, skipping band 0
		for (int i = 1; i < t; i++) {
			float val = fft.getBand(i);
			// frequencies whose amplitudes lie outside the bounds are scaled by the cut value
			if (val < leftEdge || val > rightEdge) fft.scaleBand(i, cut);
			// frequencies whose amplitudes lie inside the bounds are scaled by the boost value
			else {
				fft.scaleBand(i, boost);
			}
		}
		// inverse the transform
		fft.inverse(buf);
		pushChannel(samples, buf, chan);
		return stats;
	}
    
	/**
     * Performs a zigzag scan, centered in the image, and passes blocks 
     * to an FFT transform that uses statistical analysis to determine frequency scaling.
     * 
     * @param order   the width/height of each pixel block to sort
     */
    public void statZigzagFFT() {
    	int order = (int) Math.sqrt(bufferSize);
    	this.fftBlockWidth = order;
    	// eliminate fft averaging, don't need it
	    // fft.logAverages(minBandWidth, bandsPerOctave);
    	Zigzagger zz = new Zigzagger(order);
    	println("Zigzag order = "+ order);
    	int dw = (img.width / order);
    	int dh = (img.height / order);
    	int totalBlocks = dw * dh;
     	int w = dw * order;
       	int h = dh * order;
       	int ow = (img.width - w) / 2;
       	int oh = (img.height - h) / 2;
       	float min = 0, max = 0, mean = 0, median = 0, sd = 0, skew = 0;
       	float[] stats = new float[6];
 		backup();
		img.loadPixels();
    	for (int y = 0; y < dh; y++) {
    		for (int x = 0; x < dw; x++) {
    			int mx = x * order + ow;
    			int my = y * order + oh;
//     			if (random(1) > 0.5f) {
//    				zz.flipX();
//    			}
//     			if (random(1) > 0.5f) {
//    				zz.flipY();
//    			}
    			int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
    			if (isStatGlitchBrightness) stats = fftStatGlitch(pix, ChannelNames.L);
    			if (isStatGlitchHue) stats = fftStatGlitch(pix, ChannelNames.H);
    			if (isStatGlitchSaturation) stats = fftStatGlitch(pix, ChannelNames.S);
    			if (isStatGlitchRed) stats = fftStatGlitch(pix, ChannelNames.R);
    			if (isStatGlitchGreen) stats = fftStatGlitch(pix, ChannelNames.G);
    			if (isStatGlitchBlue) stats = fftStatGlitch(pix, ChannelNames.B);
    			min += stats[0];
    			max += stats[1];
    			mean += stats[2];
    			median += stats[3];
    			sd += stats[4];
    			skew += stats[5];
    			zz.plant(img.pixels, pix, img.width, img.height, mx, my);
    		}
    	}
    	min /= totalBlocks;
    	max /= totalBlocks;
    	mean /= totalBlocks;
    	median /= totalBlocks;
    	sd /= totalBlocks;
    	skew /= totalBlocks;
		float leftEdge = leftBound < 0 ? mean - sd * -leftBound : mean + sd * leftBound;
		float rightEdge = rightBound < 0 ? mean - sd * -rightBound : mean + sd * rightBound;
		println("---- Average statistical values for image before FFT ----");
		println("  min = "+ twoPlaces.format(min) +", max = "+ twoPlaces.format(max) +", mean = "+ twoPlaces.format(mean) 
				+", median = "+ twoPlaces.format(median) +", sd = " + twoPlaces.format(sd)  +", skew = "+ twoPlaces.format(skew));		
		println("  leftEdge = "+ twoPlaces.format(leftEdge) +", rightEdge = "+ twoPlaces.format(rightEdge) +", leftBound = "+ leftBound +", rightBound = "+ rightBound);
		img.updatePixels();
		// necessary to call fitPixels to show updated image
		fitPixels(isFitToScreen, false);
//		analyzeEq(false);
    }

    
    /**
     * Resets equalizer FFT controls
     */
    public void resetEq() {
		for (int i = 0; i < eq.length; i++) {
			String token = sliderIdentifier + noPlaces.format(i);
			Slider slider = (Slider) controlP5.getController(token);
			slider.setValue(0);
		}
		analyzeEq(false);
    }

    /**
     * Resets statistical FFT controls
     */
    public void resetStat() {
		Range r02 = (Range) controlP5.getController("setStatEqRange");
		r02.setBroadcast(false);
		r02.setLowValue(defaultLeftBound);
		r02.setHighValue(defaultRightBound);
		r02.setArrayValue(0, defaultLeftBound);
		r02.setArrayValue(1, defaultRightBound);
		rightBound = defaultRightBound;
		leftBound = defaultLeftBound;
		r02.setBroadcast(true);
    	Numberbox n4 = (Numberbox) controlP5.getController("setBoost");
    	n4.setValue(defaultBoost);
    	Numberbox n5 = (Numberbox) controlP5.getController("setCut");
    	n5.setValue(defaultCut);
   }
      
    /**
     * parameterless method that ControlP5 button calls (a workaround)
     */
    public void analyzeEqBands() {
     	analyzeEq(true);
    }
     
    // TODO calculate accurate center frequency values for the bands we actually have
    /**
     * Examines display buffer Brightness channel and outputs mean 
     * amplitudes of frequency bands shown in equalizer.
     * 
     * @param isPrintToConsole   if true, prints information to console
     */
    public void analyzeEq(boolean isPrintToConsole) {
    	int order = (int) Math.sqrt(bufferSize);
    	this.fftBlockWidth = order;
//    	if (8 != order && 16 != order && 32 != order && 64 != order && 128 != order && 256 != order && 512 != order) {
//    		println("block size must be 8, 16, 32, 64, 128, 256 or 512 for FFT glitching");
//    		return;
//    	}
    	Zigzagger zz = new Zigzagger(order);
    	// calculate how many complete blocks will fit horizontally and vertically
    	int dw = (img.width / order);
    	int dh = (img.height / order);
    	int howManyBlocks =  dw * dh;
    	// calculate the number of pixels in the vertical and horizontal block extents
     	int w = dw * order;
       	int h = dh * order;
       	// calculate offsets towards the center, if blocks don't completely cover the image
       	int ow = (img.width - w) / 2;
       	int oh = (img.height - h) / 2;
		img.loadPixels();
		int blockNum = 0;
		binTotals = new double[calculatedBands];
		// minimum brightness value in image
		float min = -1;
		// maximum brightness value in image
		float max = 0;
		java.util.Arrays.fill(binTotals, 0);
    	for (int y = 0; y < dh; y++) {
    		for (int x = 0; x < dw; x++) {
    			int mx = x * order + ow;
    			int my = y * order + oh;
    			int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
    			float[] buf = new float[pix.length];
    			colorMode(HSB, 255);
    			// load buf with brightness values from block at mx, my
    			for (int i = 0; i < pix.length; i++) {
    				int c = pix[i];
    				buf[i] = brightness(c);
    				if (verbose) println(pix[i]);
    			}
    			fft.forward(buf);
    			float[] stats = fftStat(0, buf.length, false, "fft brightness in frequency domain");
    			if (min == -1) min = stats[0];
    			else if (min > stats[0]) min = stats[0];
    			if (max < stats[1]) max = stats[1];
    			// sum the values in each band in our band list and stash the mean value in binTotals
     		   	for (int i = 0; i < calculatedBands; i++) {
     		   		IntRange ir = bandList.get(i);
     		   		float sum = 0;
     				for (int j = ir.lower; j <= ir.upper; j++) {
     					sum += fft.getBand(j);
     				}
     				// divide sum by (number of bins in band i) *  (total number of blocks)
    		   		binTotals[i] += sum/((ir.upper - ir.lower + 1) * howManyBlocks);
    	    	}
     		   blockNum++;
    		}
    	}
    	if (isPrintToConsole) {
    		println("--- "+ blockNum +" blocks read, min = "+ min +", max = "+ max);
    		for (int i = 0; i <calculatedBands; i++) {
    			// divide the accumlated mean values from each block's band ranges
    			// by the total number of blocks to get the normalized average over the image
     			println("  band "+ i +": "+ twoPlaces.format(binTotals[i]));
    		}
    	}
    }
    
    // TODO output accurate center frequency values 
    /**
     * Calculates avaialable frequency bands for current FFT buffer, returns an array of integer ranges
     * representing frequency bin index numbers. Sets calculatedBands to size of bandList array.
     * 
     * @return   array of integer ranges corresponding to frequency bin index numbers
     */
    public ArrayList<IntRange> calculateEqBands() {
    	bandList = new ArrayList<IntRange>();
    	int slots = minBandWidth * (bandsPerOctave);
    	ArrayList<FloatRange> freqList = new ArrayList<FloatRange>(slots);
    	// we can obtain frequencies up to the Nyquist limit, which is half the sample rate
    	float hiFreq = sampleRate / 2.0f, loFreq = 0;
    	// bandsPerOctave = 3
    	FloatRange fr;
    	int pos = slots - 1;
    	for (int i = 0; i < minBandWidth; i++) {
    		loFreq = hiFreq * 0.5f;
    		float incFreq = (hiFreq - loFreq)/bandsPerOctave;
    		// inner loop could be more efficient
    		for (int j = bandsPerOctave; j > 0; j--) {
    	   		fr = new FloatRange(loFreq + (j - 1) * incFreq, loFreq + j * incFreq);
         		freqList.add(fr);
    		}
     		hiFreq = loFreq;
    	}
    	// reverse the frequency list, it should go from low to high
    	for (int left = 0, right = freqList.size() - 1; left < right; left++, right--) {
    	    // exchange the first and last
    	    FloatRange temp = freqList.get(left); 
    	    freqList.set(left, freqList.get(right)); 
    	    freqList.set(right, temp);
    	}
    	// figure out the bins
     	int hiBin = 0;
    	int loBin = 0;
    	float freq0 = fft.indexToFreq(0);
    	float freq = fft.indexToFreq(hiBin);
    	IntRange ir = null;
    	for (FloatRange r : freqList) {
    		if (freq < freq0) continue;
    		while (freq < r.upper) {
    			freq = fft.indexToFreq(hiBin++);
    		}
    		IntRange temp = new IntRange(loBin, hiBin);
    		if (!temp.equals(ir)) {
    			bandList.add(temp);
    			ir = temp;
    		}
    		loBin = hiBin;
    	}
    	// TODO maybe there's a less kludgey way to initilize, without the following correction
    	// fix off by two error....
    	bandList.get(bandList.size() - 1).upper = fft.specSize() - 1;
    	// omit printing of lists
    	calculatedBands = bandList.size();
    	println("----- number of frequency bands = "+ calculatedBands);
    	// don't need to do averaging, without it FFT should be faster
    	// (minBandWidth, bandsPerOctave);
    	return bandList;
    }
    
    
    /**
     * not used
     */
    public void printEqInfo() {
    	int ct = 0;
    	println("-------- frequencies --------");
    	Iterator<FloatRange> iter = this.freqList.iterator();
    	while (iter.hasNext()) {
    		FloatRange fr = iter.next();
    		println("  "+ ct++ +": "+ twoPlaces.format(fr.lower) +", "+ twoPlaces.format(fr.upper));
    	}
    	println();
    }
  
    /**
     * Calculates and outputs statistics for display buffer, determined by current FFT and equalizer bands. 
     */
    public void testEq() {
    	int slots = minBandWidth * (bandsPerOctave);
    	ArrayList<FloatRange> freqList = new ArrayList<FloatRange>(slots);
    	// we can obtain frequencies up to the Nyquist limit, which is half the sample rate
    	float hiFreq = sampleRate / 2.0f, loFreq = 0;
    	// bandsPerOctave = 3
    	FloatRange fr;
    	int pos = slots - 1;
    	for (int i = 0; i < minBandWidth; i++) {
    		loFreq = hiFreq * 0.5f;
    		float incFreq = (hiFreq - loFreq)/bandsPerOctave;
    		// inner loop could be more efficient
    		for (int j = bandsPerOctave; j > 0; j--) {
    	   		fr = new FloatRange(loFreq + (j - 1) * incFreq, loFreq + j * incFreq);
         		freqList.add(fr);
    		}
     		hiFreq = loFreq;
    	}
    	// reverse the list
    	for (int left = 0, right = freqList.size() - 1; left < right; left++, right--) {
    	    // exchange the first and last
    	    FloatRange temp = freqList.get(left); 
    	    freqList.set(left, freqList.get(right)); 
    	    freqList.set(right, temp);
    	}
    	// figure out the bins
    	ArrayList<IntRange> theBandList = new ArrayList<IntRange>();
    	int hiBin = 0;
    	int loBin = 0;
    	float freq0 = fft.indexToFreq(0);
    	float freq = fft.indexToFreq(hiBin);
    	IntRange ir = null;
    	for (FloatRange r : freqList) {
    		if (freq < freq0) continue;
    		while (freq < r.upper) {
    			freq = fft.indexToFreq(hiBin++);
    		}
    		IntRange temp = new IntRange(loBin, hiBin);
    		if (!temp.equals(ir)) {
    			theBandList.add(temp);
    			ir = temp;
    		}
    		loBin = hiBin;
    	}
     	// print out the list
    	int ct = 0;
    	println("\n---- Frequency List ----");
    	for (FloatRange r : freqList) {
    		println("  "+ ct +": "+ r.toString());
    		ct++;
    	}
    	ct = 0;
    	println("\n---- Band List ----");
    	for (IntRange r : theBandList) {
    		println("  "+ ct +": "+ r.toString());
    		ct++;
    	}
    	println("  freq 0 = "+ fft.indexToFreq(0) +", freq "+ fft.specSize() +" = "+ fft.indexToFreq(fft.specSize()));
    	println("\n");
    }
    
    /**
     * sets up audification
     */
    public void audify() {
    	if (null == glitchSignal) {
    		glitchSignal = new GlitchSignal();
    		out = minim.getLineOut(Minim.STEREO, 64 * 64);
    		
     		out.addSignal(glitchSignal);
    	}
    	else {
        	int blockEdgeSize = (int) Math.sqrt(bufferSize);
    		// update dimensions to catch rotations, new images, etc.;
    		int dw = (img.width / blockEdgeSize);
    		int dh = (img.height / blockEdgeSize);
    		int w = dw * blockEdgeSize;
    		int h = dh * blockEdgeSize;
    		int ow = (img.width - w) / 2;
    		int oh = (img.height - h) / 2;
    		int inX = 0, inY = 0;
    		if (isFitToScreen) {
    			inX = (int) map(mouseX, 0, fitImg.width, 0, img.width);
    			inY = (int) map(mouseY, 0, fitImg.height, 0, img.height);
    		}
    		else {
    			inX = mouseX;
    			inY = mouseY;
    		}
			int mapX = (inX/blockEdgeSize) * blockEdgeSize + ow;
			int mapY = (inY/blockEdgeSize) * blockEdgeSize + oh;
			if (mapX > w - blockEdgeSize + ow || mapY > h - blockEdgeSize + oh) return;
    		Zigzagger zz = new Zigzagger(blockEdgeSize);
    		img.loadPixels();
    		int[] pix = zz.pluck(img.pixels, img.width, img.height, mapX, mapY);
    		// do something to a single block
    		if ('g' == lastCommand) this.sortTool.sort(pix);
    		else if ('k' == lastCommand) fftStatGlitch(pix, ChannelNames.L);
    		else if ('j' == lastCommand) fftEqGlitch(pix, ChannelNames.L);
    		else this.sortTool.sort(pix);
    		zz.plant(img.pixels, pix, img.width, img.height, mapX, mapY);
    		img.updatePixels();
    		// necessary to call fitPixels to show updated image
    		fitPixels(isFitToScreen, false);
     	}
    }
    
    /**
     * turns off audification
     */
    public void audifyOff() {
    	if (null != glitchSignal) {
    		out.removeSignal(glitchSignal);
    		glitchSignal = null;
    	}
    }
    
    /**
     * @author paulhz
     * a class that implements an AudioSignal interface, used by Minim library to produce sound.
     */
    public class GlitchSignal implements AudioSignal {
    	int blockEdgeSize = 64;
    	Zigzagger zz;
    	int dw;
    	int dh;
    	int w;
    	int h;
    	int ow;
    	int oh;
    	int mapX;
    	int mapY;
    	float[] buf;

    	public GlitchSignal() {
       		println("audio Zigzag order = "+ blockEdgeSize);
       		zz = new Zigzagger(blockEdgeSize);
       		
    	}
    	
    	public Zigzagger getZz() {
    		if (null == zz) {
          		zz = new Zigzagger(blockEdgeSize);
    		}
    		return zz;
    	}
    	
    	public int getBlockEdgeSize() {
    		return this.blockEdgeSize;
    	}

    	public void generate(float[] samp) {
    		// update dimensions to catch rotations, new images, etc.;
    		dw = (img.width / blockEdgeSize);
    		dh = (img.height / blockEdgeSize);
    		w = dw * blockEdgeSize;
    		h = dh * blockEdgeSize;
    		ow = (img.width - w) / 2;
    		oh = (img.height - h) / 2;
    		int inX = 0, inY = 0;
    		if (isFitToScreen) {
    			inX = (int) map(mouseX, 0, fitImg.width, 0, img.width);
    			inY = (int) map(mouseY, 0, fitImg.height, 0, img.height);
    		}
    		else {
    			inX = mouseX;
    			inY = mouseY;
    		}
			int mx = (inX/blockEdgeSize) * blockEdgeSize + ow;
			int my = (inY/blockEdgeSize) * blockEdgeSize + oh;
			if (mx > w - blockEdgeSize + ow || my > h - blockEdgeSize + oh) return;
	   		float fac = 1.0f/255 * 2.0f;
			if (mx == this.mapX && my == this.mapY) {
				// still in the same location, just copy the buffer
	     		for (int i = 0; i < buf.length; i++) {
	    			samp[i] = buf[i];
	    		}
			} 
			else {
				// in a new location, calculate a new buffer
				this.mapX = mx;
				this.mapY = my;
	    		int[] pix = getZz().pluck(img.pixels, img.width, img.height, mapX, mapY);
	    		buf = pullChannel(pix, ChannelNames.L);
	     		for (int i = 0; i < buf.length; i++) {
	     			buf[i] = buf[i] * fac - 1.0f;
	    			samp[i] = buf[i];
	    		}
			}
    	}

    	// this is a stricly mono signal
    	public void generate(float[] left, float[] right)
    	{
    		generate(left);
    		generate(right);
    	}

    }
    

	
    /********************************************/
    /*                                          */
    /*              >>> ZIGZAG <<<              */
    /*                                          */
    /********************************************/

    /**
     * Sets zigzagFloor and zigzagCeiling in response to control panel.
     * @param val   a value forwarded by ControlP5 that we will ignore (just in this case)
     */
    public void setZigzagRange(float val) {
    	// here's one way to retrieve the values of the range controller
		Range r1 = (Range) controlP5.getController("setZigzagRange");
		if (!r1.isInside()) {
			return;
		}
		zigzagFloor = (int) r1.getArrayValue()[0];
		zigzagCeiling = (int) r1.getArrayValue()[1];
    }
 
	/**
	 * Sets the value above which the current sort method will randomly interrupt, when randomBreak 
	 * is true (the default). Each sorting method uses a distinct value from 1 to 999. Quick sort
	 * can use very low values, down to 1.0. The other sorting methods--shell sort, insert sort, 
	 * bubble sort--generally work best with higher values. 
	 * @param newBreakPoint   the breakpoint to set
	 */
	public void setZigzagPercent(float newZigzagPercent) {
		if (newZigzagPercent == zigzagPercent) return;
		zigzagPercent = newZigzagPercent;
	}

    
    /**
     * Performs a zigzag sort, centered in the image.
     * @param order   the width/height of each pixel block to sort
     */
    public void zigzag(int order) {
    	// TODO better fix: ControlP5 button press calls here with 0 for order, apparently...
    	if (0 == order) order = zigzagBlockWidth;
     	Zigzagger zz = new Zigzagger(order);
    	println("Zigzag order = "+ order);
    	int dw = (img.width / order);
    	int dh = (img.height / order);
     	int w = dw * order;
       	int h = dh * order;
       	int ow = (img.width - w) / 2;
       	int oh = (img.height - h) / 2;
 		backup();
		img.loadPixels();
		println("--- "+ zigzagStyle.name() +" zigzag ----");
		if (ZigzagStyle.PERMUTE != zigzagStyle) {
			for (int y = 0; y < dh; y++) {
				for (int x = 0; x < dw; x++) {
			    	// a quick way to sort only a determined percentage of cells
			    	if (random(100) > (int)(zigzagPercent)) continue;
					int mx = x * order + ow;
					int my = y * order + oh;
					int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
					this.sortTool.sort(pix);
					zz.plant(img.pixels, pix, img.width, img.height, mx, my);
					if (ZigzagStyle.RANDOM == zigzagStyle) {
						if (random(1) > 0.5f) {
							zz.flipX();
						}
						if (random(1) > 0.5f) {
							zz.flipY();
						}
					}
				}
			}
		}
		else {
			// permute zigzag orientation in 2x2 blocks
			int[] perm = {0, 1, 2, 3};
			Zigzagger[] zzList = new Zigzagger[4];
			zzList[0] = zz;
			zz = new Zigzagger(order);
			zz.flipX();
			zzList[1] = zz;
			zz = new Zigzagger(order);
			zz.flipX();
			zz.flipY();
			zzList[2] = zz;
			zz = new Zigzagger(order);
			zz.flipY();
			zzList[3] = zz;
			int dw2 = dw/2;
			int dh2 = dh/2;
			for (int y = 0; y < dh2; y++) {
				for (int x = 0; x < dw2; x++) {
			    	// a quick way to sort only a determined percentage of cells
			    	if (random(100) > (int)(zigzagPercent)) continue;
					int mx = 2 * x * order + ow;
					int my = 2 * y * order + oh;
					shuffle(perm);
					zz = zzList[perm[0]];
					int[] pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
					this.sortTool.sort(pix);
					zz.plant(img.pixels, pix, img.width, img.height, mx, my);
					zz = zzList[perm[1]];
					my += order;
					pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
					this.sortTool.sort(pix);
					zz.plant(img.pixels, pix, img.width, img.height, mx, my);
					zz = zzList[perm[2]];
					mx += order;
					pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
					this.sortTool.sort(pix);
					zz.plant(img.pixels, pix, img.width, img.height, mx, my);
					zz = zzList[perm[3]];
					my -= order;
					pix = zz.pluck(img.pixels, img.width, img.height, mx, my);
					this.sortTool.sort(pix);
					zz.plant(img.pixels, pix, img.width, img.height, mx, my);
				}
			}
		}
		img.updatePixels();
		// necessary to call fitPixels to show updated image
		fitPixels(isFitToScreen, false);
    }
    
    /**
      * Performs a zigzag sort, centered in the image, sets the width of the square 
      * pixel blocks to a random number between zigzagFloor and zigzagCeiling + 1.
     */
    public void zigzag() {
    	int order = (int) random(zigzagFloor, zigzagCeiling + 1);
    	zigzagBlockWidth = order;
    	println("zigzagFloor = "+ zigzagFloor +", zigzagCeiling = "+ zigzagCeiling +", order = "+ order);
    	zigzag(order);
    }
    
    
    /**
     * Facilitates the "zigzag" scanning of a square block of pixels with a variable edge dimension set by the user.
     * This sort of scanning is used in the JPEG compression algorithm, and occasionally shows up in JPEG errors (glitches).
     * Provides two methods for reading (pluck) and writing (plant) from an array of pixels.
     *
     */
    class Zigzagger {
    	/** x coordinates */
    	private int[] xcoords;
    	/** y coordinates */
    	private int[] ycoords;
    	/** the dimension of an edge of the square block of pixels */
    	private int d;
    	/** counter variable f = d + d - 1: number of diagonals in zigzag */
    	private int f;

    	/**
    	 * @param order   the number of pixels on an edge of the scan block
    	 */
    	public Zigzagger(int order) {
    		d = order;
    		f = d + d - 1;
    		xcoords = new int[d * d];
    		ycoords = new int[d * d];
    		generateCoords();
    	}

    	/**
    	 * Generates coordinates of a block of pixels of specified dimensions, offset from (0,0).
    	 */
    	private void generateCoords() {
    		int p = 0;
    		int n = 0;
    		for (int t = 0; t < f; t++) {
    			if (t < d) {
    				n++;
    				if (n % 2 == 0) {
    					for (int i = 0; i < n; i++) {
    						xcoords[p] = n - i - 1;
    						ycoords[p] = i;
    						p++;
    					}
    				}
    				else {
    					for (int i = 0; i < n; i++) {
    						xcoords[p] = i;
    						ycoords[p] = n - i - 1;
    						p++;
    					}
    				}
    			}
    			else {
    				n--;
    				if (n % 2 == 0) {
    					for (int i = 0; i < n; i++) {
    						xcoords[p] = d - i - 1 ;
    						ycoords[p] = i + d - n;
    						p++;
    					}
    				}
    				else {
    					for (int i = 0; i < n; i++) {
    						xcoords[p] = i + d - n;
    						ycoords[p] = d - i - 1;
    						p++;
    					}
    				}
    			}
    		}
    	}
    	
    	public void flipX() {
    		int m = d - 1;
    		for (int i = 0; i < xcoords.length; i++) {
    			xcoords[i] = m - xcoords[i];
    		}
    	}
   	
    	public void flipY() {
    		int m = d - 1;
    		for (int i = 0; i < ycoords.length; i++) {
    			ycoords[i] = m - ycoords[i];
    		}
    	}
   	
    	/**
    	 * @param pix   an array of pixels
    	 * @param w     width of the image represented by the array of pixels
    	 * @param h     height of the image represented by the array of pixels
    	 * @param x     x-coordinate of the location in the image to scan
    	 * @param y     y-coordinate of the location in the image to scan
    	 * @return      an array in the order determined by the zigzag scan
    	 */
    	public int[] pluck(int[] pix, int w, int h, int x, int y) {
    		int len = d * d;
    		int[] out = new int[len];
    		for (int i = 0; i < len; i++) {
    			int p = (y + ycoords[i]) * w + (x) + xcoords[i];
    			if (verbose) println("x = "+ x +", y = "+ y +", i = "+ i +", p = "+ p +", zigzag = ("+ xcoords[i] +", "+ ycoords[i] +")");
    			out[i] = pix[p];
    		}
    		return out;
    	}
    	
    	/**
    	 * @param pix      an array of pixels
    	 * @param sprout   an array of d * d pixels to write to the array of pixels
    	 * @param w        width of the image represented by the array of pixels
    	 * @param h        height of the image represented by the array of pixels
    	 * @param x        x-coordinate of the location in the image to write to
    	 * @param y        y-coordinate of the location in the image to write to
    	 */
    	public void plant(int[] pix, int[] sprout, int w, int h, int x, int y) {
    		for (int i = 0; i < d * d; i++) {
     			int p = (y + ycoords[i]) * w + (x) + xcoords[i];
    			pix[p] = sprout[i];
    		}
    	}
    	
    	/* (non-Javadoc)
    	 * returns a list of coordinate points that define a zigzag scan of order d.
    	 * @see java.lang.Object#toString()
    	 */
    	public String toString() {
    		StringBuffer buf = new StringBuffer();
    		buf.append("Zigzag order: "+ this.d +"\n  ");
    		for (int i = 0; i < xcoords.length; i++) {
    			buf.append("("+ xcoords[i] +", "+ ycoords[i] +") ");
    		}
    		buf.append("\n");
    		return buf.toString();
    	}
    }

}
