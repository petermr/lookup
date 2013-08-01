package com.github.axet.lookup;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.axet.lookup.common.FontFamily;
import com.github.axet.lookup.common.FontSymbol;
import com.github.axet.lookup.common.FontSymbolLookup;
import com.github.axet.lookup.common.GPoint;
import com.github.axet.lookup.common.ImageBinary;
import com.github.axet.lookup.trans.CannyEdgeDetector;
import com.github.axet.lookup.trans.NCC;

public class OCRCore {

    static class BiggerFirst implements Comparator<FontSymbolLookup> {

        @Override
        public int compare(FontSymbolLookup arg0, FontSymbolLookup arg1) {
            int r = new Integer(arg1.size()).compareTo(new Integer(arg0.size()));

            // beeter qulity goes first
            if (r == 0)
                r = new Double(arg1.g).compareTo(new Double(arg0.g));

            return r;
        }

    }

    class Left2Right implements Comparator<FontSymbolLookup> {

        public int compare(int o1, int o2, int val) {
            if (Math.abs(o1 - o2) < val)
                return 0;

            return compare(o1, o2);
        }

        // desc algorithm (high comes at first [0])
        public int compare(int o1, int o2) {
            return new Integer(o1).compareTo(new Integer(o2));
        }

        @Override
        public int compare(FontSymbolLookup arg0, FontSymbolLookup arg1) {
            int r = 0;

            if (r == 0) {
                if (!arg0.yCross(arg1))
                    r = compare(arg0.y, arg1.y);
            }

            if (r == 0)
                r = compare(arg0.x, arg1.x);

            if (r == 0)
                r = compare(arg0.y, arg1.y);

            return r;
        }
    }

    Map<String, FontFamily> fontFamily = new HashMap<String, FontFamily>();

    CannyEdgeDetector detector = new CannyEdgeDetector();

    // 1.0f == exact match, -1.0f - completely different images
    float threshold = 0.80f;

    public OCRCore() {
        detector.setLowThreshold(3f);
        detector.setHighThreshold(3f);
        detector.setGaussianKernelWidth(2);
        detector.setGaussianKernelRadius(1f);
    }

    BufferedImage prepareImage(BufferedImage b) {
        b = Lookup.toGray(b);

        b = Lookup.filterResizeDoubleCanvas(b);

        b = Lookup.edge(b);

        return b;
    }

    BufferedImage prepareImageCrop(BufferedImage b) {
        b = prepareImage(b);

        b = Lookup.filterRemoveCanvas(b);

        return b;
    }

    List<FontSymbol> getSymbols() {
        List<FontSymbol> list = new ArrayList<FontSymbol>();

        for (FontFamily f : fontFamily.values()) {
            list.addAll(f);
        }

        return list;
    }

    List<FontSymbol> getSymbols(String fontFamily) {
        return this.fontFamily.get(fontFamily);
    }

    List<FontSymbolLookup> findAll(List<FontSymbol> list, ImageBinary bi) {
        return findAll(list, bi, 0, 0, bi.getWidth(), bi.getHeight());
    }

    List<FontSymbolLookup> findAll(List<FontSymbol> list, ImageBinary bi, int x1, int y1, int x2, int y2) {
        List<FontSymbolLookup> l = new ArrayList<FontSymbolLookup>();

        for (FontSymbol fs : list) {
            List<GPoint> ll = NCC.lookup(bi, x1, y1, x2, y2, fs.image, threshold);
            for (GPoint p : ll)
                l.add(new FontSymbolLookup(fs, p.x, p.y, p.g));
        }

        return l;
    }

}
