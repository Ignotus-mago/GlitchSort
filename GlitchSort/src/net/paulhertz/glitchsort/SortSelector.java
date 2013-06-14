package net.paulhertz.glitchsort;

import processing.core.*;
import net.paulhertz.glitchsort.constants.*;

/**
 * Class for selecting a sorting method among those available.
 *
 */
class SortSelector {
	AbstractColorSorter sorter;
	InsertSorter insert;
	ShellSorter shell;
	QuickSorter quick;
	BubbleSorter bubble;
	GlitchSort app;
	
	public SortSelector(GlitchSort app) {
		this.app = app;
		shell = new ShellSorter(app);
		quick = new QuickSorter(app);
		bubble = new BubbleSorter(app);
		insert = new InsertSorter(app);
		this.sorter = quick;
	}
	
	public void setRandomBreak(boolean isRandomBreak) {
		shell.setRandomBreak(isRandomBreak);
		quick.setRandomBreak(isRandomBreak);
		bubble.setRandomBreak(isRandomBreak);
		insert.setRandomBreak(isRandomBreak);
	}
	
	public QuickSorter getQuick() {
		return quick;
	}
	
	public ShellSorter getShell() {
		return shell;
	}
	
	public BubbleSorter getBubble() {
		return bubble;
	}
	
	public InsertSorter getInsert() {
		return insert;
	}

	public Sorter getSorter() {
		return sorter;
	}
	
	public void setSorter(SorterType type) {
		switch (type) {
		case QUICK: { sorter = quick; break; }
		case SHELL: { sorter = shell; break; }
		case BUBBLE: { sorter = bubble; break; }
		case INSERT: { sorter = insert; break; }
		default: { sorter = quick; }
		}
	}
	
	public void setControlState() {
		sorter.setAscendingSort(app.isAscendingSort);
		sorter.setBreakPoint(app.breakPoint);
		sorter.setCompOrder(app.compOrder);
		sorter.setRandomBreak(app.randomBreak);
		sorter.setSwapChannels(app.isSwapChannels);
		sorter.setSwap(app.swap);
	}

	public void sort(int[] a, int l, int r) {
		sorter.sort(a, l, r);
	}
	
	public void sort(int[] a) {
		sorter.sort(a);
	}
	
	public void insertSort(int[] a, int l, int r) {
		insert.sort(a, l, r);
	}

	public void bubbleSort(int[] a, int l, int r) {
		bubble.sort(a, l, r);
	}

	public void shellSort(int[] a, int l, int r) {
		shell.sort(a, l, r);
	}

	public void quickSort(int[] a, int l, int r) {
		quick.sort(a, l, r);
	}
	
	/**
	 * Performs an insert sort on an array of ints. Insert sort proceeds through
	 * the array from beginning to end, comparing every number against all remaining numbers. 
	 * It is much slower than quick sort or shell sort.
	 */
	class InsertSorter extends AbstractColorSorter implements Sorter {
		
		public InsertSorter(PApplet app, float breakPoint) {
			super(app);
			this.breakPoint = breakPoint;
			this.sorterType = SorterType.INSERT;
		}
		public InsertSorter(PApplet app) {
			this(app, 999.0f);
		}

		@Override
		public void sort(int[] a, int l, int r) { 
			outerloop:
				for (int i = l+1; i <= r; i++) {
					for (int j = i; j > l; j--) {
						compExch(a, j-1, j); 
						if (this.isRandomBreak) {
							if (breakTest()) {
								// if (verbose) println("random break at "+ count);
								break outerloop;
							}
						}
					}
				}
		} 
		
	}
	

	/**
	 * Performs a quick sort on an array of ints. Quicksort uses a divide and 
	 * conquer approach to sorting. It partitions the array into smaller arrays, recursively.
	 * With random breaks, it makes more interesting glitches than InsertSorter, since it operates 
	 * over larger distances to exchange keys. It is also a very fast sorting method for disordered
	 * arrays (most pictures, in other words) but will crawl if fed an array that is already sorted
	 * or nearly sorted (or inverse sorted or nearly inverse sorted). 
	 */
	class QuickSorter extends AbstractColorSorter implements Sorter {

