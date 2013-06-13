/**
 * 
 */
package net.paulhertz.glitchsort;

/**
 * @author paulhz
 * Basic sorting interface, implemented by InsertSorter, QuickSorter, ShellSorter and BubbleSorter classes.
 */
public interface Sorter {
	/**
	 * Compare two ints, return true if first is less than second.
	 * @param v   an int 
	 * @param w   another int to compare with the first
	 * @return    true if the first int is less than the second, false otherwise
	 */
	public boolean less(int v, int w);
	/**
	 * Exchange two ints in an array.
	 * @param a   an array of int
	 * @param i   an index into the array
	 * @param j   an index into the array
	 */
	public void exch(int[] a, int i, int j);
	/**
	 * Compares two ints in an array and exchanges them if the int  
	 * at the first index is less than the int at the second index.
	 * @param a   an array of int
	 * @param i   an index into the array
	 * @param j   an index into the array
	 */
	public void compExch(int[] a, int i, int j);		
	/**
	 * Sort an array or int between a left index and a right index.
	 * Each sorting algorithm has its own implementation of this method that 
	 * fills in the abstract method in AbstractColorSorter.
	 * @param a   an array of int
	 * @param l   the left (lower) index
	 * @param r   the right (upper) index
	 */
	public void sort(int[] a, int l, int r);
	/**
	 * Sorts an array of ints, returning result in the array
	 * This is a convenience method, implemented in the abstract class AbstractColorSorter
	 * @param a   an array of int
	 */
	public void sort(int[] a);
	/**
	 * @return the breakPoint
	 */
	public float getBreakPoint();
	/**
	 * @param breakPoint the breakPoint to set
	 */
	public void setBreakPoint(float breakPoint);
	/**
	 * @return the sorterType
	 */
	public SorterType getSorterType();
}
