package com.michael.xx.audiorecoder;

/**
 * Created by michael_xu on 2017/8/11.
 */

public abstract class AudioEncoder {
    protected int SAMPLE_RATE = 8000;
    protected int BIT_RATE = 64;
    protected int CHANNEL_COUNT = 1;
    protected String destinationFile;

    public void init(int SAMPLE_RATE, int BIT_RATE, int CHANNEL_COUNT) {
        this.SAMPLE_RATE = SAMPLE_RATE;
        this.BIT_RATE = BIT_RATE;
        this.CHANNEL_COUNT = CHANNEL_COUNT;
    }

    public String getDestFile() {
        return destinationFile;
    }

    public abstract void encode(String sourceFile);

}
