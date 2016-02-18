package com.snowy73.qrsync.barcode;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.snowy73.qrsync.barcode.camera.ui.GraphicOverlay;

/**
 * Factory for creating a tracker and associated graphic to be associated with a new barcode.  The
 * multi-processor uses this factory to create barcode trackers as needed -- one for each barcode.
 */
class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;
    // http://stackoverflow.com/questions/32021193
    private BarcodeGraphicTracker.Callback mCallback;

    // http://stackoverflow.com/questions/32021193
    /*
    BarcodeTrackerFactory(GraphicOverlay<BarcodeGraphic> barcodeGraphicOverlay) {
        mGraphicOverlay = barcodeGraphicOverlay;
    }
    */

    // http://stackoverflow.com/questions/32021193
    BarcodeTrackerFactory(GraphicOverlay<BarcodeGraphic> barcodeGraphicOverlay, BarcodeGraphicTracker.Callback callback) {
        mGraphicOverlay = barcodeGraphicOverlay;
        mCallback = callback;
    }

    // http://stackoverflow.com/questions/32021193
    /*
    @Override
    public Tracker<Barcode> create(Barcode barcode) {
        BarcodeGraphic graphic = new BarcodeGraphic(mGraphicOverlay);
        return new BarcodeGraphicTracker(mGraphicOverlay, graphic);
    }
    */

    // http://stackoverflow.com/questions/32021193
    @Override
    public Tracker<Barcode> create(Barcode barcode) {
        BarcodeGraphic graphic = new BarcodeGraphic(mGraphicOverlay);
        return new BarcodeGraphicTracker(mGraphicOverlay, graphic, mCallback);
    }

}