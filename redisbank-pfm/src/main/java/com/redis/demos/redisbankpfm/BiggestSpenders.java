package com.redis.demos.redisbankpfm;

import lombok.Data;

@Data
public class BiggestSpenders {

    private final double[] series;
    private final String[] labels; 

    public BiggestSpenders(int size)  {
        this.series = new double[size];
        this.labels = new String[size];
    }

}
