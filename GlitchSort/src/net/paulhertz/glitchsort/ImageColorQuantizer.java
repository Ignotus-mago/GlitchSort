package net.paulhertz.glitchsort;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;


/**
 * @author paulhz
 * Provides color reduction, useful for creating GIFs and PNGs. The colorCount value probably should not exceed 255,
 * unless you modify the octree for a larger color space. From what I recall when I read up on the subject,
 * octrees are a reasonably good color quantization method, better than "popularity contest" quantization. 
 * This is old code that I adapted for Processing recently, without much testing.
 */
public class ImageColorQuantizer implements BufferedImageOp {
	protected int colorCount = 255;
	private Octree tree;
	int[] pixels;

    public ImageColorQuantizer(int howManyColors) {
        colorCount = howManyColors;
    }

    // perform the filter operation, required by BufferedImageOp
    /* (non-Javadoc)
     * @see java.awt.image.BufferedImageOp#filter(java.awt.image.BufferedImage, java.awt.image.BufferedImage)
     */
    public final BufferedImage filter (BufferedImage src, BufferedImage dst) {
        if (dst == null) dst = createCompatibleDestImage(src, null);
        tree = new Octree();
        System.out.println("-----> quantizing image with max colors: " + colorCount);
        int w = src.getWidth();
        int h = src.getHeight();
        // pop the color data into an array of packed ARGB color components
        pixels = src.getRGB(0, 0, w, h, null, 0, w);
        tree.quantize(pixels, w, colorCount);
        // assign the array to the destination image
        dst.setRGB(0, 0, w, h, pixels, 0, w);
        return dst;
    }
    
    
    int[] getPixels() {
    	if (null != pixels) return pixels;
    	return null;
    }
	
	
	/**
	 * @return   the number of colors in the colormap
	 */
	public int colorCount() {
		return this.colorCount;
	}
	public void setColorCount(int newColorCount) {
		this.colorCount = newColorCount;
	}


	/**
	 * @return  the Octree used to sort and reduce the colors
	 */
	public Octree tree() {
		return this.tree;
	}


	/**
	 * @return   an array of int containing color values in RGB format
	 */
	public int[] colormap() {
		if (tree != null) {
			return tree.colormap;
		}
		else {
			return null;
		}
	}


