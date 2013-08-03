package com.github.axet.lookup;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

import com.github.axet.lookup.common.ImageBinary;
import com.github.axet.lookup.common.RangeColor;
import com.github.axet.lookup.trans.CannyEdgeDetector;

public class Lookup {

    public static class NotFound extends RuntimeException {
        private static final long serialVersionUID = 5393563026702192412L;

        public NotFound() {
            super("NotFound");
        }
    }

    // convert

    public BufferedImage convert(Image image) {
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, null, null);
        return bufferedImage;
    }

    // filter

    static public BufferedImage filterSimply(BufferedImage bi) {
        BufferedImage buff = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());

        float n = 1f / 25f;
        Kernel kernel = new Kernel(5, 5, new float[] { n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n,
                n, n, n, n });

        ConvolveOp op = new ConvolveOp(kernel);
        op.filter(bi, buff);

        return buff;
    }

    public BufferedImage filterResize(BufferedImage bi) {
        int cx = bi.getWidth() / 7;
        int cy = bi.getHeight() / 7;
        BufferedImage resizedImage = new BufferedImage(cx, cy, bi.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(bi, 0, 0, cx, cy, null);
        g.dispose();

        return resizedImage;
    }

    static public BufferedImage filterResizeDouble(BufferedImage bi) {
        int cx = bi.getWidth() * 2;
        int cy = bi.getHeight() * 2;
        BufferedImage resizedImage = new BufferedImage(cx, cy, bi.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(bi, 0, 0, cx, cy, null);
        g.dispose();
        return resizedImage;
    }

    static public BufferedImage filterResizeDoubleCanvas(BufferedImage bi) {
        int cx = bi.getWidth() * 4;
        int cy = bi.getHeight() * 4;
        BufferedImage resizedImage = new BufferedImage(cx, cy, bi.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(bi, cx / 4, cy / 4, cx / 2, cy / 2, null);
        g.dispose();

        return resizedImage;
    }

    static public BufferedImage filterShrinkDouble(BufferedImage bi) {
        int cx = bi.getWidth() / 2;
        int cy = bi.getHeight() / 2;
        BufferedImage resizedImage = new BufferedImage(cx, cy, bi.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(bi, 0, 0, cx, cy, null);
        g.dispose();

        return resizedImage;
    }

    static public BufferedImage filterRemoveCanvas(BufferedImage bi) {
        int x1 = bi.getWidth();
        int x2 = 0;

        int y1 = bi.getHeight();
        int y2 = 0;

        for (int x = 0; x < bi.getWidth(); x++) {
            for (int y = 0; y < bi.getHeight(); y++) {
                if ((bi.getRGB(x, y) & 0xffffff) != 0) {
                    if (x1 > x)
                        x1 = x;
                    if (x2 < x)
                        x2 = x;
                    if (y1 > y)
                        y1 = y;
                    if (y2 < y)
                        y2 = y;
                }
            }
        }

        int cx = x2 - x1;
        int cy = y2 - y1;

        BufferedImage dest = new BufferedImage(cx, cy, bi.getType());
        Graphics g = dest.getGraphics();
        g.drawImage(bi, 0, 0, (int) dest.getWidth(), (int) dest.getHeight(), x1, y1, x1 + dest.getWidth(),
                y1 + dest.getHeight(), null);
        g.dispose();

        return dest;
    }

    public BufferedImage filterBlur(BufferedImage bi) {
        BufferedImage buff = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());

        float n = 1f / 25f;
        Kernel kernel = new Kernel(5, 5, new float[] { n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n,
                n, n, n, n });

        ConvolveOp op = new ConvolveOp(kernel);
        op.filter(bi, buff);

        return buff;
    }

    static public BufferedImage toGray(BufferedImage bi) {
        BufferedImage out = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(bi, out);

        return out;
    }

    static public BufferedImage edge(BufferedImage bi) {
        CannyEdgeDetector detector = new CannyEdgeDetector();

        detector.setLowThreshold(3f);
        detector.setHighThreshold(3f);
        detector.setGaussianKernelWidth(2);
        detector.setGaussianKernelRadius(1f);

        detector.setSourceImage(bi);
        detector.process();

        return detector.getEdgesImage();
    }

    //
    // lookup
    //

    static boolean find(BufferedImage bi, int x, int y, BufferedImage icon, float m) {
        for (int yy = 0; yy < icon.getHeight(); yy++) {
            for (int xx = 0; xx < icon.getWidth(); xx++) {
                int rgb1 = icon.getRGB(xx, yy);
                int rgb2 = bi.getRGB(x + xx, y + yy);
                RangeColor r = new RangeColor(rgb1, m);
                if (!r.inRange(rgb2))
                    return false;
            }
        }

        return true;
    }

    public static boolean find(ImageBinary image, int x, int y, ImageBinary template, double m) {
        m = m * 255;
        for (int yy = 0; yy < template.getHeight(); yy++) {
            for (int xx = 0; xx < template.getWidth(); xx++) {
                double rgb1 = template.zeroMean.s(xx, yy);
                double rgb2 = image.zeroMean.s(x + xx, y + yy);
                double min = rgb1 - m;
                double max = rgb1 + m;
                if (rgb2 < min || rgb2 > max)
                    return false;
            }
        }

        return true;
    }

    public static Point lookup(BufferedImage bi, BufferedImage icon) {
        return lookupUL(bi, icon, 0.10f);
    }

    public static Point lookupUL(BufferedImage image, BufferedImage template, float m) {
        return lookupUL(image, template, 0, 0, image.getWidth() - 1, image.getHeight() - 1, m);
    }

    public static Point lookupUL(BufferedImage image, BufferedImage template, int x1, int y1, int x2, int y2, float m) {
        for (int y = y1; y < y2 - template.getHeight(); y++) {
            for (int x = x1; x < x2 - template.getWidth(); x++) {
                if (find(image, x, y, template, m))
                    return new Point(x, y);
            }
        }

        return null;
    }

    public static Point lookupUL(ImageBinary image, ImageBinary template, float m) {
        for (int y = 0; y < image.getHeight() - template.getHeight(); y++) {
            for (int x = 0; x < image.getWidth() - template.getWidth(); x++) {
                if (find(image, x, y, template, m))
                    return new Point(x, y);
            }
        }

        return null;
    }

    /**
     * lookup center of image
     * 
     * @param bi
     * @param exit
     * @param m
     * @return
     */
    static public Point lookup(BufferedImage bi, BufferedImage exit, float m) {
        return lookup(bi, exit, 0, 0, bi.getWidth() - 1, bi.getHeight() - 1, m);
    }

    static public Point lookup(BufferedImage bi, BufferedImage exit, int x1, int y1, int x2, int y2, float m) {
        Point pul = lookupUL(bi, exit, x1, y1, x2, y2, m);
        if (pul == null)
            throw new NotFound();

        int x = pul.x + exit.getWidth() / 2;
        int y = pul.y + exit.getHeight() / 2;
        return new Point(x, y);
    }

    static public Point lookupMeanImage(ImageBinary bi, ImageBinary i, int x1, int y1, int x2, int y2, float p) {
        Point pul = lookupUL(bi, i, p);
        if (pul == null)
            throw new NotFound();

        int x = pul.x + i.getWidth() / 2;
        int y = pul.y + i.getHeight() / 2;
        return new Point(x, y);
    }

}
