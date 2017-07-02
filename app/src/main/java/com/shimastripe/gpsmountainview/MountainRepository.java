package com.shimastripe.gpsmountainview;

import com.google.gson.annotations.Expose;

/**
 * Created by takagi_go on 2017/07/03.
 */

public class MountainRepository {
    @Expose
    private int jobId;
    @Expose
    private Integer[] ridge;
    @Expose
    private Mountain summit;
    @Expose
    private int ridgeWidth;
    @Expose
    private int ridgeHeight;
}