    // create a destination image compatible with the input, required by BufferedImageOp
    /* (non-Javadoc)
     * @see java.awt.image.BufferedImageOp#createCompatibleDestImage(java.awt.image.BufferedImage, java.awt.image.ColorModel)
     */
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel dstCM) {
        BufferedImage image;
        if (dstCM == null) dstCM = src.getColorModel();
        int width = src.getWidth();
        int height = src.getHeight();
        image = new BufferedImage ( dstCM,
                                    dstCM.createCompatibleWritableRaster(width, height),
                                    dstCM.isAlphaPremultiplied(), null );
        return image;
    }
	
	
    // some filters change image size, so BufferedImageOp requires this method
    // to report destination size. We don't change size, so we return the source image bounds
    /* (non-Javadoc)
     * @see java.awt.image.BufferedImageOp#getBounds2D(java.awt.image.BufferedImage)
     */
    public final Rectangle2D getBounds2D(BufferedImage src) {
        return src.getRaster().getBounds();
    }
	
	
    // BufferedImageOp requires this method to indicate how points in source image
    // map to points in destination image. We don't do geometric transforms, so
    // we just return the srcPt values in dstPt.
    /* (non-Javadoc)
     * @see java.awt.image.BufferedImageOp#getPoint2D(java.awt.geom.Point2D, java.awt.geom.Point2D)
     */
    public final Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) dstPt = new Point2D.Float();
        dstPt.setLocation(srcPt.getX(), srcPt.getY());
        return dstPt;
    }
	
	
    // BufferedImageOp requires a method to report RenderingHints applied in the filter
    // We don't use RenderingHints, so we return null
    /* (non-Javadoc)
     * @see java.awt.image.BufferedImageOp#getRenderingHints()
     */
    public final RenderingHints getRenderingHints() { return null; }
	
	
	/**
	 * Octree.java
	 * -----------
	 * @author  Sascha L. Teichmann (sascha@intevation.de)
	 * @version 0.1
	 * date     21:31:02 Mi 07-Mrz-2001
	 * modified to use BufferedImage 7-Aug-2002 by Paul Hertz
	 * Octrees partition 3D space. In this instance, the space is a color cube
	 * with R, G, and B axes. We parse the colorCount in the source image to create the octree
	 */
    public class Octree {
        Node root;
        public static final int MAX_TREE_DEPTH = 8;
        public static final int MAX_NODES      = 266817;
        public static final int MAX_RGB        = 255;
        // prepare squares table, used to calculate distances between colorCount
        public int SQUARES[] = new int[MAX_RGB*2+1];
        public int shift[] = new int[MAX_TREE_DEPTH+1];
        int depth;  // depth of the tree
        int nodes;  // number of nodes in tree
        int colors; // nodes of colorCount
        int pruningTreshold;
        int nextPruningTreshold;
        int colormap[];
        byte indexedPixels[];
        int cdistance;
        int cred, cgreen, cblue;
        int colorNumber;
		
        /** 
		 * Constructor for Octree class, creates root node.
		 */
        public Octree() {
            // initialize SQUARES, indexed from 0..2 * MAX_RGB 
            // with values -255^2..255^2
            for (int i = -MAX_RGB; i <= MAX_RGB; ++i) {
                SQUARES[i+MAX_RGB] = i*i;
            }
            // initialize root node with (127, 127, 127, null, 0)
            root = new Node( (MAX_RGB+1) >> 1, (MAX_RGB+1) >> 1, (MAX_RGB+1) >> 1, null, (byte)0 );
            root.parent = root;
            root.numberColors = Integer.MAX_VALUE;
        }
		
        
        public int[] colormap() {
        	return colormap;
        }
        
        
        public byte[] indexedPixels() {
        	return indexedPixels;
        }


        /** 
		 * @param pixels      image as an array of RGBA colorCount 
		 * @param width       width of the image
		 * @param maxColors   number of colorCount requested in quantized image
		 */
        public void quantize(int pixels[], int width, int maxColors) {
            depth = 1;
            for (int mc = maxColors; mc != 0; mc >>= 2, ++depth);
            int numberPixels = pixels.length;
            int maxShift;
            for (maxShift = 4*8; numberPixels != 0; maxShift--)
                numberPixels >>= 1;
            for (int level = 0; level <= depth; level++) {
                shift[level] = maxShift;
                if (maxShift != 0) maxShift--;
            }
            sortInImage(pixels);
            reduction(maxColors);
            assignment(pixels, maxColors);
        }
		
		
        void sortInImage(int pixels[]) {
            int r, g, b, color;
            for (int i = 0; i < pixels.length; ++i) {
                color = pixels[i];
                r = (color >> 16) & 0xff;
                g = (color >>  8) & 0xff;
                b =  color        & 0xff;
                sortInRGB(r, g, b);
            }
        }
		
		
        void sortInRGB(int red, int green, int blue) {
            // prune one level if tree is too large
            if (nodes > MAX_NODES) {
                pruneLevel(root);
                --depth;
            }
            // descent the tree, start with root
            Node node = root;
            for (int level = 1; level <= depth; ++level) {
                int id =
                (red   > node.midRed   ? 1 : 0) |
                (green > node.midGreen ? 2 : 0) |
                (blue  > node.midBlue  ? 4 : 0);
                // was this branch visited before ?
                if (node.children[id] == null) {
                    int bisect = (1 << (MAX_TREE_DEPTH - level)) >> 1;
                    Node n = new Node(
                                      node.midRed   + ((id & 1) != 0 ? bisect : -bisect),
                                      node.midGreen + ((id & 2) != 0 ? bisect : -bisect),
                                      node.midBlue  + ((id & 4) != 0 ? bisect : -bisect),
                                      node, (byte)id);
                    ++nodes;
                    node.census |= 1 << id; // register new child
                    node.children[id] = n;
                    if (level == depth) ++colors;
                }
                node = node.children[id]; // descent to next level
                node.numberColors += 1 << shift[level];
            }
            ++node.numberUnique;
            node.totalRed   += red;
            node.totalGreen += green;
            node.totalBlue  += blue;
        }
		
		
        void pruneLevel(Node node) {
            if (node.census != 0)
                for (int i = 0; i < node.children.length; ++i)
                    if ((node.census & (1 << i)) != 0)
                        pruneLevel(node.children[i]);
            if (node.level == depth)
                pruneChild(node);
        }
		
		
        void reduction(int numberColors) {
            nextPruningTreshold = 1;
            while (colors > numberColors) {
                pruningTreshold = nextPruningTreshold;
                nextPruningTreshold = root.numberColors;
                colors = 0;
                reduce(root);
            }
        }
		
		
        void reduce(Node node) {
            if (node.census != 0)
                for (int i = 0; i < node.children.length; ++i)
                    if ((node.census & (1 << i)) != 0)
                        reduce(node.children[i]);
            if (node.numberColors <= pruningTreshold)
                pruneChild(node);
            else {
                if (node.numberUnique > 0)
                    ++colors;
                if (node.numberColors < nextPruningTreshold)
                    nextPruningTreshold = node.numberColors;
            }
        }
		
		
		void pruneChild(Node node) {
            Node parent = node.parent;
            // parent.children[node.id] = null;
            parent.census &= ~(1 << node.id);
            parent.numberUnique += node.numberUnique;
            parent.totalRed     += node.totalRed;
            parent.totalGreen   += node.totalGreen;
            parent.totalBlue    += node.totalBlue;
            --nodes;
        }
		

        void assignment(int pixels[], int maxColors) {
            colormap = new int[maxColors];
            indexedPixels = new byte[pixels.length];
            colors = 0;
            colorMap(root);
            int r, g, b, color;
            for (int i = 0; i < pixels.length; ++i) {
                color = pixels[i];
                r = (color >> 16) & 0xff;
                g = (color >>  8) & 0xff;
                b =  color        & 0xff;
                int index = rgb2idx(r, g, b);
                indexedPixels[i] = (byte) index;
                pixels[i] = colormap[index];
            }
        }
		

        void colorMap(Node node) {
            if (node.census != 0)
                for (int i = 0; i < node.children.length; ++i)
                    if ((node.census & (1 << i)) != 0)
                        colorMap(node.children[i]);
            if (node.numberUnique != 0) {
                int uh = node.numberUnique >> 1;
                int  r = (node.totalRed   + uh) / node.numberUnique;
                int  g = (node.totalGreen + uh) / node.numberUnique;
                int  b = (node.totalBlue  + uh) / node.numberUnique;
                node.colorNumber = colors;
                colormap[colors++] = (r << 16) | (g << 8) | b;
            }
        }
		

        int rgb2idx(int red, int green, int blue) {
            Node node = root;
            for (;;) {
                byte id = (byte)(
                                 (red   > node.midRed   ? 1 : 0) |
                                 (green > node.midGreen ? 2 : 0) |
                                 (blue  > node.midBlue  ? 4 : 0));
                if ((node.census & (1 << id)) == 0) break;
                node = node.children[id];
            }
            cdistance = Integer.MAX_VALUE;
            cred   = red;
            cgreen = green;
            cblue  = blue;
            closestColor(node.parent);
            return colorNumber;
        }

		
        void closestColor(Node node) {
            if (node.census != 0)
                for (int i = 0; i < node.children.length; i++)
                    if ((node.census & (1 << i)) != 0)
                        closestColor(node.children[i]);
            if (node.numberUnique != 0) {
                int color = colormap[node.colorNumber];
                int r = (color >> 16) & 0xff;
                int g = (color >>  8) & 0xff;
                int b =  color        & 0xff;
                int rD = r - cred   + MAX_RGB;
                int gD = g - cgreen + MAX_RGB;
                int bD = b - cblue  + MAX_RGB;
               int distance = SQUARES[rD] + SQUARES[gD] + SQUARES[bD];
                if (distance < cdistance) {
                    cdistance = distance;
                    colorNumber = node.colorNumber;
                }
            }
        }
		
		
        public class Node {
            Node parent;
            Node children[] = new Node[8];
            int midRed, midGreen, midBlue;
            int totalRed, totalGreen, totalBlue;
            byte id;           // which child of parent?
            byte census;       // used children
            byte level;        // level in tree
            int colorNumber;   // index in color table
            int numberUnique;
            int numberColors;
			
            // constructor for a null node
            public Node() {}
			
            // constructor for a node with RGB information
            public Node( int midRed, int midGreen, int midBlue, Node parent, byte id ) {
                this.midRed   = midRed;
                this.midGreen = midGreen;
                this.midBlue  = midBlue;
                this.parent = parent;
                this.level  = (byte)(parent == null ? 0 : parent.level + 1);
                this.id     = id;
            }
        }
		
    } // Octree


}
