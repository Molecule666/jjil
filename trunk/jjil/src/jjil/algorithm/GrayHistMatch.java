/*
 * GrayHistMatch.java
 *
 * Created on September 9, 2006, 4:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 * Copyright 2007 by Jon A. Webb
 *     This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the Lesser GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package jjil.algorithm;
import jjil.core.Gray8Image;
import jjil.core.Image;
import jjil.core.PipelineStage;
import jjil.core.RgbVal;

/**
 * Pipeline stage modifies a gray image's pixel values to make its histogram
 * match a target histogram (to the extent this is possible while uniquely
 * mapping each input grayvalue). This PipelineStage modifies its input.
 *
 * @author webb
 */
public class GrayHistMatch extends PipelineStage {
    /* We use the cumulative pixel count in computation, not
     * the input histogram.
     */
    private int[] histCumTarget;
    
    /** Creates a new instance of GrayHistMatch 
     *
     * @param histTarget the input histogram.
     * @throws IllegalArgumentException if the input histogram does not have
     * 256 elements.
     */
    public GrayHistMatch(int[] histTarget) throws IllegalArgumentException {
        setHistogram(histTarget);
    }
    
    private byte[] createLookup(int[] histCumTarget, int[] histCumSource) {
        byte[] lookup = new byte[256];
        int j=0;
        for (int i=0; i<256; i++) {
            while (histCumTarget[j] < histCumSource[i]) {
                j++;
            }
            if (j<256) {
                // don't forget byte is a signed 8-bit value
                lookup[i] = RgbVal.toSignedByte((byte)j);
            } else {
                lookup[i] = Byte.MAX_VALUE;
            }
        }
        return lookup;
    }
    
    /** getHistogram returns the target histogram that has been
     * previously set.
     *
     * @return the target histogram
     */
    public int[] getHistogram() {
        int[] result = new int[256];
        /* since we store the cumulative histogram, not the original,
         * we have to recover it by taking the differences.
         */
        result[0] = this.histCumTarget[0];
        for (int i=1; i<256; i++) {
            result[i] = this.histCumTarget[i] - this.histCumTarget[i-1];
        }
        return result;
    }
    
    /** Push transforms an input gray image to have the target histogram,
     * as near as possible while assigning each input grayvalue a unique
     * output grayvalue.
     *
     * @param image the input image.
     * @throws IllegalArgumentException if the input image is not gray.
     */
    public void Push(Image image) throws IllegalArgumentException {
        if (!(image instanceof Gray8Image)) {
            throw new IllegalArgumentException(image.toString() + 
                    " should be a Gray8Image");
        }
        /* We could do a test here to make sure that the uppermost
         * element of histCumTarget equals the count of pixels in
         * the input image. But my own feeling is that it is OK if
         * they don't match. The result will be a working lookup
         * table, in any case, though the histogram won't quite
         * match what was intended.
         */
        Gray8Image input = (Gray8Image) image;
        // get the input histogram
        int[] histCum = GrayHist.computeHistogram(input);
        // for the purposes of computation below we need a cumulative
        // pixel count, not a histogram
        for (int i=1; i<256; i++) {
            histCum[i] = histCum[i] + histCum[i-1];
        }
        // create a lookkup table to map the input cumulative histogram
        // to the target cumulative histogram.
        byte[] lookup = createLookup(this.histCumTarget, histCum);
        // apply the lookup table
        GrayLookup modify = new GrayLookup(lookup);
        modify.Push(input);
        super.setOutput(modify.Front());
    }
    
    /** setHistogram sets a new target histogram.
     *
     * @param histTarget the new target histogram
     * @throws IllegalArgumentException if histTarget does not have 256
     * elements.
     */
    public void setHistogram(int[] histTarget) throws IllegalArgumentException {
        if (histTarget.length != 256) {
            throw new IllegalArgumentException("histogram length not 256");
        }
        this.histCumTarget = new int[256];
        /* We actually store the cumulative histogram, not the original.
         */
        histCumTarget[0] = histTarget[0];
        for (int i=1; i<256; i++) {
            histCumTarget[i] = histCumTarget[i-1] + histTarget[i];
        }
    }
}