		public QuickSorter(PApplet app, float breakPoint) {
			super(app);
			this.breakPoint = breakPoint;
			this.sorterType = SorterType.QUICK;
		}
		public QuickSorter(PApplet app) {
			this(app, 144.0f);
		}
				
		@Override
		public void sort(int[] a, int l, int r) { 
			if (r <= l) return;
			int i = partition(a, l, r);
			if (this.isRandomBreak) {
				if (breakTest()) {
					// if (verbose) println("random break at "+ count);
					return;
				}
			}
			sort(a, l, i - 1);
			sort(a, i + 1, r);
		} 
		
		public int partition(int[] a, int l, int r) {
			int i = l-1;
			int j = r; 
			int v = a[r]; 
			for (;;) { 
				while (less(a[++i], v)); 
				while (less(v, a[--j])) if (j == l) break; 
				if (i >= j) break; 
				exch(a, i, j); 
			} 
			exch(a, i, r); 
			return i; 
		}
	}
	

	/**
	 * Performs a shell sort on an array of ints. Shell sort uses a divide and 
	 * conquer approach to sorting, partitioning the array into smaller arrays 
	 * using a variable h to mark the boundaries of subarrays. With random breaks, 
	 * it makes more interesting glitches than InsertSorter and different from QuickSorter.
	 * Vary ratio and divisor to get different partitions of the pixels
	 */
	class ShellSorter extends AbstractColorSorter implements Sorter {
		int h;
		int ratio = 3;
		int divisor = 9;

		public ShellSorter(PApplet app, float breakPoint) {
			super(app);
			this.breakPoint = breakPoint;
			this.sorterType = SorterType.SHELL;
		}
		public ShellSorter(PApplet app) {
			this(app, 996.0f);
		}
		
		@Override
		public void sort(int[] a, int l, int r) {
			for (h = 1; h <= (r - l)/divisor; h = ratio * h + 1);
			outerloop:
			for ( ; h > 0; h /= ratio) {
				// perform an "h-sort" over the array, i.e., an insert sort of every h elements
				for (int i = l+h; i <= r; i++) { 
					int j = i; 
					int v = a[i]; 
					while (j >= l + h && less(v, a[j - h])) { 
						a[j] = a[j - h]; 
						j -= h; 
					} 
					a[j] = v; 
					if (this.isRandomBreak) {
						if (breakTest()) {
							// if (verbose) println("random break at "+ count);
							break outerloop;
						}
					}
				}
			}
		}
	
		/**
		 * @param ratio the ratio to set
		 */
		public void setRatio(int ratio) {
			this.ratio = ratio;
		}
		/**
		 * @param divisor the divisor to set
		 */
		public void setDivisor(int divisor) {
			this.divisor = divisor;
		}
		
	}
	
	
	/**
	 * Performs a bubble sort on an array of int, such as pixel rows in an image.
	 * Small keys percolate over to the left in bubble sort. As the sort moves from right to left, 
	 * each key is exchanged with the one on its left until a smaller one is encountered.
	 * Bubble sort is very slow, but the way it operates creates some interesting glitches. 
	 * Color-swapping also looks good with this sorting method.
	 */
	class BubbleSorter extends AbstractColorSorter implements Sorter {
	
		public BubbleSorter(PApplet app, float breakPoint) {
			super(app);
			this.breakPoint = breakPoint;
			this.sorterType = SorterType.BUBBLE;
		}
		public BubbleSorter(PApplet app) {
			this(app, 990.0f);
		}
		
		@Override
		public void sort(int[]a, int l, int r) {
			outerloop:
				for (int i = l; i < r; i++) 
					for (int j = r; j > i; j--) {
						compExch(a, j-1, j); 		
						if (this.isRandomBreak) {
							if (breakTest()) {
								// if (verbose) println("random break at "+ count);
								break outerloop;
							}
						}
					}
		}
	}
	
	

}
